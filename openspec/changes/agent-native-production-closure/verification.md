# 验证记录

Status: passed.

Owner: Test Agent（单人执行）
review_mode: single-agent
base_ref: `120b382`

## 定向验证

- 命令：`mvn -f services/api/pom.xml test -Dtest=<33 个 Agent/RAG/Material/Architecture 测试类>`
- 结果：Tests run: 88, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS
- 原始输出：`/tmp/final-target.log`

## 工作流验证

- `python3 scripts/change_scope.py --base 120b382`
- `python3 scripts/check_suilearn_workflow.py --base-ref 120b382`
- `python3 scripts/check_agent_notes.py`
- `python3 scripts/check_workflow_skill.py`
- `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py'`
- `git diff --check`

## 覆盖点

- rag_qa / question_generation 工具循环与 prompt policy。
- MemoryTurnRecorder 成功/失败/重复 snapshot 幂等，记忆异常不改变回合终态。
- RagService/SearchService 经 RagPipeline；embedding index version 幂等与换签名。
- 既有 material contract、architecture boundary、legacy scan 与 Agent 契约回归。
