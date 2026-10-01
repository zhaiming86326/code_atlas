-- Stage 1 demo fixtures mapped to the four stage 2 tables. NOT scraped source documents.
-- Optional seed, never executed automatically. Re-run inserts missing fixtures only; user changes are preserved.
-- All fixtures use deterministic UUIDs and DEMO:stage1:* source keys.
BEGIN;

-- doc-001 Spring Boot 3.5 Reference Documentation
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:doc-001') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('5a198535-004c-56bf-ac89-6ed1ec4da246','DEMO:stage1:doc-001','MANUAL_NOTE','Spring（演示）','https://docs.spring.io/spring-boot/','2026-09-29 16:20:00+08','2026-09-29 16:20:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('b5fe1288-e425-53ea-8411-9b1031caf805','5a198535-004c-56bf-ac89-6ed1ec4da246',1,'8c8e54f0ad7ab8e7890146dfaa7c4fe69c11d8f707f0e9a52d81feefc44bd12f','8c8e54f0ad7ab8e7890146dfaa7c4fe69c11d8f707f0e9a52d81feefc44bd12f',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a4f4646494349414c5f444f43efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a656eefbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9a4170616368652d322e30efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0a2320537072696e6720426f6f7420332e350a0ae69cace69687e6a1a3e4bb8be7bb8de887aae58aa8e9858de7bdaee38081e5a496e983a8e58c96e9858de7bdaee3808157656220e5bc80e58f91e38081e695b0e68daee8aebfe997aee38081e6b58be8af95e5928ce7949fe4baa7e8bf90e7bbb4e380820a0a23232045787465726e616c697a656420436f6e66696775726174696f6e0a0a537072696e6720426f6f7420e694afe68c812070726f70657274696573e3808159414d4ce38081e78eafe5a283e58f98e9878fe5928ce591bde4bba4e8a18ce58f82e695b0e38082','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：OFFICIAL_DOC；原语言字段：en；原许可字段：Apache-2.0（均为演示字段）。

# Spring Boot 3.5

本文档介绍自动配置、外部化配置、Web 开发、数据访问、测试和生产运维。

## Externalized Configuration

Spring Boot 支持 properties、YAML、环境变量和命令行参数。','stage1-demo-seed-v1','[演示] Spring Boot 3.5 Reference Documentation','Spring Boot','3.5.x','zh',ARRAY['Java','Web','官方文档','演示数据'],'未知','未知','演示数据，未验证','APPROVED','INDEXED','2026-09-29 16:20:00+08');
INSERT INTO atlas_chunk(id,version_id,ordinal,section_path,content,chunker_version) VALUES ('a932a037-ccc2-5730-bd7e-150ff53e276d','b5fe1288-e425-53ea-8411-9b1031caf805',0,'演示资料 / Spring Boot 3.5 Reference Documentation','> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：OFFICIAL_DOC；原语言字段：en；原许可字段：Apache-2.0（均为演示字段）。

# Spring Boot 3.5

本文档介绍自动配置、外部化配置、Web 开发、数据访问、测试和生产运维。

## Externalized Configuration

Spring Boot 支持 properties、YAML、环境变量和命令行参数。','demo-single-chunk-v1');
INSERT INTO atlas_review(id,version_id,decision,note,reviewed_at) VALUES ('ebd4185e-bdc1-5432-ab12-21a8cfd7d237','b5fe1288-e425-53ea-8411-9b1031caf805','APPROVED','阶段 1 演示已索引状态转换；不是实际人工验证','2026-09-29 16:20:00+08');
UPDATE atlas_document SET current_version_id='b5fe1288-e425-53ea-8411-9b1031caf805',searchable_version_id='b5fe1288-e425-53ea-8411-9b1031caf805' WHERE id='5a198535-004c-56bf-ac89-6ed1ec4da246';
END IF;
END $seed$;

-- doc-002 PostgreSQL 16：向量检索与全文检索组合实践
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:doc-002') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('10610c47-5cee-5368-87c7-327768ecea23','DEMO:stage1:doc-002','MANUAL_NOTE','个人笔记（演示）',NULL,'2026-09-28 21:45:00+08','2026-09-28 21:45:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('0e635203-c52c-56dd-9ce8-4d0ca8a53c52','10610c47-5cee-5368-87c7-327768ecea23',1,'9447cf3934bafea6377e6446d5d25a5da4caff19717e9e66a5381516abd1684c','9447cf3934bafea6377e6446d5d25a5da4caff19717e9e66a5381516abd1684c',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a4d414e55414c5f4e4f5445efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a7a68efbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9a50726976617465efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0a2320506f737467726553514c20e6b7b7e59088e6a380e7b4a20a0ae7b2bee7a1aee79a84e7b1bbe5908de38081e99499e8afafe7a081e5928c2041504920e5908de7a7b0e4bdbfe794a8e585a8e69687e6a380e7b4a2e58face59b9eefbc8ce887aae784b6e8afade8a880e997aee9a298e4bdbfe794a8e59091e9878fe58face59b9eefbc8ce69c80e5908ee9809ae8bf872052524620e59088e5b9b6e68e92e5908de38082','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：MANUAL_NOTE；原语言字段：zh；原许可字段：Private（均为演示字段）。

# PostgreSQL 混合检索

精确的类名、错误码和 API 名称使用全文检索召回，自然语言问题使用向量召回，最后通过 RRF 合并排名。','stage1-demo-seed-v1','[演示] PostgreSQL 16：向量检索与全文检索组合实践','PostgreSQL','16','zh',ARRAY['pgvector','混合检索','RAG','演示数据'],'未知','未知','演示数据，未验证','APPROVED','INDEXED','2026-09-28 21:45:00+08');
INSERT INTO atlas_chunk(id,version_id,ordinal,section_path,content,chunker_version) VALUES ('70f9c982-532e-5db2-9cf0-c2e8d0d38165','0e635203-c52c-56dd-9ce8-4d0ca8a53c52',0,'演示资料 / PostgreSQL 16：向量检索与全文检索组合实践','> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：MANUAL_NOTE；原语言字段：zh；原许可字段：Private（均为演示字段）。

# PostgreSQL 混合检索

精确的类名、错误码和 API 名称使用全文检索召回，自然语言问题使用向量召回，最后通过 RRF 合并排名。','demo-single-chunk-v1');
INSERT INTO atlas_review(id,version_id,decision,note,reviewed_at) VALUES ('d85aaadd-a16d-5ba5-b8ab-0031ecdd3063','0e635203-c52c-56dd-9ce8-4d0ca8a53c52','APPROVED','阶段 1 演示已索引状态转换；不是实际人工验证','2026-09-28 21:45:00+08');
UPDATE atlas_document SET current_version_id='0e635203-c52c-56dd-9ce8-4d0ca8a53c52',searchable_version_id='0e635203-c52c-56dd-9ce8-4d0ca8a53c52' WHERE id='10610c47-5cee-5368-87c7-327768ecea23';
END IF;
END $seed$;

-- doc-003 Redis 8.2 Release Notes
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:doc-003') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('996e3663-1ff3-5036-8002-aedf64cbb897','DEMO:stage1:doc-003','MANUAL_NOTE','redis/redis（演示）','https://github.com/redis/redis/releases','2026-09-30 08:12:00+08','2026-09-30 08:12:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('cf575ab5-f944-558b-9748-5df8c8c749a7','996e3663-1ff3-5036-8002-aedf64cbb897',1,'3aee72b19e508d6fb34a272320bfca79123b94decc999214926d095d2601baa3','3aee72b19e508d6fb34a272320bfca79123b94decc999214926d095d2601baa3',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a4749544855425f52454c45415345efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a656eefbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9a4253442d332d436c61757365efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0ae5b09ae69caae689b9e58786efbc8ce689b9e58786e5908ee6898de4bc9ae8a7a3e69e90e6ada3e69687e5b9b6e5bbbae7ab8be6ada3e5bc8fe7b4a2e5bc95e38082','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：GITHUB_RELEASE；原语言字段：en；原许可字段：BSD-3-Clause（均为演示字段）。

尚未批准，批准后才会解析正文并建立正式索引。','stage1-demo-seed-v1','[演示] Redis 8.2 Release Notes','Redis','8.2','zh',ARRAY['Release','升级','缓存','演示数据'],'未知','未知','演示数据，未验证','PENDING_REVIEW','NOT_INDEXED','2026-09-30 08:12:00+08');
UPDATE atlas_document SET current_version_id='cf575ab5-f944-558b-9748-5df8c8c749a7',searchable_version_id=NULL WHERE id='996e3663-1ff3-5036-8002-aedf64cbb897';
END IF;
END $seed$;

-- doc-004 Vue 3 Composition API 常见陷阱
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:doc-004') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('7d957e83-8fd4-5c4a-a718-aa8ae7562472','DEMO:stage1:doc-004','MANUAL_NOTE','Vue.js Blog（演示）','https://vuejs.org/guide/','2026-09-27 10:30:00+08','2026-09-27 10:30:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('36d64e9e-32da-50fc-bb74-867360478dbb','7d957e83-8fd4-5c4a-a718-aa8ae7562472',1,'dd5b1ea981fd9292c74fea1b983f106e1a528c4ce729037fd2c0acc577621b79','dd5b1ea981fd9292c74fea1b983f106e1a528c4ce729037fd2c0acc577621b79',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a5745425f50414745efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a7a68efbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9a4d4954efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0a2320436f6d706f736974696f6e204150490a0ae4bf9de79599e5938de5ba94e5bc8fe5bc95e794a8e697b6e4bdbfe794a820746f52656673efbc9b776174636845666665637420e98082e59088e887aae58aa8e8b79fe8b8aae5908ce6ada5e689a7e8a18ce69c9fe997b4e8afbbe58f96e79a84e4be9de8b596e38082','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：WEB_PAGE；原语言字段：zh；原许可字段：MIT（均为演示字段）。

# Composition API

保留响应式引用时使用 toRefs；watchEffect 适合自动跟踪同步执行期间读取的依赖。','stage1-demo-seed-v1','[演示] Vue 3 Composition API 常见陷阱','Vue','3.5','zh',ARRAY['前端','Composition API','演示数据'],'未知','未知','演示数据，未验证','APPROVED','INDEXED','2026-09-27 10:30:00+08');
INSERT INTO atlas_chunk(id,version_id,ordinal,section_path,content,chunker_version) VALUES ('c3b8c5dd-e339-5fe0-bdf1-06446c9c7f75','36d64e9e-32da-50fc-bb74-867360478dbb',0,'演示资料 / Vue 3 Composition API 常见陷阱','> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：WEB_PAGE；原语言字段：zh；原许可字段：MIT（均为演示字段）。

# Composition API

保留响应式引用时使用 toRefs；watchEffect 适合自动跟踪同步执行期间读取的依赖。','demo-single-chunk-v1');
INSERT INTO atlas_review(id,version_id,decision,note,reviewed_at) VALUES ('bc11a3d3-b734-505c-852e-9172a2c5d976','36d64e9e-32da-50fc-bb74-867360478dbb','APPROVED','阶段 1 演示已索引状态转换；不是实际人工验证','2026-09-27 10:30:00+08');
UPDATE atlas_document SET current_version_id='36d64e9e-32da-50fc-bb74-867360478dbb',searchable_version_id='36d64e9e-32da-50fc-bb74-867360478dbb' WHERE id='7d957e83-8fd4-5c4a-a718-aa8ae7562472';
END IF;
END $seed$;

-- doc-005 Java 21 Virtual Threads Guide
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:doc-005') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('f0bb0cc6-94ae-57a4-bc21-e688cdac604b','DEMO:stage1:doc-005','MANUAL_NOTE','OpenJDK（演示）','https://openjdk.org/jeps/444','2026-09-26 14:10:00+08','2026-09-26 14:10:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('daba4294-fa31-5ba5-836f-8867669e1890','f0bb0cc6-94ae-57a4-bc21-e688cdac604b',1,'b02893017e2dc24168060a472805721c041f4c8c472808ddd866df8678724917','b02893017e2dc24168060a472805721c041f4c8c472808ddd866df8678724917',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a4f4646494349414c5f444f43efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a656eefbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9a47504c2d322e30efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0a23205669727475616c20546872656164730a0ae8999ae68b9fe7babfe7a88be98082e59088e5a4a7e9878fe4bba5e998bbe5a19e20492f4f20e4b8bae4b8bbe79a84e5b9b6e58f91e4bbbbe58aa1efbc8ce4b88de794a8e4ba8ee8aea92043505520e5af86e99b86e59e8be4bbbbe58aa1e8bf90e8a18ce5be97e69bb4e5bfabe38082','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：OFFICIAL_DOC；原语言字段：en；原许可字段：GPL-2.0（均为演示字段）。

# Virtual Threads

虚拟线程适合大量以阻塞 I/O 为主的并发任务，不用于让 CPU 密集型任务运行得更快。','stage1-demo-seed-v1','[演示] Java 21 Virtual Threads Guide','Java','21','zh',ARRAY['并发','虚拟线程','JEP 444','演示数据'],'未知','未知','演示数据，未验证','APPROVED','INDEXED','2026-09-26 14:10:00+08');
INSERT INTO atlas_chunk(id,version_id,ordinal,section_path,content,chunker_version) VALUES ('8331ac2e-6df7-5d94-a714-bb75514f1a81','daba4294-fa31-5ba5-836f-8867669e1890',0,'演示资料 / Java 21 Virtual Threads Guide','> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：OFFICIAL_DOC；原语言字段：en；原许可字段：GPL-2.0（均为演示字段）。

# Virtual Threads

虚拟线程适合大量以阻塞 I/O 为主的并发任务，不用于让 CPU 密集型任务运行得更快。','demo-single-chunk-v1');
INSERT INTO atlas_review(id,version_id,decision,note,reviewed_at) VALUES ('c2b67259-6038-5f45-948a-e5798d93d6bd','daba4294-fa31-5ba5-836f-8867669e1890','APPROVED','阶段 1 演示已索引状态转换；不是实际人工验证','2026-09-26 14:10:00+08');
UPDATE atlas_document SET current_version_id='daba4294-fa31-5ba5-836f-8867669e1890',searchable_version_id='daba4294-fa31-5ba5-836f-8867669e1890' WHERE id='f0bb0cc6-94ae-57a4-bc21-e688cdac604b';
END IF;
END $seed$;

-- review-2 Spring Boot 3.5.7 available now
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:review-2') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('cee46bc0-43a3-51af-be42-536d9e0b7e1d','DEMO:stage1:review-2','MANUAL_NOTE','spring.io/blog（演示）',NULL,'2026-10-01 08:00:00+08','2026-10-01 08:00:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('04af2544-4b79-5b48-a510-4072a334da54','cee46bc0-43a3-51af-be42-536d9e0b7e1d',1,'acb1f0ee94770e1eb8c5465bceaef3d7451f668bd10ecf43d9693cb35a8e3b32','acb1f0ee94770e1eb8c5465bceaef3d7451f668bd10ecf43d9693cb35a8e3b32',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a5245564945575f43414e444944415445efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9ae69caae79fa5efbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9ae69caae79fa5efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0ae591bde4b8ade585b3e6b3a8e78988e69cac20332e7820e5928ce5ae98e696b9e69da5e6ba900a0ae6bc94e7a4bae58f98e58c96e58897e8a1a8efbc88e69caae7bb8fe69da5e6ba90e9aa8ce8af81efbc89efbc9a0a2d20e4be9de8b596e58d87e7baa70a2d20e5ae89e585a8e4bfaee5a48d0a2d20e9858de7bdaee58583e695b0e68daee4bfaee6ada3','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：REVIEW_CANDIDATE；原语言字段：未知；原许可字段：未知（均为演示字段）。

命中关注版本 3.x 和官方来源

演示变化列表（未经来源验证）：
- 依赖升级
- 安全修复
- 配置元数据修正','stage1-demo-seed-v1','[演示] Spring Boot 3.5.7 available now','Spring Boot','3.5.7','zh',ARRAY['审核候选','演示数据'],'未知','未知','演示数据，未验证','PENDING_REVIEW','NOT_INDEXED','2026-10-01 08:00:00+08');
UPDATE atlas_document SET current_version_id='04af2544-4b79-5b48-a510-4072a334da54',searchable_version_id=NULL WHERE id='cee46bc0-43a3-51af-be42-536d9e0b7e1d';
END IF;
END $seed$;

-- review-3 PostgreSQL 18 Query Optimizer Deep Dive
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:review-3') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('71d63886-be4c-5a63-a20c-6f463748b8ad','DEMO:stage1:review-3','MANUAL_NOTE','example.dev（演示）',NULL,'2026-10-01 08:00:00+08','2026-10-01 08:00:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('5c82e042-9280-5d59-bb2b-1cdc8d0d6fbd','71d63886-be4c-5a63-a20c-6f463748b8ad',1,'146a17e8bda2f24c94fabde806da5679467e93e61bf1b3d923044bd61a8dc599','146a17e8bda2f24c94fabde806da5679467e93e61bf1b3d923044bd61a8dc599',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a5245564945575f43414e444944415445efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9ae69caae79fa5efbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9ae69caae79fa5efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0ae4b8bbe9a298e79bb8e585b3efbc8ce4bd86e78988e69cace4b88de59ca8e5bd93e5898d203136e28094313720e88c83e59bb40a0ae6bc94e7a4bae58f98e58c96e58897e8a1a8efbc88e69caae7bb8fe69da5e6ba90e9aa8ce8af81efbc89efbc9a0a2d20e4bc98e58c96e599a8e58f98e58c960a2d20e680a7e883bde6b58be8af95','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：REVIEW_CANDIDATE；原语言字段：未知；原许可字段：未知（均为演示字段）。

主题相关，但版本不在当前 16—17 范围

演示变化列表（未经来源验证）：
- 优化器变化
- 性能测试','stage1-demo-seed-v1','[演示] PostgreSQL 18 Query Optimizer Deep Dive','PostgreSQL','18','zh',ARRAY['审核候选','演示数据'],'未知','未知','演示数据，未验证','PENDING_REVIEW','NOT_INDEXED','2026-10-01 08:00:00+08');
UPDATE atlas_document SET current_version_id='5c82e042-9280-5d59-bb2b-1cdc8d0d6fbd',searchable_version_id=NULL WHERE id='71d63886-be4c-5a63-a20c-6f463748b8ad';
END IF;
END $seed$;

-- demo-answer Spring Boot 外部化配置示例问答
DO $seed$ BEGIN
IF NOT EXISTS (SELECT 1 FROM atlas_document WHERE source_key='DEMO:stage1:demo-answer') THEN
INSERT INTO atlas_document(id,source_key,source_type,source_name,url,created_at,updated_at) VALUES ('98829c2b-d71b-5ec5-a50b-f8783c753b8e','DEMO:stage1:demo-answer','MANUAL_NOTE','阶段 1 演示回答（演示）',NULL,'2026-10-01 08:00:00+08','2026-10-01 08:00:00+08');
INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version,title,technology,version_range,language,tags,author,license,trust,review_status,index_status,collected_at) VALUES ('f1bf3fde-61ab-519c-9497-039e6ff88bcf','98829c2b-d71b-5ec5-a50b-f8783c753b8e',1,'99384551ca4eded2896cf4364eb9f95650b7438ab75bf69735cefbabd5658075','99384551ca4eded2896cf4364eb9f95650b7438ab75bf69735cefbabd5658075',decode('3e20e6bc94e7a4bae695b0e68daeefbc9ae4bb8ee998b6e6aeb5203120e6a8a1e68b9fe58685e5aeb9e8bdace68da2efbc8ce69caae5ae9ee697b6e68a93e58f96e38081e69caae9aa8ce8af81e58e9fe5bc95e794a8e380820a3e20e58e9fe69da5e6ba90e7b1bbe59e8befbc9a44454d4f5f414e53574552efbc9be58e9fe8afade8a880e5ad97e6aeb5efbc9a7a68efbc9be58e9fe8aeb8e58fafe5ad97e6aeb5efbc9ae69caae79fa5efbc88e59d87e4b8bae6bc94e7a4bae5ad97e6aeb5efbc89e380820a0ae6a0b9e68daee5bd93e5898de79fa5e8af86e5ba93efbc8c537072696e6720426f6f7420332e3520e694afe68c81e9809ae8bf872070726f70657274696573e3808159414d4ce38081e78eafe5a283e58f98e9878fe5928ce591bde4bba4e8a18ce58f82e695b0e68f90e4be9be5a496e983a8e58c96e9858de7bdaee38082e9858de7bdaee69c89e6988ee7a1aee79a84e5b19ee680a7e6ba90e4bc98e58588e7baa7efbc8ce983a8e7bdb2e697b6e58fafe4bba5e4bdbfe794a8e78eafe5a283e58f98e9878fe8a686e79b96e58c85e58685e9bb98e8aea4e580bce38082e5afb9e4ba8ee58c85e590abe5af86e992a5e79a84e9858de7bdaeefbc8ce5ba94e4bb8ee8bf90e8a18ce78eafe5a283e6b3a8e585a5efbc8ce981bfe5858de58699e585a5e6ba90e7a081e380825b315d0a0ae5a682e69e9ce8a681e59ca8e5aeb9e599a8e4b8ade983a8e7bdb2efbc8ce5bbbae8aeaee68a8ae99d9ee6958fe6849fe9bb98e8aea4e580bce4bf9de79599e59ca8206170706c69636174696f6e2e796d6cefbc8ce5b086e695b0e68daee5ba93e5af86e7a081e5928ce6a8a1e59e8be5af86e992a5e9809ae8bf87e78eafe5a283e58f98e9878fe4bca0e585a5e380820a0ae58e9fe6bc94e7a4bae5bc95e794a8efbc88e99d9ee79c9fe5ae9ee6a380e7b4a2e8af81e68daeefbc89efbc9a0a5b0a20207b0a20202020226964223a2022636974652d31222c0a20202020227469746c65223a2022537072696e6720426f6f7420332e35205265666572656e636520446f63756d656e746174696f6e222c0a202020202275726c223a202268747470733a2f2f646f63732e737072696e672e696f2f737072696e672d626f6f742f222c0a202020202273656374696f6e223a202245787465726e616c697a656420436f6e66696775726174696f6e222c0a202020202271756f7465223a2022537072696e6720426f6f74206c65747320796f752065787465726e616c697a6520796f757220636f6e66696775726174696f6e20736f207468617420796f752063616e20776f726b2077697468207468652073616d65206170706c69636174696f6e20636f646520696e20646966666572656e7420656e7669726f6e6d656e74732e222c0a202020202273636f7265223a20302e39340a20207d0a5d','hex'),'> 演示数据：从阶段 1 模拟内容转换，未实时抓取、未验证原引用。
> 原来源类型：DEMO_ANSWER；原语言字段：zh；原许可字段：未知（均为演示字段）。

根据当前知识库，Spring Boot 3.5 支持通过 properties、YAML、环境变量和命令行参数提供外部化配置。配置有明确的属性源优先级，部署时可以使用环境变量覆盖包内默认值。对于包含密钥的配置，应从运行环境注入，避免写入源码。[1]

如果要在容器中部署，建议把非敏感默认值保留在 application.yml，将数据库密码和模型密钥通过环境变量传入。

原演示引用（非真实检索证据）：
[
  {
    "id": "cite-1",
    "title": "Spring Boot 3.5 Reference Documentation",
    "url": "https://docs.spring.io/spring-boot/",
    "section": "Externalized Configuration",
    "quote": "Spring Boot lets you externalize your configuration so that you can work with the same application code in different environments.",
    "score": 0.94
  }
]','stage1-demo-seed-v1','[演示] Spring Boot 外部化配置示例问答','Spring Boot','3.5.x','zh',ARRAY['问答记录','未经验证','演示数据'],'未知','未知','演示数据，未验证','PENDING_REVIEW','NOT_INDEXED','2026-10-01 08:00:00+08');
UPDATE atlas_document SET current_version_id='f1bf3fde-61ab-519c-9497-039e6ff88bcf',searchable_version_id=NULL WHERE id='98829c2b-d71b-5ec5-a50b-f8783c753b8e';
END IF;
END $seed$;
COMMIT;
