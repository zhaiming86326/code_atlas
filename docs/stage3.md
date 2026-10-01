# 阶段 3：引用问答与向量索引

实现代码已经写入项目。现有 PostgreSQL 配置保持可用，不自动安装服务器扩展、不要求启动时配置模型密钥。

## 数据关系与检索

沿用四张阶段 2 表。`atlas_model_config` 保存公开模型配置；密钥仍来自后端文件/环境变量。
`atlas_model_call` 保存调用状态、Token、价格依据及估算费用，不保存密钥、提示词或上游错误正文。
`atlas_qa` 保存问题、答复、筛选条件和引用原文快照，引用记录可追溯文档/版本/分块。
`atlas_embedding_profile` 用 Base URL、模型、维度、修订标识确定兼容配置。
`atlas_vector_index` 独立记录版本在该配置下的索引状态；`atlas_chunk_embedding` 按分块与配置保存向量。

关键词通过参数化 ILIKE 匹配完整问题、英文 API/错误码及中文双字词，不依赖 PostgreSQL 默认英文全文检索。中文双字词属于启发式召回，可能命中无关资料。
语义检索使用同一配置下的余弦距离，最低相似度 0.35，混合检索使用 RRF；RRF 排序分不是概率。
混合检索只纳入当前配置成功生成向量的版本。关键词模式无需向量，正式检索只使用批准且完成正文索引的可检索版本。
演示种子默认排除；页面勾选后显示演示标记。暂不开放待审核内容参与问答。

向量使用精确扫描，不要求 HNSW 的维度上限。模型返回向量必须与配置维度一致、有限且非零；不同模型不会混用。
正文重新索引会级联删除对应向量并将向量索引标记 STALE；版本删除/资料删除级联清理。
生成过程中不保持数据库长事务，最终落库前重新检查批准状态、版本和分块 ID。

## 数据库安装

现有数据库：按序执行以下文件，不会删除原有四表资料：

```powershell
psql -h YOUR_HOST -U YOUR_USER -d code_atlas -v ON_ERROR_STOP=1 -f database/003_rag_core.sql
psql -h YOUR_HOST -U YOUR_USER -d code_atlas -v ON_ERROR_STOP=1 -f database/004_pgvector.sql
```

当前已通过已配置 JDBC 导入 003；004 尚未导入，因为服务器 `pg_available_extensions` 中没有 vector。
在服务器安装与 PostgreSQL 主版本对应的 pgvector 后再执行 004。服务器安装方式见 [pgvector 官方说明](https://github.com/pgvector/pgvector#installation)。扩展安装需要服务器操作权限，JDBC 不能安装操作系统组件。

全新 Docker Compose 数据卷会按顺序导入 001、003、004。已有数据卷不会重跑初始化脚本，请手动执行上述迁移。不要删除已有数据卷。
Linux 命令同上；密码由 psql 交互读取或安全的 PGPASSFILE 提供。

## 模型配置

已经创建被 Git 忽略的 `backend/config/models.yml`，内容为无密钥模板。可直接在此填写 LLM/Embedding Base URL、模型、各自密钥、维度。保留 `${VAR:默认值}` 时，可用环境变量覆盖。

```powershell
# 也可以通过后端进程环境变量配置；不要把真实密钥写进可提交的脚本。
$env:LLM_BASE_URL = 'https://YOUR_PROVIDER/v1'
$env:LLM_MODEL = 'YOUR_CHAT_MODEL'
$env:EMBEDDING_BASE_URL = 'https://YOUR_EMBEDDING_PROVIDER/v1'
$env:EMBEDDING_MODEL = 'YOUR_EMBEDDING_MODEL'
$env:EMBEDDING_DIMENSIONS = '1536'
# LLM_API_KEY / EMBEDDING_API_KEY 分别在安全的本地环境中设置
```

Linux 使用 `export` 设置同名变量。Base URL 包含厂商要求的 `/v1`，系统追加 `/chat/completions`、`/embeddings`；部分厂商的路径不含 v1，应按其公开接口填写。

页面“模型与预算设置”保存的是公开配置，数据库配置优先于文件中的公开配置；密钥永远从文件/环境读取。修改文件后重启后端。
页面不支持 Azure 专有协议，仅支持标准 OpenAI 兼容协议。尚未验证任一实际云厂商的全部协议差异。
价格按 USD/百万 Token 填写并注明来源与日期。价格或 usage 未提供时费用未知；显示已知部分合计与未知调用数量，不将未知费用当零。
更换同名模型权重时递增修订标识，然后重新生成向量。

## 启动与使用

PowerShell 两个终端：

```powershell
cd H:\github\code-atlas\backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd H:\github\code-atlas\frontend
npm ci
npm run dev
```

Linux：后端目录 `sh ./mvnw spring-boot:run`；前端命令相同。浏览器访问 http://127.0.0.1:5173。
Docker：根目录复制 `.env.example` 为 `.env`，填写配置后 `docker compose up --build`，访问 http://127.0.0.1:8088。

1. 导入真实文件/网页/笔记，填写技术、版本并批准。
2. 打开文档详情，点击“为当前可检索版本生成向量”。批准/正文索引成功与向量成功是不同状态。
3. 搜索页选择关键词、语义或混合。“检索证据”不调用生成模型；语义/混合会调用 Embedding。
4. “生成引用回答”调用 LLM，引用可定位具体历史版本与分块，原文片段必须连续逐字匹配。
5. 无证据则说明不足；同技术多个版本且未填版本筛选时，先要求明确版本。
6. 模型输出的 JSON、编号、引用原文不合法时返回 502，阻止不可靠答案，保留失败状态。

模型系统提示隔离文档指令，没有注册工具，不执行文档代码。引用校验只能确保编号/原文真实，不能保证所有结论语义被证据支持；请复核技术选型与性能推断。

## API 契约

- `GET /api/models`：配置（只有 keyConfigured）、pgvector 就绪状态、月度调用统计、最近 50 条记录。
- `PUT /api/models`：LLM 与 EMBEDDING 两条公开配置数组，字段见 models.example.yml 和页面；不接受密钥字段。
- `GET/POST /api/documents/{id}/embedding-index`：查看/生成可检索版本向量；成功配置重复调用复用。
- `POST /api/rag/retrieve`：返回最多 8 个分块证据。
- `POST /api/rag/answer`：返回 answer/status/citations/results/demo/searchMode。
- `GET /api/rag/history`：最近 50 个问题与引用快照。
- `/api/search/answer` 保留阶段 2 关键词协议，兼容已有脚本。

请求示例：

```json
{"query":"虚拟线程适用于什么场景？","technologies":["Java"],"version":"21","searchMode":"HYBRID","sourceType":"","language":"zh","from":"","to":"","includeDemo":false}
```

技术/版本/语言/来源精确匹配，时间按 UTC 采集日期筛选；不声称支持任意 semver 范围推理。

## 验收与当前结果

```powershell
cd backend
.\mvnw.cmd test
cd ..\frontend
npm run build
cd ..
python scripts/verify-stage2.py
python scripts/verify-stage3.py
# 模型与 pgvector 完成配置后执行：会产生实际厂商调用费用
python scripts/verify-stage3.py --vectors
```

已执行：Java 构建/5 个测试、前端 TypeScript 与生产构建；阶段 2 真实 PostgreSQL 回归；阶段 3 真实 PostgreSQL 中文/精确 API 检索、审核隔离、版本筛选、版本歧义、资料不足、演示排除、归档排除、缺少 pgvector 的错误提示。
Spring AI 1.1.2 的实际 HTTP 请求已由本机明确标记 test-only 的测试服务验证：Embedding 与 Chat 路径、响应解析、维度验证。引用虚构编号与虚构原文会被测试拦截。
未执行：真实云端 LLM/Embedding、pgvector SQL 与距离检索、Docker Compose 运行及浏览器视觉验收。当前没有密钥、远程 pgvector 不可用。
固定问题：虚拟线程适用场景、不存在的 API、Java 21/22 版本冲突、Class.forName 代码示例、Spring Boot/Quarkus 比较；后两项需先导入相应证据，缺少资料应拒答。

## 限制与下一阶段

手动向量索引同步执行，每次最多 32 分块、每块 12000 字符；长代码拒绝而非任意截断。索引失败可重试；进程中断的 INDEXING 记录 30 分钟后可重新领取。
网络连接 5 秒、请求读取 45 秒，模型调用不自动重试以避免重复收费。暂未实现后台批量索引队列、Reranker、问题触发联网、采集预算硬限制；这些随阶段 4/5 补齐。
没有登录，默认绑定本机。不要将 API 直接暴露公网；模型设置属于可信管理员配置。
备份沿用 PostgreSQL pg_dump，备份包含新表、向量与引用快照；恢复服务器必须先安装 pgvector。密钥文件应单独安全备份。
版本依据：[Spring AI 1.1 系列与 Spring Boot 3.5 兼容说明](https://github.com/spring-projects/spring-ai/blob/main/README.md?plain=1)；项目固定 spring-ai-openai 1.1.2。

## 本机 Ollama Embedding

Embedding 已支持 `http://127.0.0.1:11434/v1` 或 `http://localhost:11434/v1`；其他地址仍要求 HTTPS。URL 不允许凭证、查询参数或片段。无需启用测试服务开关。
使用模型 `qwen3-embedding:0.6b`、维度 1024、占位 api-key `ollama`，在 models.yml 的 code-atlas.models.embedding 下配置。价格保持未知，注明本机推理的硬件与电费未估算。
文件修改后重启后端；页面保存的公开配置优先于文件。更换模型后重新生成向量。
本机 HTTP 配置及拒绝非本机 HTTP 已通过测试；实际 Qwen 模型尚需安装后联调。

## 模型诊断日志

模型调用开始/成功使用 INFO，失败使用 WARN，默认输出到后端启动终端。向量索引开始与数据库提交也默认打印；单块进度为 DEBUG。
PowerShell 启动前设置 `$env:CODE_ATLAS_LOG_LEVEL = 'DEBUG'` 可显示分块进度，随后运行 `.\mvnw.cmd spring-boot:run`。需要重启后端才能加载新代码与环境变量。
模型错误现在保留 HTTP 状态码和安全分类（404 模型/路径不存在、401/403 权限、429 限流、连接/超时、维度不匹配）；不记录上游正文、密钥或文档内容。最近失败原因在模型设置页调用记录中查看。
本机 Ollama 必须先下载配置中完全相同的模型：`ollama pull qwen3-embedding:0.6b`，用 `ollama list` 确认。

### 2026-10-01 本机补充验收

已下载 `qwen3-embedding:0.6b`，实际 Ollama `/v1/embeddings` 返回 1024 维（首次加载约 30 秒）。通过正在运行的真实 API 验证：临时笔记批准后向量入库、重复索引复用、pgvector 混合检索返回目标分块、正文重新索引后向量状态 STALE。测试资料已删除。此前“实际 Qwen/pgvector 未联调”的限制已在此项验证中解除；真实云端 LLM 引用回答仍未执行。
新增日志代码通过测试，正在运行的后端必须重启后才生效。

### 引用生成与校验修复

DeepSeek 官方接口的引用问答请求显式关闭 thinking，启用 JSON object 输出；输出上限为 3000 Token。空正文/长度截断会单独报告，并记录 finishReason 和正文字符数，不打印正文。
模型只选择检索证据编号；引用原文由后端直接取对应分块。若兼容模型仍返回 quote，仍逐字校验，伪造摘录会被拒绝。未知编号仍被拒绝；代码块/行内代码中的数组下标不作为引用编号。接受完整 JSON 围栏，不接受 JSON 后附加其他对象。
2026-10-01 已通过真实 DeepSeek + 本机 Qwen3-Embedding-0.6B + pgvector 混合问答验收，引用对应实际分块原文；临时测试资料已删除。原先“真实云端引用回答未执行”的限制在本次验收中解除。该验证不代表每个实际问题的回答语义都正确，引用校验只保证证据编号与原文来源。
更新 Java 源码后需重启后端；无需因本次问答修复重建向量。
