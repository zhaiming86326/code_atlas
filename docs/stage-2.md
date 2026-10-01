# 阶段 2：手动入库与基础搜索

## 先导入服务器 SQL

交付脚本：`database/001_stage2.sql`。要求 PostgreSQL 16+、UTF-8 数据库，以及安装 `pg_trgm` 的权限；**阶段 2 不需要 vector 扩展**。脚本不会创建用户或数据库，不写入演示资料。先在服务器创建独立数据库和账号，再使用目标库的所有者执行脚本。

```powershell
# 在工程根目录执行。psql 会提示输入密码；不要将密码放进命令行。
psql -h YOUR_DB_HOST -p 5432 -U code_atlas -d code_atlas -v ON_ERROR_STOP=1 -f .\database\001_stage2.sql
```

Linux 同样使用 `psql ... -f database/001_stage2.sql`。DBeaver/pgAdmin 中请选择目标数据库，执行整个文件，确认 COMMIT 成功。若扩展权限不足，请管理员先在目标数据库执行 `CREATE EXTENSION IF NOT EXISTS pg_trgm;`。

脚本在事务中执行，可对**相同结构**重复运行；`IF NOT EXISTS` 不负责升级其他同名旧表。本次阶段 1 没有持久化表，因此这是首次迁移。应用默认不会自动执行 DDL，适合你自行导入服务器。

## 设计与关系

- `atlas_document`：来源身份、原始链接、归档状态；保存当前展示版本和正式检索版本两个指针。
- `atlas_document_version`：正文、原始二进制快照、正文/原始 SHA-256、元数据、解析器版本、审核状态、索引状态、采集时间。发布和来源更新时间无法确认时为 NULL，不用采集时间代替。
- `atlas_chunk`：版本下的章节路径与正文，保存分块配置。Markdown 围栏代码保留缩进和上下文，不运行代码。
- `atlas_review`：版本下的审核决策、备注和时间。单用户本地模式，无虚构审核人。

一个来源有多个版本，一个版本有多个分块和审核记录。指针的复合外键确保不能指向其他文档的版本。删除文档通过外键级联删除快照、版本、审核、分块。标签用版本内 `text[]`，技术与版本信息也属于版本，避免待审核新版本污染已批准旧版本的筛选。

采集队列、模型调用、向量与问答实体在阶段 3/4 再迁移，本阶段不创建空壳表。

## 已接通流程

1. 网页 URL、本地 Markdown/TXT/HTML、个人 Markdown 笔记导入。
2. HTML 清除脚本、导航等，优先提取 main/article；正文纯文本显示，不执行 HTML/代码。
3. 文件名（不含目录）、笔记标题、规范化后最终网页 URL 分别标识来源。同来源正文哈希未变则返回 `duplicate=true`，不新增版本；新内容创建待审核版本。不同来源保留独立溯源，不跨来源强行合并。
4. 审核批准与关键词索引分别保存状态；本阶段同一事务同步完成批准与分块，失败整体回滚，页面显示错误。重复批准不会新增审核记录或重复分块。
5. 新版本待审核时旧批准版本仍可搜索；批准后切换正式检索版本。审核提交携带 versionId，过期版本返回 409。
6. 中文与类名/API/错误码使用大小写不敏感 `ILIKE` 子串搜索，`pg_trgm` GIN 可加速长关键词；`%`、`_`、反斜杠按字面转义。版本采用**精确字符串匹配**，不解析语义版本范围。
7. 列表正文搜索、技术/状态筛选、分页；详情元数据编辑、版本正文、原始快照下载、重新索引、归档、永久删除。

搜索页面只给真实资料，不生成答案或引用编号；RAG 和语义检索在阶段 3。后端或数据库故障显示错误，默认不回退演示资料。模型与策略界面明确保留占位，费用为未知。

## Windows PowerShell 启动（连接你的服务器）

JDK 21、Node.js 22+。后端不自动加载根目录 `.env`，本地必须设置当前 PowerShell 的环境变量：

```powershell
cd H:\github\code-atlas
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:DB_URL = 'jdbc:postgresql://YOUR_DB_HOST:5432/code_atlas'
$env:DB_USER = 'code_atlas'
$securePassword = Read-Host '数据库密码' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
cd backend
.\mvnw.cmd spring-boot:run
```

如果通过公网连接 PostgreSQL，请使用服务器要求的 TLS 配置，例如 JDBC URL 加 `?sslmode=verify-full` 并配置可信 CA；也可走 SSH 私有隧道。不要开放数据库到全网。

另一终端：

```powershell
cd H:\github\code-atlas\frontend
npm ci
npm run dev
```

打开 `http://127.0.0.1:5173`，后端默认 `127.0.0.1:8080`。导入 → 查看正文/来源 → 批准 → 搜索关键词。字段未知可留空。更新文件时保持相同文件名；多个同名文件需要先重命名区分来源。

## Linux 启动

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL='jdbc:postgresql://YOUR_DB_HOST:5432/code_atlas'
export DB_USER=code_atlas
read -rsp 'Database password: ' DB_PASSWORD; echo
export DB_PASSWORD
cd backend
sh ./mvnw spring-boot:run
# 另一终端：cd frontend && npm ci && npm run dev
```

## Docker Compose（本地新库）

```powershell
Copy-Item .env.example .env
# 编辑 .env，设置 DB_PASSWORD；该文件已被 gitignore 忽略
docker compose up --build
```

打开 `http://127.0.0.1:8088`。Compose 自带数据库，初始化脚本只在**空数据卷首次启动**时执行；已有阶段 1 数据卷请手动导入 SQL，无需删除数据卷。Compose 后端使用容器内数据库地址，根 `.env` 中 DB_URL 不会覆盖它。连接外部服务器时优先按上述本地启动步骤，避免误以为 Compose 已使用外部库。

已有卷手动迁移示例（目标账号为 code_atlas）：

```powershell
docker compose cp .\database\001_stage2.sql postgres:/tmp/001_stage2.sql
docker compose exec postgres psql -U code_atlas -d code_atlas -v ON_ERROR_STOP=1 -f /tmp/001_stage2.sql
```

没有登录系统，API/网页/数据库端口默认仅绑定本机。Linux 对外部署前应加身份认证、TLS 和私有网络访问控制；目前不能直接公开。

## 验证

```powershell
cd backend
.\mvnw.cmd -B -ntp test
cd ..\frontend
npm run build
cd ..
# 测试 API 必须处于真实模式。优先使用单独测试数据库。
python .\scripts\verify-stage2.py --base http://127.0.0.1:8080
# 可选网络验证，会真实抓取；请确认来源允许访问
python .\scripts\verify-stage2.py --base http://127.0.0.1:8080 --url https://openjdk.org/jeps/444
```

脚本只使用 Python 标准库，创建测试资料，结束时删除这些资料。默认不联网获取来源；`--url` 指定来源可选。不自动执行采集到的代码。网络验证请使用测试库，避免指定已有资料的 URL 后产生更新。

已执行结果见 `docs/stage-2-verification.md`。固定问题基准见 `docs/retrieval-baseline.md`。

## 备份、恢复、重新索引

快照和解析结果均在 PostgreSQL，不依赖外部文件目录，整库备份即可：

```powershell
pg_dump -h YOUR_DB_HOST -U code_atlas -d code_atlas -Fc --file=code_atlas.dump
# 恢复到预先创建的空库；不覆盖现有数据库
pg_restore -h YOUR_DB_HOST -U code_atlas -d code_atlas_restore --no-owner --exit-on-error code_atlas.dump
```

Linux 使用相同命令。恢复账号需要相应建表/扩展权限；备份包含文档正文，请按个人资料保护。恢复后切换 DB_URL，在详情页点击重新索引。阶段 2 重新索引当前已批准版本；当前版本待审核时按钮返回明确错误，旧批准索引仍保留。全库批量重建和向量索引升级属于阶段 5。

## 依赖与边界

保持 Java 21 + Spring Boot 3.5.16；JDBC、PostgreSQL 驱动 42.7.11、Apache HttpClient 5.5.2 由固定 Boot BOM 管理；jsoup 固定 1.21.2。新依赖均已下载、编译并运行。Boot 对 Java 21 的兼容范围见 [官方系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)；子串索引依据 [PostgreSQL pg_trgm 文档](https://www.postgresql.org/docs/16/pgtrgm.html)。Compose 固定 pgvector `0.8.1-pg16`，本阶段不启用向量；Java/Node/Docker 基础镜像沿用阶段 1。

- 仅 UTF-8，文件/网页正文最多 2 MiB。网页仅 HTTP/HTTPS 标准端口、无凭证；每次连接 DNS 校验公网地址，拒绝私网、回环、链路本地及保留地址；重定向逐跳校验，最多 3 次。连接 5 秒、读超时 10 秒，下载循环检查 20 秒总预算（阻塞读可能延后一个读超时时间）。不自动重试或绕过登录/反爬机制。
- SSRF 测试覆盖常见 IPv4/IPv6 私网与本地地址；部署仍应使用出口防火墙，防止内部网络自定义公网路由。URL 导入由用户明确发起，请确认来源访问与内容使用许可；未确认许可标为未知。
- HTML 是通用正文提取，不支持 JS 渲染或站点专用解析；导航变化不影响正文时不会新增版本。相同正文的新原始页面不会另存快照。
- Markdown 原样保存；分块保留标题层级和围栏代码，超长代码/段落暂不截断（最高导入大小），阶段 3 接入 Embedding 时增加 token 限制。
- 关键词要求连续子串，不是自然语言意图检索或中文分词；短词可能扫描，适合个人小库。支持技术、版本、来源类型、语言、采集日期（UTC）组合筛选；语义版本范围解析后续扩展。
- 同步导入与关键词索引，无 Worker；采集/队列/恢复/预算执行在阶段 4，LLM/Embedding/RAG 在阶段 3。

下一步：在你导入 SQL 并配置连接后，用自己的文档验收；阶段 3 再增加独立 Embedding 配置、向量兼容标识、混合检索和证据引用。
