# 修复 Agent Turn Runtime Spring 装配

- Change: `fix-agent-turn-runtime-spring-wiring`
- Owner: Server Backend
- 级别: Standard
- base_ref: `e15bdd644db728256eee7907ef7aa6c69be54f34`
- 执行模式: L2
- 决策记录: 无新取舍；修复两个已发现的装配错误。

## 待办

- [x] 1.1 为 AgentTurnController 注入构造器标记 @Autowired
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/controller/AgentTurnController.java`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnControllerTest`
  - Review focus: 多构造器下 Spring 唯一候选
- [x] 1.2 移除重复 TurnExecutor bean，直接注入 TurnOrchestrator
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/runtime/AgentTurnRuntimeConfiguration.java`
  - Test: Docker 依赖下完整 `mvn test`
  - Review focus: 无 NoUniqueBeanDefinitionException
