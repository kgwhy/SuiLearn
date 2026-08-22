# 最小验证选择

先运行：

```bash
python3 scripts/change_scope.py --base <base_ref>
```

再按变更路径选择最小验证。不默认全量；只报告真正执行过的命令。

| 变更路径 | 最小验证 |
| --- | --- |
| `apps/android/**` | `./gradlew :app:testDebugUnitTest --no-daemon`；涉及构建产物再跑 `assembleDebug` |
| `services/api/**` | `mvn -f services/api/pom.xml test -q`；涉及契约、存储、迁移或共享配置时扩大 |
| `contracts/**` | OpenAPI 校验 + 受影响消费端测试 |
| `apps/web/**` | `npm --prefix apps/web test && npm --prefix apps/web run build`；用户可见 UI 再补 UI 证据 |
| `.agents/**` | `python3 scripts/check_workflow_skill.py`；改 workflow reference 时再读 `forward-testing.md` |
| `.agents/notes/**` | `python3 scripts/check_agent_notes.py` |
| `docs/**`、`AGENTS.md`、workflow | `python3 scripts/check_suilearn_workflow.py --base-ref <ref>` |
| `scripts/**` | `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py'` |

规则：

- 不重复 pre-commit 已做的事。
- 没有适用测试时写明确的不适用原因，不得静默跳过。
- 全量测试只在用户明确要求、CI 失败定位或跨所有模块的变更时使用。
- 测试选择与覆盖率选择分开；不要用“只跑了部分测试”伪装“验证了全部受影响面”。
