"""Tests for the SuiLearn Agent REST CLI."""
from __future__ import annotations

import http.server
import importlib.util
import io
import json
import subprocess
import sys
import threading
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "agent_cli.py"
spec = importlib.util.spec_from_file_location("suilearn_agent_cli", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)


class NormalizeBaseUrlTests(unittest.TestCase):
    def test_default_base_url_gets_api_v2(self):
        self.assertEqual(mod.normalize_base_url("http://127.0.0.1:8080"), "http://127.0.0.1:8080/api/v2")

    def test_existing_api_v2_is_kept(self):
        self.assertEqual(
            mod.normalize_base_url("http://127.0.0.1:8080/api/v2/"),
            "http://127.0.0.1:8080/api/v2",
        )

    def test_host_port_gets_http_scheme(self):
        self.assertEqual(mod.normalize_base_url("localhost:8080"), "http://localhost:8080/api/v2")

    def test_empty_value_uses_default(self):
        self.assertEqual(mod.normalize_base_url(""), "http://127.0.0.1:8080/api/v2")


class PayloadTests(unittest.TestCase):
    def test_build_start_payload_with_knowledge_base(self):
        args = argparse_namespace(
            learner="learner_1",
            knowledge_base="kb_1",
            material=None,
            capability="rag_qa",
        )
        payload = mod.build_start_payload(args, "hello", "sess_1")
        self.assertEqual(payload["scope"], {"knowledgeBaseId": "kb_1"})
        self.assertEqual(payload["learnerId"], "learner_1")
        self.assertEqual(payload["sessionId"], "sess_1")
        self.assertEqual(payload["capability"], "rag_qa")

    def test_build_start_payload_with_material(self):
        args = argparse_namespace(
            learner="learner_1",
            knowledge_base=None,
            material="material_1",
            capability="study_agent",
        )
        payload = mod.build_start_payload(args, "hello", "sess_1")
        self.assertEqual(payload["scope"], {"materialId": "material_1"})


def argparse_namespace(**kwargs):
    return type("Args", (), kwargs)()


class FastTurnClient:
    def __init__(self):
        self.input_calls = 0
        self.replies = []
        self.cancelled = []
        self.start_calls = 0
        self.active_calls = 0

    def start_turn(self, payload, timeout=None):
        self.start_calls += 1
        return {
            "turnId": "turn_1",
            "sessionId": payload["sessionId"],
            "status": "COMPLETED",
            "lastSeq": 4,
            "terminalEvent": {"type": "done", "content": "fast answer", "metadata": {}},
            "promptTokens": 10,
            "completionTokens": 5,
            "usageCostUsd": 0.0123,
            "actionTraceCount": 1,
        }

    def active_turn(self, session_id):
        self.active_calls += 1
        return {"sessionId": session_id, "turnId": "turn_1", "status": "COMPLETED"}

    def events(self, turn_id, after_seq):
        events = [
            {"turnId": turn_id, "seq": 1, "type": "turn_started", "content": "", "metadata": {}},
            {"turnId": turn_id, "seq": 3, "type": "result", "content": "fast answer", "metadata": {}},
            {"turnId": turn_id, "seq": 4, "type": "done", "content": "fast answer", "metadata": {}},
        ]
        return {"turnId": turn_id, "afterSeq": after_seq, "lastSeq": 4, "events": [e for e in events if e["seq"] > after_seq]}

    def reply(self, turn_id, text):
        self.replies.append(text)
        return {"turnId": turn_id, "status": "RUNNING"}

    def cancel(self, turn_id):
        self.cancelled.append(turn_id)
        return {"turnId": turn_id, "status": "CANCELLED"}


class WaitingTurnClient(FastTurnClient):
    def __init__(self):
        super().__init__()
        self.started = threading.Event()
        self.finished = threading.Event()

    def start_turn(self, payload, timeout=None):
        self.start_calls += 1
        self.started.set()
        self.finished.wait(timeout=5)
        return {
            "turnId": "turn_1",
            "sessionId": payload["sessionId"],
            "status": "COMPLETED" if self.replies else "CANCELLED",
            "lastSeq": 4,
            "promptTokens": 10,
            "completionTokens": 5,
            "usageCostUsd": 0.0123,
            "actionTraceCount": 1,
        }

    def active_turn(self, session_id):
        self.active_calls += 1
        if not self.started.is_set() or self.finished.is_set():
            return {"sessionId": session_id, "turnId": None, "status": None}
        return {"sessionId": session_id, "turnId": "turn_1", "status": "WAITING_INPUT"}

    def events(self, turn_id, after_seq):
        wait_events = [
            {"turnId": turn_id, "seq": 1, "type": "turn_started", "content": "", "metadata": {}},
            {
                "turnId": turn_id,
                "seq": 2,
                "type": "wait_for_input",
                "content": "Choose A or B?",
                "metadata": {},
            },
        ]
        terminal_events = [
            {"turnId": turn_id, "seq": 3, "type": "result", "content": "Thanks!", "metadata": {}},
            {"turnId": turn_id, "seq": 4, "type": "done", "content": "Thanks!", "metadata": {}},
        ]
        if after_seq < 2:
            return {"turnId": turn_id, "afterSeq": after_seq, "lastSeq": 2, "events": [e for e in wait_events if e["seq"] > after_seq]}
        return {"turnId": turn_id, "afterSeq": after_seq, "lastSeq": 4, "events": [e for e in terminal_events if e["seq"] > after_seq]}

    def reply(self, turn_id, text):
        self.replies.append(text)
        self.finished.set()
        return {"turnId": turn_id, "status": "RUNNING"}

    def cancel(self, turn_id):
        self.cancelled.append(turn_id)
        self.finished.set()
        return {"turnId": turn_id, "status": "CANCELLED"}


def turn_args(**overrides):
    values = dict(
        learner="learner_1",
        session="sess_1",
        capability="study_agent",
        knowledge_base="kb_1",
        material=None,
        poll_interval=0.01,
        start_timeout=2.0,
        verbose=False,
        request_timeout=1.0,
    )
    values.update(overrides)
    return argparse_namespace(**values)


class RunTurnTests(unittest.TestCase):
    def test_fast_turn_prints_result_once_and_returns_ok(self):
        client = FastTurnClient()
        output = io.StringIO()
        code = mod.run_turn(client, turn_args(), "hello", output=output, input_fn=None, poll_interval=0.01)
        self.assertEqual(code, mod.EXIT_OK)
        self.assertEqual(client.start_calls, 1)
        self.assertEqual(output.getvalue().count("fast answer"), 1)
        self.assertIn("status: COMPLETED", output.getvalue())
        self.assertIn("cost_usd=0.012300", output.getvalue())

    def test_waiting_turn_uses_reply_and_resumes(self):
        client = WaitingTurnClient()
        output = io.StringIO()
        code = mod.run_turn(
            client,
            turn_args(),
            "hello",
            output=output,
            input_fn=lambda prompt: "A",
            poll_interval=0.01,
        )
        self.assertEqual(code, mod.EXIT_OK)
        self.assertEqual(client.replies, ["A"])
        self.assertEqual(client.cancelled, [])
        self.assertIn("Choose A or B?", output.getvalue())
        self.assertIn("Thanks!", output.getvalue())

    def test_non_interactive_waiting_turn_is_cancelled(self):
        client = WaitingTurnClient()
        output = io.StringIO()
        code = mod.run_turn(client, turn_args(), "hello", output=output, input_fn=None, poll_interval=0.01)
        self.assertEqual(code, mod.EXIT_WAITING_CANCELLED)
        self.assertEqual(client.cancelled, ["turn_1"])
        self.assertIn("non-interactive", output.getvalue())


class ToolCommandClient:
    def __init__(self, tools=None, knowledge_bases=None):
        self.tools = tools or [{"name": "ask_user", "description": "Pause for user input."}]
        self.knowledge_bases = knowledge_bases if knowledge_bases is not None else [{"id": "kb_1", "name": "Java"}]
        self.capability_calls = 0

    def capabilities(self):
        self.capability_calls += 1
        return {
            "capabilities": [{"name": "study_agent", "description": "Bounded study assistant.", "ownedTools": ["ask_user"]}],
            "tools": self.tools,
        }

    def list_knowledge_bases(self):
        return self.knowledge_bases


class ChatScopeTests(unittest.TestCase):
    def test_numeric_knowledge_base_selection(self):
        client = ToolCommandClient()
        args = argparse_namespace(knowledge_base=None, material=None)
        self.assertTrue(mod.resolve_chat_scope(client, args, io.StringIO(), lambda prompt: "1"))
        self.assertEqual(args.knowledge_base, "kb_1")

    def test_material_choice_sets_material_scope(self):
        client = ToolCommandClient(knowledge_bases=[])
        args = argparse_namespace(knowledge_base=None, material=None)
        self.assertTrue(mod.resolve_chat_scope(client, args, io.StringIO(), lambda prompt: "material:mat_1"))
        self.assertEqual(args.material, "mat_1")


class ChatCommandTests(unittest.TestCase):
    def test_tool_and_tools_commands_list_tools(self):
        client = ToolCommandClient()
        output = io.StringIO()
        lines = iter(["/tool", "/tools", "/quit"])
        args = argparse_namespace(
            learner="learner_1",
            session="sess_1",
            capability="study_agent",
            verbose=False,
        )
        code = mod.run_chat(client, args, output=output, input_fn=lambda prompt: next(lines))
        self.assertEqual(code, mod.EXIT_OK)
        self.assertEqual(client.capability_calls, 2)
        self.assertEqual(output.getvalue().count("Tools:"), 2)
        self.assertIn("ask_user: Pause for user input.", output.getvalue())


class CapabilitiesPrintingTests(unittest.TestCase):
    def test_table_and_json_output(self):
        data = {
            "capabilities": [{"name": "study_agent", "description": "Bounded study assistant.", "ownedTools": ["ask_user"]}],
            "tools": [{"name": "ask_user", "description": "Pause for user input."}],
        }
        table = io.StringIO()
        mod.print_capabilities(data, table)
        self.assertIn("study_agent", table.getvalue())
        self.assertIn("ask_user", table.getvalue())

        raw = io.StringIO()
        mod.print_capabilities(data, raw, as_json=True)
        self.assertEqual(json.loads(raw.getvalue()), data)


class ServerFixture:
    def __init__(self, handler):
        self.httpd = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()

    @property
    def base_url(self):
        host, port = self.httpd.server_address[:2]
        return f"http://{host}:{port}"

    def close(self):
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join(timeout=2)


class ApiClientTests(unittest.TestCase):
    def test_capabilities_request_sends_bearer_token(self):
        seen = {}

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                seen["path"] = self.path
                seen["authorization"] = self.headers.get("Authorization")
                body = json.dumps({"capabilities": [], "tools": []}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, format, *args):
                pass

        server = ServerFixture(Handler)
        try:
            client = mod.AgentApiClient(server.base_url, token="secret-token", timeout=2)
            self.assertEqual(client.capabilities(), {"capabilities": [], "tools": []})
            self.assertEqual(seen["path"], "/api/v2/agent/capabilities")
            self.assertEqual(seen["authorization"], "Bearer secret-token")
        finally:
            server.close()

    def test_start_turn_sends_expected_payload(self):
        seen = {}

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_POST(self):
                length = int(self.headers.get("Content-Length", "0"))
                seen["path"] = self.path
                seen["payload"] = json.loads(self.rfile.read(length).decode("utf-8"))
                body = json.dumps({"turnId": "turn_1", "sessionId": "sess_1", "status": "COMPLETED"}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, format, *args):
                pass

        server = ServerFixture(Handler)
        try:
            client = mod.AgentApiClient(server.base_url, timeout=2)
            result = client.start_turn({"learnerId": "learner_1", "message": "hi", "scope": {"materialId": "m_1"}})
            self.assertEqual(result["turnId"], "turn_1")
            self.assertEqual(seen["path"], "/api/v2/agent/turns")
            self.assertEqual(seen["payload"]["scope"], {"materialId": "m_1"})
        finally:
            server.close()

    def test_http_error_keeps_agent_code_and_message(self):
        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                body = json.dumps({"code": "AGENT_FEATURE_DISABLED", "message": "Agent is disabled"}).encode("utf-8")
                self.send_response(503)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, format, *args):
                pass

        server = ServerFixture(Handler)
        try:
            client = mod.AgentApiClient(server.base_url, timeout=2)
            with self.assertRaises(mod.ApiError) as raised:
                client.capabilities()
            self.assertEqual(raised.exception.code, "AGENT_FEATURE_DISABLED")
            self.assertIn("Agent is disabled", str(raised.exception))
        finally:
            server.close()


class CliInvocationTests(unittest.TestCase):
    def test_default_command_is_chat(self):
        from unittest import mock

        with mock.patch.object(mod, "run_chat", return_value=mod.EXIT_OK) as run_chat, \
             mock.patch.object(mod, "AgentApiClient", autospec=True) as client_type:
            client_type.return_value.list_knowledge_bases.return_value = []
            code = mod.main(["--knowledge-base", "kb_1"])
            self.assertEqual(code, mod.EXIT_OK)
            run_chat.assert_called_once()

    def test_non_interactive_ask_passes_no_input_fn_to_run_turn(self):
        from unittest import mock

        with mock.patch.object(mod, "run_turn", return_value=mod.EXIT_OK) as run_turn, \
             mock.patch.object(mod, "AgentApiClient", autospec=True) as client_type:
            client_type.return_value.capabilities.return_value = {}
            code = mod.main(["ask", "hello", "--knowledge-base", "kb_1", "--non-interactive"])
            self.assertEqual(code, mod.EXIT_OK)
            run_turn.assert_called_once()
            call_args, call_kwargs = run_turn.call_args
            self.assertIsNone(call_kwargs["input_fn"])

    def test_missing_scope_is_rejected(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "ask", "hello"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=10,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("one of --knowledge-base or --material is required", result.stderr)

    def test_root_launcher_shows_agent_usage(self):
        launcher = ROOT / "agent"
        if not launcher.exists():
            self.skipTest("agent launcher is not available on this checkout")
        result = subprocess.run(
            [str(launcher), "--help"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=10,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("usage: agent", result.stdout)


if __name__ == "__main__":
    unittest.main()
