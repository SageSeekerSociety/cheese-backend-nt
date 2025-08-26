测试 Client 当作 CLI 使用指南

目的
- 复用 PR 中的测试 Client（如 UserClient）做“手动命令”，不额外写代码；在需要时一条命令造数或拿 token。

原理
- 若测试类/方法上标记了 `@EnabledIfSystemProperty(named = "cli", matches = "true")`，则默认不跑；只有传入 `-Dcli=true` 时才启用。
- 通过 Maven 只跑指定测试方法（`-Dtest=类#方法`），等价于执行一条 CLI 命令。

快速开始
- 进入项目：
  cd cheese-backend-nt

- 创建用户并登录（打印 token），复用 `UserClientTest#createUserAndLogin`：
  ./mvnw -q -Dcli=true -Dtest=org.rucca.cheese.auth.UserClientTest#createUserAndLogin test

- 只创建用户/只登录：可在需要时为相应测试方法增加同样注解后，使用相同方式运行。

可选参数
- 如需让方法读取自定义参数，可在测试中使用 `System.getProperty("cli.xxx")` 读取；命令行传递 `-Dcli.xxx=...`。

前置依赖
- 使用测试配置（`@ActiveProfiles("test")` 已在测试类上声明）
  - PostgreSQL：jdbc:postgresql://localhost:5433/postgres（postgres/postgres）
  - Legacy 登录地址：`application.legacy-url`，默认 http://localhost:7779
  - 未启动 legacy 时，登录相关动作会失败（仅造用户成功）。

为什么不直接 java -jar？
- 这些 Client 在 test 源集中，且部分依赖 MockMvc/测试上下文。可执行 jar 不包含它们，也不会装配 MockMvc，因此直接 `java -jar` 无法使用。

