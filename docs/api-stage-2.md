# 阶段 2 API（真实 postgres 模式）

前缀 `/api`。分块由审核/重新索引生成，历史版本由导入或新增版本写入，审核记录由决策接口追加；不提供绕过审核的直接分块写入或修改历史审核记录接口。错误返回 `{code,message,timestamp,details}`；校验 400、不存在 404、过期审核 409、过大上传 413、来源获取失败 502、数据库失败 503。不存在的路由/框架级错误可能使用 Spring 默认错误体。

| 方法 / 路径 | 行为 |
|---|---|
| GET /health | 模式和服务状态；demoMode=false 表示真实模式，数据库连通性请用 /actuator/health |
| GET /dashboard | 真实文档/分块/待审核计数；模型费用为 null，采集计数暂为 0 |
| GET /documents | query（标题、正文、标签）、technology、status、page、pageSize；pageSize 1..100，默认 20 |
| GET /documents/{id} | 当前展示版本，字段兼容阶段 1 Document |
| GET /documents/{id}/metadata | 作者、可信度、创建时间、当前/可检索版本 ID |
| GET /documents/{id}/versions/{versionId} | 单版本详情、完整元数据与正文，不返回 bytea；校验文档归属 |
| POST /documents/{id}/versions | `{content,format?,metadata?}` 创建手工待审核版本；格式 MARKDOWN/TXT/HTML，metadata 省略继承当前版本 |
| GET /documents/{id}/versions/{versionId}/chunks | query/page/pageSize 查询该版本分块，未索引返回空列表 |
| GET /chunks/{id} | 分块详情与所属文档/版本/索引状态 |
| GET /review-records | documentId/versionId/decision/page/pageSize 过滤并分页审核历史 |
| GET /review-records/{id} | 单条审核记录详情 |
| GET /documents/{id}/versions | 历史版本，按 sequence_no 倒序；含正文、哈希、解析器、审核/索引状态与采集时间 |
| GET /documents/{id}/versions/{versionId}/raw | 原始快照二进制下载，不在浏览器执行原始 HTML |
| POST /imports/url | `{url,metadata?}`；同步抓取解析 |
| POST /imports/file | multipart：file；可选 metadata part 类型必须为 application/json |
| POST /imports/note | `{title,content,metadata?}`；Markdown 笔记 |
| PUT /documents/{id} | 替换当前版本 Metadata；没有填写的字段变为未知，历史版本元数据不变 |
| POST /documents/{id}/review | `{versionId,approved,note?}`；审核当前版本并同步关键词索引/撤销 |
| POST /documents/{id}/reindex | 仅重建当前已批准版本；未批准返回 400 |
| POST /documents/{id}/archive?archived=true | 归档；false 取消归档 |
| DELETE /documents/{id} | 永久删除及级联关联内容；空响应体 |
| GET /reviews | 当前版本待审核的未归档资料；relevance=0 表示未计算相关性，不是低相关判断 |
| POST /search/answer | 真实关键词检索，answer 是状态说明，citations 为空、demo=false；不调用 LLM |
| GET /collection-policies、/collection-tasks | 阶段 4 尚未接通，返回空列表 |

Metadata：`{title,technology,version,language,tags,author,license,trust}`；除 tags 为字符串数组外，均为字符串。未知字段留空/null，将存为“未知”；导入 title 留空时从 Markdown 一级标题、HTML title 或文件名取得，不生成作者或许可。最多 30 个标签，每个最多 100 字符；一般元数据字段最多 1000 字符。

导入响应：`{document,duplicate,versionId}`。同来源正文不变时 duplicate=true，versionId 为已有匹配版本（可能是历史版本）；不会把旧内容自动回滚为当前版本。来源键：类型+文件名、类型+笔记标题、类型+规范化后的最终 URL。

搜索请求示例：

```json
{
  "query": "虚拟线程",
  "technologies": ["Java"],
  "version": "21",
  "searchMode": "KEYWORD",
  "sourceType": "MANUAL_NOTE",
  "language": "zh",
  "from": "2026-10-01",
  "to": "2026-10-01"
}
```

query 必填、最多 500 字符，最多 20 种技术；其他筛选可以省略/空。日期按采集时间 UTC，from 包含当天 00:00，to 包含整天。version 是精确元数据字符串，例如 `3.5.x` 不会与 `3.5.16` 自动匹配。关键词大小写不敏感、连续子串，无自然语言扩展。最多返回 20 条正式资料，实际正文来自 searchable_version_id，可能不是列表中尚未审核的 current_version_id。响应 resultVersions 将文档 ID 映射到实际命中的版本 ID，前端链接可定位该历史快照。

阶段 2 的 INDEXED 仅代表关键词分块索引成功，不代表已有 Embedding。审批与索引字段分别持久化，但操作采用同一事务，索引失败不会留下批准成功的假象。旧已批准历史版本保留分块以供追溯，正式搜索每文档仅用一个版本。

上传 multipart 示例（不设 JSON Content-Type）：

```powershell
curl.exe -F "file=@C:\notes\java.md" http://127.0.0.1:8080/api/imports/file
```

审核示例：先获取 versions[0].id，再 POST review；前端已完成这一流程。服务无登录/用户权限模型，只能在本机或受控私网使用。
