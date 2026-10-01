# 阶段 1 验证记录

验证日期：2026-10-01（北京时间）

## 已通过

### 前端

- Node.js 24.19.0
- `npm install`
- `npm run build`
- Vue TypeScript 类型检查通过
- Vite 生产构建通过
- 8 个主要路由均已进入构建产物

### 后端

- Eclipse Temurin JDK 21.0.12.1 LTS
- Spring Boot 3.5.16
- `./mvnw test`
- 业务演示数据契约测试：1 项通过，0 失败
- `./mvnw package -DskipTests`
- Spring Boot 可执行 JAR 构建通过
- 服务实际启动成功
- `GET /api/health` 返回 `status: ok`
- `GET /api/documents` 返回分页资料数据

## 已知限制

- PostgreSQL 与 pgvector 尚未接入，数据库容器只作为后续阶段预留。
- 导入、审核、采集、模型配置等写操作目前用于验证交互流程，不会持久化。
- 问答内容和引用为明确标记的演示结果。
- 前端全量引入 Element Plus，生产构建提示主包较大；后续阶段可改为组件按需引入。
- 当前执行环境不允许从外部浏览器访问本地预览端口，因此本轮以 TypeScript 检查、生产构建和接口运行结果完成自动验收。
