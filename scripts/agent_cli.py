#!/usr/bin/env python3
"""Command-line client for the SuiLearn Agent REST API.

The backend exposes a synchronous turn endpoint plus event replay, active-turn
lookup, reply, and cancel endpoints. This client starts a turn in a background
thread, polls events/active-turn, and resumes WAITING_INPUT turns when running
interactively.

Examples:
    python3 scripts/agent_cli.py capabilities
    python3 scripts/agent_cli.py ask "Explain HashMap" --knowledge-base kb_01
    python3 scripts/agent_cli.py chat --knowledge-base kb_01 --verbose
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from typing import Any, Callable

DEFAULT_BASE_URL = "http://127.0.0.1:8080"
DEFAULT_LEARNER_ID = "cli-learner"
DEFAULT_CAPABILITY = "study_agent"
DEFAULT_POLL_INTERVAL = 0.5
DEFAULT_REQUEST_TIMEOUT = 15.0
DEFAULT_START_TIMEOUT = 200.0

EXIT_OK = 0
EXIT_ERROR = 1
EXIT_WAITING_CANCELLED = 2


class ApiError(RuntimeError):
    """Sanitized error from the Agent API or the transport."""

    def __init__(self, message: str, code: str | None = None, status: int | None = None):
        super().__init__(message)
        self.message = message
        self.code = code
        self.status = status

    def __str__(self) -> str:
        if self.code:
            return f"{self.message} (code={self.code})"
        if self.status:
            return f"{self.message} (HTTP {self.status})"
        return self.message


class AgentApiClient:
    """Minimal standard-library client for the SuiLearn Agent REST API."""

    def __init__(self, base_url: str, token: str | None = None, timeout: float = DEFAULT_REQUEST_TIMEOUT):
        self.base_url = normalize_base_url(base_url)
        self.token = token
        self.timeout = timeout

    def capabilities(self) -> dict[str, Any]:
        return self._request("GET", "/agent/capabilities")

    def list_knowledge_bases(self) -> list[dict[str, Any]]:
        result = self._request("GET", "/knowledge-bases")
        return result if isinstance(result, list) else []

    def start_turn(self, payload: dict[str, Any], timeout: float | None = None) -> dict[str, Any]:
        return self._request("POST", "/agent/turns", payload=payload, timeout=timeout)

    def events(self, turn_id: str, after_seq: int = 0) -> dict[str, Any]:
        quoted = urllib.parse.quote(turn_id, safe="")
        return self._request("GET", f"/agent/turns/{quoted}/events?afterSeq={int(after_seq)}")

    def active_turn(self, session_id: str) -> dict[str, Any]:
        quoted = urllib.parse.quote(session_id, safe="")
        return self._request("GET", f"/agent/sessions/{quoted}/active-turn")

    def reply(self, turn_id: str, text: str) -> dict[str, Any]:
        quoted = urllib.parse.quote(turn_id, safe="")
        return self._request("POST", f"/agent/turns/{quoted}/reply", payload={"text": text})

    def cancel(self, turn_id: str) -> dict[str, Any]:
        quoted = urllib.parse.quote(turn_id, safe="")
        return self._request("POST", f"/agent/turns/{quoted}/cancel", payload={})

    def _request(
        self,
        method: str,
        path: str,
        payload: dict[str, Any] | None = None,
        timeout: float | None = None,
    ) -> Any:
        url = f"{self.base_url}{path}"
        headers: dict[str, str] = {"Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        data: bytes | None = None
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"

        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=timeout or self.timeout) as response:
                body = response.read().decode("utf-8")
                return json.loads(body) if body else {}
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            exc.close()
            raise self._http_error(exc.code, body, exc.reason) from exc
        except urllib.error.URLError as exc:
            raise ApiError(f"unable to reach Agent API at {self.base_url}: {exc.reason}") from exc
        except TimeoutError as exc:
            raise ApiError(f"Agent API request timed out: {path}") from exc

    @staticmethod
    def _http_error(status: int, body: str, reason: str) -> ApiError:
        code = f"HTTP_{status}"
        message = f"Agent API request failed: {reason}"
        try:
            parsed = json.loads(body)
        except json.JSONDecodeError:
            parsed = {}
        if isinstance(parsed, dict):
            code = str(parsed.get("code") or code)
            server_message = parsed.get("message")
            field_errors = parsed.get("fieldErrors") or []
            if server_message:
                message = str(server_message)
            if field_errors:
                details = "; ".join(
                    f"{item.get('field', '?')}: {item.get('message', '')}"
                    for item in field_errors
                    if isinstance(item, dict)
                )
                if details:
                    message = f"{message} ({details})"
        return ApiError(message, code=code, status=status)


def normalize_base_url(base_url: str) -> str:
    """Return an /api/v2 base URL with no trailing slash."""
    value = (base_url or DEFAULT_BASE_URL).strip().rstrip("/")
    if not value:
        value = DEFAULT_BASE_URL
    if "://" not in value:
        value = f"http://{value}"
    if value.endswith("/api/v2"):
        return value
    return f"{value}/api/v2"


def new_session_id() -> str:
    return f"cli_{uuid.uuid4().hex[:20]}"


def emit(output: Any, text: str) -> None:
    """Write text to output, ensuring a trailing newline."""
    if not text:
        output.write("\n")
        return
    output.write(text)
    if not text.endswith("\n"):
        output.write("\n")
    output.flush()


def scope_from_args(args: argparse.Namespace) -> dict[str, str]:
    scope: dict[str, str] = {}
    if getattr(args, "knowledge_base", None):
        scope["knowledgeBaseId"] = args.knowledge_base
    if getattr(args, "material", None):
        scope["materialId"] = args.material
    return scope


def validate_scope(parser: argparse.ArgumentParser, args: argparse.Namespace) -> dict[str, str]:
    scope = scope_from_args(args)
    if not scope:
        parser.error("one of --knowledge-base or --material is required")
    return scope


def build_start_payload(args: argparse.Namespace, message: str, session_id: str) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "learnerId": args.learner,
        "sessionId": session_id,
        "message": message,
        "capability": args.capability,
        "scope": scope_from_args(args),
    }
    return payload


def handle_event(
    event: dict[str, Any],
    verbose: bool,
    output: Any,
    state: dict[str, str | None],
) -> str | None:
    """Print one stream event.

    Returns the prompt text for wait_for_input events; run_turn prints it only
    when it actually asks the user for a reply.
    """
    event_type = str(event.get("type") or "")
    content = str(event.get("content") or "")
    stage = str(event.get("stage") or "")
    code = None
    if isinstance(event.get("metadata"), dict):
        code = event["metadata"].get("code")

    if event_type == "content":
        if content:
            emit(output, content)
    elif event_type == "result":
        if content:
            state["result_content"] = content
            emit(output, content)
    elif event_type == "done":
        if content and content != state.get("result_content"):
            emit(output, content)
    elif event_type in ("failed", "cancelled"):
        suffix = f" (code={code})" if code else ""
        emit(output, f"[{event_type}] {content}{suffix}")
    elif event_type == "error":
        suffix = f" (code={code})" if code else ""
        emit(output, f"[error] {content}{suffix}")
    elif event_type == "wait_for_input":
        return content or "Agent is waiting for your reply."
    elif verbose:
        label = stage or event_type
        emit(output, f"[{event_type}:{label}] {content}".rstrip())

    return None


def prompt_for_reply(input_fn: Callable[[str], str], output: Any, prompt: str) -> str | None:
    emit(output, f"\n[wait_for_input] {prompt}")
    for _ in range(3):
        try:
            answer = input_fn("  reply> ").strip()
        except EOFError:
            return None
        if answer:
            return answer
        emit(output, "  (reply cannot be empty)")
    return None


def print_result(result: dict[str, Any], output: Any) -> None:
    emit(output, "")
    emit(output, f"status: {result.get('status', 'UNKNOWN')}")
    emit(
        output,
        "usage: "
        f"prompt_tokens={result.get('promptTokens', 0)} "
        f"completion_tokens={result.get('completionTokens', 0)} "
        f"tool_calls={result.get('actionTraceCount', 0)} "
        f"cost_usd={result.get('usageCostUsd', 0.0):.6f}",
    )


def run_turn(
    client: AgentApiClient,
    args: argparse.Namespace,
    message: str,
    *,
    output: Any | None = None,
    input_fn: Callable[[str], str] | None = input,
    poll_interval: float | None = None,
) -> int:
    """Run one Agent turn, streaming events and handling WAITING_INPUT."""
    output = sys.stdout if output is None else output
    poll_seconds = poll_interval if poll_interval is not None else args.poll_interval
    session_id = args.session
    payload = build_start_payload(args, message, session_id)

    box: dict[str, Any] = {}

    def start_worker() -> None:
        try:
            box["result"] = client.start_turn(payload, timeout=args.start_timeout)
        except Exception as exc:  # surfaced after the worker finishes
            box["error"] = exc

    thread = threading.Thread(target=start_worker, daemon=True)
    thread.start()

    turn_id: str | None = None
    last_seq = 0
    wait_prompt = ""
    wait_handled_for_turn = False
    waiting_cancelled = False
    state: dict[str, str | None] = {"result_content": None}

    try:
        while thread.is_alive():
            active: dict[str, Any] | None = None
            try:
                active = client.active_turn(session_id)
                if active:
                    candidate = active.get("turnId")
                    if candidate and not turn_id:
                        turn_id = str(candidate)
            except ApiError as exc:
                if args.verbose:
                    emit(output, f"[warn] active-turn lookup failed: {exc}")

            if turn_id:
                try:
                    page = client.events(turn_id, last_seq)
                    for event in page.get("events", []):
                        seq = int(event.get("seq") or 0)
                        if seq <= last_seq:
                            continue
                        last_seq = seq
                        prompt = handle_event(event, args.verbose, output, state)
                        if prompt is not None:
                            wait_prompt = prompt
                except ApiError as exc:
                    if args.verbose:
                        emit(output, f"[warn] event replay failed: {exc}")

                if active and active.get("status") == "WAITING_INPUT":
                    if waiting_cancelled:
                        pass
                    elif input_fn is None:
                        emit(
                            output,
                            "[non-interactive] Agent is waiting for input; cancelling the turn.",
                        )
                        client.cancel(turn_id)
                        waiting_cancelled = True
                    elif not wait_handled_for_turn:
                        answer = prompt_for_reply(input_fn, output, wait_prompt or "Agent is waiting for your reply.")
                        wait_handled_for_turn = True
                        wait_prompt = ""
                        if answer is None:
                            emit(output, "[cancelled] no reply available; cancelling the turn.")
                            client.cancel(turn_id)
                            waiting_cancelled = True
                        else:
                            client.reply(turn_id, answer)

                if active and active.get("status") != "WAITING_INPUT":
                    wait_handled_for_turn = False

            time.sleep(poll_seconds)
    except KeyboardInterrupt:
        if turn_id:
            try:
                client.cancel(turn_id)
            except ApiError:
                pass
        thread.join(timeout=1.0)
        raise

    thread.join(timeout=2.0)
    if thread.is_alive():
        emit(output, "[error] turn did not finish after the start request returned.")
        if turn_id:
            try:
                client.cancel(turn_id)
            except ApiError:
                pass
        return EXIT_ERROR

    if "error" in box:
        if turn_id:
            try:
                client.cancel(turn_id)
            except ApiError:
                pass
        emit(output, f"[error] {box['error']}")
        return EXIT_ERROR

    result = box.get("result")
    if not isinstance(result, dict):
        emit(output, "[error] Agent API returned an empty result.")
        return EXIT_ERROR

    # Catch any events that were emitted after the final poll iteration.
    result_turn_id = turn_id or str(result.get("turnId") or "")
    if result_turn_id:
        try:
            page = client.events(result_turn_id, last_seq)
            for event in page.get("events", []):
                seq = int(event.get("seq") or 0)
                if seq > last_seq:
                    last_seq = seq
                    handle_event(event, args.verbose, output, state)
        except ApiError as exc:
            emit(output, f"[warn] final event replay failed: {exc}")
            terminal = result.get("terminalEvent")
            if isinstance(terminal, dict):
                handle_event(terminal, args.verbose, output, state)

    print_result(result, output)
    return EXIT_WAITING_CANCELLED if waiting_cancelled else EXIT_OK


def print_capabilities(data: dict[str, Any], output: Any, as_json: bool = False) -> None:
    if as_json:
        output.write(json.dumps(data, ensure_ascii=False, indent=2))
        output.write("\n")
        output.flush()
        return

    emit(output, "Capabilities:")
    for item in data.get("capabilities", []):
        name = item.get("name", "?")
        description = item.get("description", "")
        emit(output, f"  {name}: {description}")
        tools = item.get("ownedTools") or []
        if tools:
            emit(output, f"    tools: {', '.join(str(tool) for tool in tools)}")

    tools = data.get("tools") or []
    if tools:
        emit(output, "Tools:")
        for item in tools:
            emit(output, f"  {item.get('name', '?')}: {item.get('description', '')}")


def print_tools(data: dict[str, Any], output: Any) -> None:
    tools = data.get("tools") or []
    if not tools:
        emit(output, "No tools registered.")
        return
    emit(output, "Tools:")
    for item in tools:
        name = item.get("name", "?")
        description = item.get("description", "")
        emit(output, f"  {name}: {description}")


def print_chat_help(output: Any) -> None:
    emit(output, "Commands:")
    emit(output, "  /help          show this help")
    emit(output, "  /tool, /tools  list registered tools")
    emit(output, "  /capabilities  list Agent capabilities and tools")
    emit(output, "  /verbose       toggle verbose event output")
    emit(output, "  /quit, /exit   leave the chat")


def resolve_chat_scope(
    client: AgentApiClient,
    args: argparse.Namespace,
    output: Any,
    input_fn: Callable[[str], str],
) -> bool:
    """Interactively choose a knowledge base or material when none was passed."""
    if scope_from_args(args):
        return True

    emit(output, "No scope selected. Loading knowledge bases...\n")
    knowledge_bases: list[dict[str, Any]] = []
    try:
        knowledge_bases = client.list_knowledge_bases()
    except ApiError as exc:
        emit(output, f"[warn] could not load knowledge bases: {exc}")

    if knowledge_bases:
        emit(output, "Knowledge bases:")
        for index, item in enumerate(knowledge_bases, start=1):
            kb_id = str(item.get("id") or "?")
            name = str(item.get("name") or item.get("id") or "?")
            emit(output, f"  {index}. {kb_id}  {name}")
        emit(output, "Enter a number, a knowledge base id, or material:<id>.")
    else:
        emit(output, "No knowledge bases found.")
        emit(output, "Enter a knowledge base id or material:<id>.")

    for _ in range(3):
        try:
            choice = input_fn("scope> ").strip()
        except EOFError:
            return False
        if not choice:
            continue
        if choice.isdigit() and knowledge_bases:
            index = int(choice) - 1
            if 0 <= index < len(knowledge_bases):
                args.knowledge_base = str(knowledge_bases[index].get("id") or "")
                return bool(args.knowledge_base)
            emit(output, f"  {choice} is not in range 1-{len(knowledge_bases)}.")
            continue
        if choice.startswith("material:"):
            material_id = choice.removeprefix("material:").strip()
            if material_id:
                args.material = material_id
                return True
        if choice.startswith("kb:"):
            choice = choice.removeprefix("kb:").strip()
        if choice:
            args.knowledge_base = choice
            return True
    return False


def run_chat(
    client: AgentApiClient,
    args: argparse.Namespace,
    *,
    output: Any | None = None,
    input_fn: Callable[[str], str] = input,
) -> int:
    output = sys.stdout if output is None else output
    emit(output, f"SuiLearn Agent chat (learner={args.learner}, capability={args.capability})")
    emit(output, f"session={args.session}")
    emit(output, "Type /help for commands.")
    while True:
        try:
            line = input_fn(f"\n{args.learner}> ").strip()
        except EOFError:
            emit(output, "\nbye")
            return EXIT_OK
        except KeyboardInterrupt:
            emit(output, "\nbye")
            return EXIT_OK

        if not line:
            continue
        if line in ("/quit", "/exit"):
            emit(output, "bye")
            return EXIT_OK
        if line == "/help":
            print_chat_help(output)
            continue
        if line in ("/tool", "/tools"):
            try:
                print_tools(client.capabilities(), output)
            except ApiError as exc:
                emit(output, f"[error] {exc}")
            continue
        if line == "/capabilities":
            try:
                print_capabilities(client.capabilities(), output)
            except ApiError as exc:
                emit(output, f"[error] {exc}")
            continue
        if line == "/verbose":
            args.verbose = not args.verbose
            emit(output, f"verbose={'on' if args.verbose else 'off'}")
            continue
        if line.startswith("/"):
            emit(output, f"unknown command: {line}")
            continue

        run_turn(client, args, line, output=output, input_fn=input_fn)


def build_parser() -> argparse.ArgumentParser:
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument(
        "--base-url",
        default=os.environ.get("SUILEARN_API_BASE_URL", DEFAULT_BASE_URL),
        help=f"backend base URL (default: {DEFAULT_BASE_URL}, env SUILEARN_API_BASE_URL)",
    )
    common.add_argument(
        "--token",
        default=os.environ.get("SUILEARN_AGENT_TOKEN"),
        help="optional Bearer token for the Agent API (env SUILEARN_AGENT_TOKEN)",
    )
    common.add_argument(
        "--request-timeout",
        type=float,
        default=DEFAULT_REQUEST_TIMEOUT,
        help=f"timeout in seconds for metadata/event requests (default: {DEFAULT_REQUEST_TIMEOUT})",
    )

    turn = argparse.ArgumentParser(add_help=False)
    turn.add_argument("--knowledge-base", help="knowledge base id used as the turn scope")
    turn.add_argument("--material", help="material id used as the turn scope")
    turn.add_argument("--learner", default=DEFAULT_LEARNER_ID, help=f"learner id (default: {DEFAULT_LEARNER_ID})")
    turn.add_argument("--session", help="session id; generated when omitted")
    turn.add_argument("--capability", default=DEFAULT_CAPABILITY, help=f"capability name (default: {DEFAULT_CAPABILITY})")
    turn.add_argument(
        "--poll-interval",
        type=float,
        default=DEFAULT_POLL_INTERVAL,
        help=f"event poll interval in seconds (default: {DEFAULT_POLL_INTERVAL})",
    )
    turn.add_argument(
        "--start-timeout",
        type=float,
        default=DEFAULT_START_TIMEOUT,
        help=f"start-turn HTTP timeout in seconds (default: {DEFAULT_START_TIMEOUT})",
    )
    turn.add_argument("--verbose", action="store_true", help="also print thinking/progress/tool events")

    prog = os.environ.get("SUILEARN_AGENT_PROG") or os.path.basename(sys.argv[0]) or "agent_cli.py"
    parser = argparse.ArgumentParser(
        prog=prog,
        description="CLI client for the SuiLearn Agent REST API. Without a subcommand, starts chat.",
        parents=[common, turn],
    )
    subparsers = parser.add_subparsers(dest="command", required=False)
    parser.set_defaults(command="chat")

    capabilities_parser = subparsers.add_parser(
        "capabilities", parents=[common], help="list Agent capabilities and tools"
    )
    capabilities_parser.add_argument("--json", dest="json_output", action="store_true", help="print raw JSON")

    ask_parser = subparsers.add_parser(
        "ask", parents=[common, turn], help="ask one question in a single Agent turn"
    )
    ask_parser.add_argument("message", help="question or learning request")
    ask_parser.add_argument(
        "--non-interactive",
        action="store_true",
        help="cancel the turn instead of prompting if the Agent asks for input",
    )

    subparsers.add_parser("chat", parents=[common, turn], help="start an interactive Agent session")

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.command == "ask":
        validate_scope(parser, args)
    if args.command in ("ask", "chat"):
        if args.poll_interval <= 0:
            parser.error("--poll-interval must be greater than 0")
        if args.start_timeout <= 0:
            parser.error("--start-timeout must be greater than 0")
        if not args.session:
            args.session = new_session_id()

    client = AgentApiClient(args.base_url, token=args.token, timeout=args.request_timeout)

    try:
        if args.command == "capabilities":
            print_capabilities(client.capabilities(), sys.stdout, as_json=args.json_output)
            return EXIT_OK
        if args.command == "ask":
            ask_input_fn = None if args.non_interactive else input
            return run_turn(client, args, args.message, input_fn=ask_input_fn)
        if args.command == "chat":
            if not scope_from_args(args) and not resolve_chat_scope(client, args, sys.stdout, input):
                print("[error] no Agent scope selected; use --knowledge-base or --material next time.", file=sys.stderr)
                return EXIT_ERROR
            return run_chat(client, args)
    except ApiError as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return EXIT_ERROR
    except KeyboardInterrupt:
        print("\nbye", file=sys.stderr)
        return 130

    parser.error(f"unknown command: {args.command}")
    return EXIT_ERROR


if __name__ == "__main__":
    raise SystemExit(main())
