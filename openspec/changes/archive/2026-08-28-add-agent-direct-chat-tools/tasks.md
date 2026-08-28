# Tasks: Agent CLI 直达对话与 /tool 命令

Status: Approved

批准者：用户（2026-08-28 对话指令）

- [x] 1.1 Add root `agent` launcher and default-chat parser
  - Owner: 本次 change 单一实现者
  - Allowed: `agent`, `scripts/agent_cli.py`
  - Forbidden: `services/api/**`, `contracts/**`, `apps/**`, `docs/**`
  - Test: `./agent --help`
- [x] 1.2 Add `/tool` command and interactive scope selection
  - Owner: 本次 change 单一实现者
  - Allowed: `scripts/agent_cli.py`
  - Forbidden: 生产后端/前端/契约文件
  - Test: `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q`
- [x] 1.3 Update tests, decision note, and verification record
  - Owner: 本次 change 单一实现者
  - Allowed: `tests/test_agent_cli.py`, `.agents/notes/implemented/architecture/2026-08-28-add-agent-launcher-and-tool-command.md`, `openspec/changes/add-agent-direct-chat-tools/verification.md`
  - Forbidden: 业务代码
  - Test: `python3 scripts/check_agent_notes.py`
