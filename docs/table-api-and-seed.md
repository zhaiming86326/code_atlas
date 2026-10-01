# 四表接口与演示数据 SQL

## 本次完成

四张表继续使用阶段 2 结构，不需要额外建表迁移。原有导入、文档编辑/审核/索引/归档/删除、关键词搜索接口全部连接 JDBC；新增：

| 表 | 新增接口 |
|---|---|
| atlas_document_version | GET `/api/documents/{id}/versions/{versionId}`；POST `/api/documents/{id}/versions` |
| atlas_chunk | GET `/api/documents/{id}/versions/{versionId}/chunks`；GET `/api/chunks/{id}` |
| atlas_review | GET `/api/review-records`；GET `/api/review-records/{id}` |
| atlas_document | 保留已实现的列表、详情、元数据编辑、审核、重建、归档、删除和导入；概览改为真实导入/审核事件与七日趋势 |

所有接口及参数见 [API 契约](api-stage-2.md)。版本与分块关联严格校验；跨文档版本查询返回 404。手工版本用 `parser_version` 中的 `manual-revision` 标识，正文去重与来源导入复用同一事务路径，不覆盖原始历史快照。省略 metadata 时继承当前版本元数据。

分块属于衍生索引，只由审核/重新索引生成；审核历史属于审计数据，只追加决定。不会提供直接改写分块或历史审核记录的普通 CRUD，以免绕过审核或损坏正文/索引一致性。

前端详情新增“新增版本”、分块查看、审核历史分页；原有资料管理页面继续从 PostgreSQL 加载。真实 postgres 模式不会创建内存 DemoDataService，只有显式 demo 模式使用阶段 1 模拟服务。

## 演示数据

`database/002_demo_seed.sql` 包含：

- 原有 Spring Boot、PostgreSQL、Redis、Vue、Java 共 5 份演示文档。
- 原待审核列表另外 2 份演示候选（Redis 已与原文档合并）。
- 原演示问答转换为 1 份待审核笔记，保留示例引用文字，明确标明并非真实检索证据。

合计 **8 文档、8 版本、4 实际分块、4 演示审核记录**，4 份已索引、4 份待审核。所有标题加 `[演示]`，正文加说明，标签包含“演示数据”，作者和许可为未知，可信度为“演示数据，未验证”。原来源类型/语言/许可字段仅保留在演示正文说明中，不作为已核实事实。

这些数据不是在线来源快照，数据库来源类型统一为 MANUAL_NOTE；URL 仅保留参考链接。原界面虚构的 184/26 等分块数不入库，改为短演示正文各一个实际分块，并标记 `demo-single-chunk-v1`。重新索引时使用真实分块器重建。

原 dashboard 的假总量/假费用由实际聚合取代；不存在策略、采集任务和模型计费表，因此不把那些演示数值塞入四张内容表。采集策略/任务仍属于阶段 4，费用未配置时为未知。

SQL 使用固定 UUID 和 `DEMO:stage1:*` 来源键，**重复执行不新增、也不覆盖已有同来源数据和用户修改**。不会自动执行种子；本次经用户授权，已直接导入配置的远程 `code_atlas` 数据库，并执行第二遍验证幂等。

手工导入：

```powershell
psql -h YOUR_DB_HOST -U postgres -d code_atlas -v ON_ERROR_STOP=1 -f .\database\002_demo_seed.sql
```

## 本地 JDBC 配置与启动

用户原来配置在资源文件的连接已保留，移动至 **`backend/config/local.yml`**。此文件被 gitignore 忽略，位于资源目录之外，不打包进 JAR/前端，也不写入 SQL。配置仅在 postgres profile 导入；从工程根目录或 backend 目录启动均可找到。

`DB_URL`、`DB_USER`、`DB_PASSWORD` 环境变量仍可覆盖本地默认值。不要将本地配置提交或复制到公开部署目录；Docker 使用环境变量，不挂载本地密钥文件。

```powershell
# 已有 JDBC 配置，不需要重新填写密码
cd H:\github\code-atlas\backend
.\mvnw.cmd spring-boot:run
# 另一终端
cd H:\github\code-atlas\frontend
npm run dev
```

API 默认 `http://127.0.0.1:8080`，前端 `http://127.0.0.1:5173`。本次验证用临时端口 18080，验证完已关闭该测试服务；正常启动仍用 8080。

## 验收

- 用户配置的远程 JDBC：连接成功，导入前四表均为 0。
- 演示 SQL 执行两遍：两次均为 `(8,8,4,4)`。
- 后端 JUnit：3 个通过，0 失败；前端 TypeScript/Vite 构建通过。
- 真实 API 验证：版本详情、标签 JSON、分块分页/详情、审核历史/详情、重复审核、手工新增版本/继承元数据/去重、跨文档版本校验、非法格式、真实概览通过；原阶段 2 导入/搜索/版本/删除检查也通过。
- 测试只创建唯一名称的临时资料，结束删除；演示种子保留，未修改用户已有资料。
- 未执行浏览器点击验收、Docker 构建或备份恢复；本次没有调用 LLM、在线采集或运行采集代码。

可重复验收（会创建并清理临时测试资料）：

```powershell
python scripts/verify-stage2.py --base http://127.0.0.1:8080
```
