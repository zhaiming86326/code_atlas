阶段 3 引用问答、模型配置与向量索引已实现，配置/迁移/验收步骤见 [docs/stage3.md](docs/stage3.md)。真实云端与 pgvector 验收仍需配置模型并在服务器安装扩展。

# CodeAtlas（代码图谱）

当前已完成 **阶段 2：手动入库与基础搜索**。保留阶段 1 演示模式；默认模式改为 PostgreSQL 真实数据。

先在服务器导入 [database/001_stage2.sql](database/001_stage2.sql)，再按 [阶段 2 操作说明](docs/stage-2.md) 设置后端连接。

已补齐四表接口与演示 SQL：[接口与种子数据说明](docs/table-api-and-seed.md)。演示种子使用 [database/002_demo_seed.sql](database/002_demo_seed.sql)，不会自动执行。

## 本阶段交付

- URL、Markdown/TXT/HTML 文件、个人笔记导入；UTF-8，最大 2 MiB。
- PostgreSQL 持久化原始快照、解析正文、元数据、历史版本、审核记录和章节分块。
- 审核、重新索引、归档、删除；数据库唯一约束与行锁保证重复/并发导入幂等。
- 中文和 API/类名/错误码关键词检索，技术、版本、来源、语言、采集日期筛选。
- 前端接通真实操作，网络错误默认不回退演示资料；不执行资料中的代码或指令。
- LLM/Embedding/采集 Worker 尚未实现，相关界面明确占位，未知费用不显示成零。

## 启动摘要

Windows PowerShell，先设置 DB_URL、DB_USER、DB_PASSWORD（详见操作说明）：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
# 另一终端
cd frontend
npm ci
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。Linux 后端用 `sh ./mvnw spring-boot:run`。

本地 Compose：复制 `.env.example` 为 `.env`，修改 DB_PASSWORD，再执行 `docker compose up --build`；访问 `http://127.0.0.1:8088`。SQL 只在空数据卷首次启动时自动执行，已有卷需要手动导入。

阶段 1 演示：后端设置 `SPRING_PROFILES_ACTIVE=demo`；仅想前端断网演示时可在 `frontend/.env.local` 设置 `VITE_DEMO_FALLBACK=true`。真实模式默认关闭回退。不要同时激活 postgres 与 demo。

## 验收与文档

```powershell
cd backend
.\mvnw.cmd -B -ntp test
cd ..\frontend
npm run build
cd ..
python scripts/verify-stage2.py --base http://127.0.0.1:8080
```

- [阶段 2 设计、导库、Windows/Linux 启动、备份恢复与限制](docs/stage-2.md)
- [阶段 2 API 契约](docs/api-stage-2.md)
- [阶段 2 验收记录](docs/stage-2-verification.md)
- [检索效果基准](docs/retrieval-baseline.md)
- [阶段 1 原始契约](docs/api-contract.md)

暂未实现登录，API、Vite、Compose 发布端口默认仅绑定本机；当前不能直接公开部署。下一阶段接通 Embedding、混合检索与带证据引用的云端问答。
