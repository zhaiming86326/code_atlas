# CodeAtlas 阶段 1 API 契约

基础路径：`/api`。请求与响应均使用 UTF-8 JSON。

## 通用约定

- 时间字段在阶段 1 使用面向界面的字符串；阶段 2 持久化时统一改为 ISO 8601。
- 枚举值使用大写英文，前端负责中文显示。
- 列表接口将在阶段 2 统一支持 `page`、`pageSize`、`query`、`sort` 与筛选参数。
- 正式问答默认仅使用 `APPROVED + INDEXED` 的文档版本。

错误格式：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "问题不能为空",
  "timestamp": "2026-09-30T15:00:00Z",
  "details": {}
}
```

## 健康检查

### `GET /api/health`

返回服务版本、演示模式和服务器时间。

## 概览

### `GET /api/dashboard`

返回资料数、分块数、待审核数、运行策略数、失败任务、本月费用估算、最近动态和近 7 日趋势。

## 资料

### `GET /api/documents`

阶段 1 返回第一页演示资料：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

### `GET /api/documents/{id}`

返回单条资料的元数据、摘要和正文快照。不存在时返回 `404 NOT_FOUND`。

阶段 2 预留：

- `POST /api/imports/url`
- `POST /api/imports/file`
- `POST /api/imports/github`
- `PATCH /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `POST /api/documents/{id}/reviews`
- `POST /api/documents/{id}/reindex`

## 检索与回答

### `POST /api/search/answer`

请求：

```json
{
  "query": "Spring Boot 3.5 如何管理不同环境的配置？",
  "technologies": ["Spring Boot"],
  "version": "3.5.x",
  "searchMode": "HYBRID"
}
```

响应包含 `answer`、`citations`、`results` 和 `demo`。引用必须关联真实检索结果；阶段 1 固定返回带演示标记的回答。

## 主动采集

### `GET /api/collection-policies`

返回策略的主题、来源、频率、审核模式、每日数量限制、月度预算与启停状态。

### `GET /api/collection-tasks`

返回任务状态、发现数量、接受数量、耗时和错误或完成消息。

### `GET /api/reviews`

返回候选资料、相关度、发现原因和变化摘要。

阶段 4 预留：

- `POST /api/collection-policies`
- `PATCH /api/collection-policies/{id}`
- `POST /api/collection-policies/{id}/runs`
- `POST /api/collection-tasks/{id}/retry`
- `POST /api/reviews/{id}/approve`
- `POST /api/reviews/{id}/reject`

## 设置与预算

设置接口将在模型配置的加密存储方案确定后实现。API Key 永不通过读取接口返回，前端只显示是否已配置。

