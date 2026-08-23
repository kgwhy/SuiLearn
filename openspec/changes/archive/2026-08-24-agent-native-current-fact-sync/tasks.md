# 当前事实收口任务

- Change: `agent-native-current-fact-sync`
- Owner: Product / Architect / Leader 协调
- 级别: Standard
- 基线引用: `d450780`

## 待办

- [x] 1.1 创建 change 包与政策
  - Allowed: `openspec/changes/agent-native-current-fact-sync/**`
  - Test: `python3 scripts/check_suilearn_workflow.py --base-ref d450780`
- [x] 2.1 同步产品规格
  - Allowed: `docs/product-requirements.md`
  - Test: `python3 scripts/check_suilearn_workflow.py --base-ref d450780`
- [x] 3.1 同步架构当前事实
  - Allowed: `docs/architecture.md`
  - Test: `python3 scripts/check_suilearn_workflow.py --base-ref d450780`
- [x] 4.1 同步技术基线
  - Allowed: `docs/tech-selection.md`
  - Test: `python3 scripts/check_suilearn_workflow.py --base-ref d450780`
- [x] 5.1 验证、自审与归档
  - Test: `python3 scripts/check_suilearn_workflow.py --base-ref d450780`；`python3 scripts/check_agent_notes.py`
