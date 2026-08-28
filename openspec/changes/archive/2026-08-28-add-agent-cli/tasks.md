# Tasks: Add Agent REST CLI

Status: Approved

批准者：用户（2026-08-28 对话指令）

- [x] 1.1 Implement `scripts/agent_cli.py`
  - Owner: 本次 change 单一实现者
  - Allowed: `scripts/agent_cli.py`
  - Forbidden: `services/api/**`, `contracts/**`, `apps/**`, `docs/**`
  - Test: `python3 scripts/agent_cli.py --help`
- [x] 1.2 Add `tests/test_agent_cli.py`
  - Owner: 本次 change 单一实现者
  - Allowed: `tests/test_agent_cli.py`
  - Forbidden: 生产后端/前端/契约文件
  - Test: `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q`
- [x] 1.3 Write decision note and verification record
  - Owner: 本次 change 单一实现者
  - Allowed: `.agents/notes/implemented/architecture/2026-08-28-add-agent-rest-cli.md`, `openspec/changes/add-agent-cli/verification.md`
  - Forbidden: 业务代码
  - Test: `python3 scripts/check_agent_notes.py`
