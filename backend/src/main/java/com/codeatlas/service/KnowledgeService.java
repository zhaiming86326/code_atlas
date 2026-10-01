package com.codeatlas.service;

import com.codeatlas.domain.ApiModels.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@Profile("postgres")
public class KnowledgeService {
    private final JdbcTemplate db;
    public KnowledgeService(JdbcTemplate db) { this.db = db; }
    public record Metadata(String title, String technology, String version, String language, List<String> tags,
                           String author, String license, String trust) {}
    public record Imported(Document document, boolean duplicate, String versionId) {}
    private static String known(String value) { if (value != null && value.length() > 1000) throw new IllegalArgumentException("元数据字段最多 1000 字符"); return value == null || value.isBlank() ? "未知" : value.strip(); }
    private static final String SELECT = """
        SELECT d.id,d.source_type,d.source_name,d.url,d.archived,d.updated_at,
          v.title,v.technology,v.version_range,v.language,v.tags,v.license,v.content,v.review_status,v.index_status,v.collected_at,
          (SELECT count(*) FROM atlas_chunk c WHERE c.version_id=v.id) chunk_count
        FROM atlas_document d JOIN atlas_document_version v ON v.id=d.current_version_id
        """;
    private Document map(ResultSet r, int row) throws SQLException {
        String status = r.getBoolean("archived") ? "ARCHIVED" : r.getString("review_status");
        if (status.equals("APPROVED")) status = r.getString("index_status").equals("INDEXED") ? "INDEXED" : "APPROVED";
        String text = r.getString("content");
        return new Document(r.getString("id"), r.getString("title"), r.getString("source_type"), r.getString("source_name"),
                r.getString("url"), r.getString("technology"), r.getString("version_range"), r.getString("language"),
                Arrays.asList((String[])r.getArray("tags").getArray()), status, r.getString("license"),
                r.getTimestamp("updated_at").toInstant().toString(), r.getInt("chunk_count"),
                text.substring(0, Math.min(text.length(), 180)), text);
    }
    public Document document(String id) {
        return db.query(SELECT + " WHERE d.id=?", this::map, uuid(id)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "资料不存在"));
    }
    private static UUID uuid(String id) {
        try { return UUID.fromString(id); } catch (RuntimeException e) { throw new IllegalArgumentException("资料 ID 无效"); }
    }
    public PagedResponse<Document> documents(String query, String technology, String status, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("分页范围：page>=1，pageSize=1..100");
        String where = " WHERE (?='' OR v.title ILIKE ? OR v.content ILIKE ? OR array_to_string(v.tags,' ') ILIKE ?) AND (?='' OR v.technology=?)"
                + " AND (?='' OR (?='ARCHIVED' AND d.archived) OR (NOT d.archived AND (?=v.review_status OR (?='INDEXED' AND v.review_status='APPROVED' AND v.index_status='INDEXED'))))";
        Object[] args = {query, pattern(query), pattern(query), pattern(query), technology, technology, status, status, status, status};
        long total = db.queryForObject("SELECT count(*) FROM (" + SELECT + where + ") hits", Long.class, args);
        var paged = new ArrayList<Object>(Arrays.asList(args)); paged.add(pageSize); paged.add((page - 1L) * pageSize);
        var items = db.query(SELECT + where + " ORDER BY d.updated_at DESC LIMIT ? OFFSET ?", this::map, paged.toArray());
        return new PagedResponse<>(items, page, pageSize, total);
    }
    public static String pattern(String query) {
        if (query.length() > 500) throw new IllegalArgumentException("关键词最多 500 字符");
        return "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
    @Transactional
    public Imported ingest(byte[] raw, String name, String type, String url, boolean html, Metadata metadata) {
        if (name.length() > 2048) throw new IllegalArgumentException("来源名称过长");
        var parsed = ContentParser.parse(raw, name, html);
        String key = type + ":" + (url == null ? name : url);
        UUID documentId = UUID.randomUUID();
        db.update("""
            INSERT INTO atlas_document(id,source_key,source_type,source_name,url)
            VALUES (?,?,?,?,?) ON CONFLICT(source_key) DO NOTHING
            """, documentId, key, type, url == null ? name : java.net.URI.create(url).getHost(), url);
        // Row lock serializes imports/reviews of the same document across all API instances.
        documentId = db.queryForObject("SELECT id FROM atlas_document WHERE source_key=? FOR UPDATE", UUID.class, key);
        return storeVersion(documentId, raw, parsed, metadata, ContentParser.VERSION);
    }
    private Imported storeVersion(UUID documentId, byte[] raw, ContentParser.Parsed parsed, Metadata metadata, String parserVersion) {
        String hash = ContentParser.hash(parsed.content().getBytes(StandardCharsets.UTF_8));
        var existing = db.queryForList("SELECT id FROM atlas_document_version WHERE document_id=? AND content_hash=?", documentId, hash);
        if (!existing.isEmpty()) return new Imported(document(documentId.toString()), true, existing.getFirst().get("id").toString());
        UUID versionId = UUID.randomUUID();
        db.update("""
            INSERT INTO atlas_document_version(id,document_id,sequence_no,content_hash,raw_hash,raw_content,content,parser_version)
            VALUES (?,?,(SELECT coalesce(max(sequence_no),0)+1 FROM atlas_document_version WHERE document_id=?),?,?,?,?,?)
            """, versionId, documentId, documentId, hash, ContentParser.hash(raw), raw, parsed.content(), parserVersion);
        db.update("UPDATE atlas_document SET current_version_id=?,updated_at=now() WHERE id=?", versionId, documentId);
        updateMetadata(documentId.toString(), new Metadata(metadata.title() == null || metadata.title().isBlank() ? parsed.title() : metadata.title(),
                metadata.technology(), metadata.version(), metadata.language(), metadata.tags(), metadata.author(), metadata.license(), metadata.trust()));
        return new Imported(document(documentId.toString()), false, versionId.toString());
    }
    @Transactional
    public Document updateMetadata(String id, Metadata m) {
        document(id);
        db.queryForObject("SELECT id FROM atlas_document WHERE id=? FOR UPDATE",UUID.class,uuid(id));
        if (m.tags() != null && (m.tags().size() > 30 || m.tags().stream().anyMatch(t -> t == null || t.length() > 100)))
            throw new IllegalArgumentException("标签最多 30 个，每个最多 100 字符");
        List<String> tags = m.tags() == null ? List.of() : m.tags().stream().map(String::strip).filter(s -> !s.isBlank()).distinct().toList();
        db.update(connection -> {
            var statement = connection.prepareStatement("UPDATE atlas_document_version SET title=?,technology=?,version_range=?,language=?,tags=?,author=?,license=?,trust=? WHERE id=(SELECT current_version_id FROM atlas_document WHERE id=?)");
            Object[] values = {known(m.title()),known(m.technology()),known(m.version()),known(m.language()),connection.createArrayOf("text", tags.toArray()),known(m.author()),known(m.license()),known(m.trust()),uuid(id)};
            for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]);
            return statement;
        });
        db.update("UPDATE atlas_document SET updated_at=now() WHERE id=?",uuid(id));
        return document(id);
    }
    @Transactional
    public Document review(String id, String expectedVersion, boolean approved, String note) {
        document(id);
        UUID docId = uuid(id);
        UUID versionId = db.queryForObject("SELECT current_version_id FROM atlas_document WHERE id=? FOR UPDATE", UUID.class, docId);
        if (!Objects.equals(versionId, uuid(expectedVersion))) throw new ResponseStatusException(HttpStatus.CONFLICT, "资料已更新，请重新加载后审核");
        String decision = approved ? "APPROVED" : "REJECTED";
        String previous = db.queryForObject("SELECT review_status FROM atlas_document_version WHERE id=?", String.class, versionId);
        if (!decision.equals(previous)) {
            db.update("UPDATE atlas_document_version SET review_status=? WHERE id=?", decision, versionId);
            db.update("INSERT INTO atlas_review(id,version_id,decision,note) VALUES (?,?,?,?)", UUID.randomUUID(), versionId, decision, note == null ? "" : note);
        }
        if (approved) index(versionId, docId);
        else {
            db.update("DELETE FROM atlas_chunk WHERE version_id=?", versionId);
            db.update("UPDATE atlas_document_version SET index_status='NOT_INDEXED' WHERE id=?", versionId);
            db.update("UPDATE atlas_document SET searchable_version_id=NULL WHERE id=? AND searchable_version_id=?", docId, versionId);
        }
        return document(id);
    }
    private void index(UUID versionId, UUID docId) {
        String content = db.queryForObject("SELECT content FROM atlas_document_version WHERE id=?", String.class, versionId);
        db.update("DELETE FROM atlas_chunk WHERE version_id=?", versionId);
        var chunks = ContentParser.chunks(content);
        for(int i=0;i<chunks.size();i++) db.update("INSERT INTO atlas_chunk(id,version_id,ordinal,section_path,content,chunker_version) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID(), versionId, i, chunks.get(i).section(), chunks.get(i).content(), "heading-paragraph-v1;target=1600;preserve-code");
        db.update("UPDATE atlas_document_version SET index_status='INDEXED' WHERE id=?", versionId);
        db.update("UPDATE atlas_document SET searchable_version_id=?,updated_at=now() WHERE id=?", versionId, docId);
    }
    @Transactional
    public Document reindex(String id) {
        document(id);
        UUID docId = uuid(id);
        UUID versionId = db.queryForObject("SELECT current_version_id FROM atlas_document WHERE id=? FOR UPDATE", UUID.class, docId);
        if (!"APPROVED".equals(db.queryForObject("SELECT review_status FROM atlas_document_version WHERE id=?", String.class, versionId)))
            throw new IllegalArgumentException("只有已批准版本可以重新索引");
        index(versionId, docId); return document(id);
    }
    public List<Map<String,Object>> versions(String id) {
        document(id);
        return db.queryForList("""
            SELECT id,sequence_no,title,technology,version_range,language,content_hash,raw_hash,parser_version,review_status,index_status,collected_at,published_at,source_updated_at,
              content,(SELECT count(*) FROM atlas_review r WHERE r.version_id=v.id) review_count
            FROM atlas_document_version v WHERE document_id=? ORDER BY sequence_no DESC
            """, uuid(id));
    }
    public Map<String,Object> version(String id, String versionId) {
        document(id);
        var rows = db.query("""
            SELECT id,document_id,sequence_no,title,technology,version_range,language,tags,author,license,trust,
                content_hash,raw_hash,parser_version,review_status,index_status,published_at,source_updated_at,collected_at,content
            FROM atlas_document_version WHERE document_id=? AND id=?
            """,(r,n)-> {
                var row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(r,n);
                row.put("tags",r.getArray("tags").getArray());return row;
            },uuid(id),uuid(versionId));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"版本不存在或不属于该资料");
        return rows.getFirst();
    }
    @Transactional
    public Imported appendVersion(String id, byte[] raw, boolean html, Metadata metadata) throws java.sql.SQLException {
        document(id);
        UUID docId = db.queryForObject("SELECT id FROM atlas_document WHERE id=? FOR UPDATE",UUID.class,uuid(id));
        if (metadata == null) {
            var current = db.queryForMap("SELECT v.title,v.technology,v.version_range,v.language,v.tags,v.author,v.license,v.trust FROM atlas_document_version v JOIN atlas_document d ON v.id=d.current_version_id WHERE d.id=?",docId);
            metadata = new Metadata((String)current.get("title"),(String)current.get("technology"),(String)current.get("version_range"),
                (String)current.get("language"),Arrays.asList((String[])((java.sql.Array)current.get("tags")).getArray()),
                (String)current.get("author"),(String)current.get("license"),(String)current.get("trust"));
        }
        return storeVersion(docId,raw,ContentParser.parse(raw,document(id).title(),html),metadata,ContentParser.VERSION+";manual-revision");
    }
    public PagedResponse<Map<String,Object>> chunks(String id,String versionId,String query,int page,int pageSize) {
        version(id,versionId);
        return records("SELECT id,version_id,ordinal,section_path,content,chunker_version FROM atlas_chunk",
                " WHERE version_id=? AND (?='' OR content ILIKE ?)", " ORDER BY ordinal",List.of(uuid(versionId),query,pattern(query)),page,pageSize);
    }
    public Map<String,Object> chunk(String chunkId) {
        var rows = db.queryForList("""
            SELECT c.*,v.document_id,v.title,v.review_status,v.index_status FROM atlas_chunk c
              JOIN atlas_document_version v ON v.id=c.version_id WHERE c.id=?
            """,uuid(chunkId));
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"分块不存在");
        return rows.getFirst();
    }
    public PagedResponse<Map<String,Object>> reviewRecords(String documentId,String versionId,String decision,int page,int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if(documentId != null && !documentId.isBlank()) { document(documentId); where.append(" AND v.document_id=?"); args.add(uuid(documentId)); }
        if(versionId != null && !versionId.isBlank()) { where.append(" AND r.version_id=?"); args.add(uuid(versionId)); }
        if(decision != null && !decision.isBlank()) {
            if(!List.of("APPROVED","REJECTED").contains(decision)) throw new IllegalArgumentException("审核决定必须为 APPROVED 或 REJECTED");
            where.append(" AND r.decision=?"); args.add(decision);
        }
        return records("SELECT r.*,v.document_id,v.sequence_no,v.title FROM atlas_review r JOIN atlas_document_version v ON v.id=r.version_id",
                where.toString()," ORDER BY r.reviewed_at DESC,r.id",args,page,pageSize);
    }
    public Map<String,Object> reviewRecord(String reviewId) {
        var rows=db.queryForList("SELECT r.*,v.document_id,v.sequence_no,v.title FROM atlas_review r JOIN atlas_document_version v ON v.id=r.version_id WHERE r.id=?",uuid(reviewId));
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"审核记录不存在");
        return rows.getFirst();
    }
    private PagedResponse<Map<String,Object>> records(String select,String where,String order,List<Object> args,int page,int pageSize) {
        if(page<1 || pageSize<1 || pageSize>100) throw new IllegalArgumentException("分页范围：page>=1，pageSize=1..100");
        long total=db.queryForObject("SELECT count(*) FROM ("+select+where+") records",Long.class,args.toArray());
        var paged=new ArrayList<>(args);paged.add(pageSize);paged.add((page-1L)*pageSize);
        return new PagedResponse<>(db.queryForList(select+where+order+" LIMIT ? OFFSET ?",paged.toArray()),page,pageSize,total);
    }
    public Map<String,Object> metadata(String id) {
        document(id);
        return db.queryForMap("SELECT v.author,v.trust,d.created_at,d.current_version_id,d.searchable_version_id FROM atlas_document d JOIN atlas_document_version v ON v.id=d.current_version_id WHERE d.id=?",uuid(id));
    }
    public byte[] raw(String id, String versionId) {
        document(id);
        var rows = db.query("SELECT raw_content FROM atlas_document_version WHERE document_id=? AND id=?", (r,n) -> r.getBytes(1), uuid(id),uuid(versionId));
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"快照不存在");
        return rows.getFirst();
    }
    public List<ReviewItem> reviews() {
        return db.query(SELECT + " WHERE v.review_status='PENDING_REVIEW' AND NOT d.archived ORDER BY v.collected_at DESC", (r,n) ->
                new ReviewItem(r.getString("id"),r.getString("title"),r.getString("source_name"),r.getString("technology"),r.getString("version_range"),
                        0,r.getTimestamp("collected_at").toInstant().toString(),"手动导入；未使用 AI 相关性评分，请检查正文与元数据",List.of("待审核新版本")));
    }
    public SearchAnswer search(SearchRequest request) {
        return search(request, "", "", "", "");
    }
    public SearchAnswer search(SearchRequest request, String source, String language, String from, String to) {
        if (request.searchMode() != null && !request.searchMode().equals("KEYWORD")) throw new IllegalArgumentException("阶段 2 仅支持 KEYWORD 检索");
        if (request.technologies() != null && request.technologies().size() > 20) throw new IllegalArgumentException("最多筛选 20 种技术");
        StringBuilder sql = new StringBuilder("""
            SELECT d.id,d.source_type,d.source_name,d.url,d.archived,d.updated_at,
              v.id AS version_id,v.title,v.technology,v.version_range,v.language,v.tags,v.license,v.content,v.review_status,v.index_status,v.collected_at,
              (SELECT count(*) FROM atlas_chunk c WHERE c.version_id=v.id) chunk_count
            FROM atlas_document d JOIN atlas_document_version v ON v.id=d.searchable_version_id
            WHERE NOT d.archived AND v.review_status='APPROVED' AND v.index_status='INDEXED'
              AND EXISTS (SELECT 1 FROM atlas_chunk c WHERE c.version_id=v.id AND c.content ILIKE ?)
            """);
        var args = new ArrayList<Object>(); args.add(pattern(request.query().strip()));
        if (request.technologies() != null && !request.technologies().isEmpty()) {
            sql.append(" AND v.technology IN (" + String.join(",",Collections.nCopies(request.technologies().size(),"?")) + ")"); args.addAll(request.technologies());
        }
        if(request.version() != null && !request.version().isBlank()) { sql.append(" AND v.version_range=?"); args.add(request.version()); }
        if (source != null && !source.isBlank()) { sql.append(" AND d.source_type=?"); args.add(source); }
        if (language != null && !language.isBlank()) { sql.append(" AND v.language=?"); args.add(language); }
        if (from != null && !from.isBlank()) { sql.append(" AND v.collected_at>=?"); args.add(date(from)); }
        if (to != null && !to.isBlank()) { sql.append(" AND v.collected_at<?"); args.add(date(to).plusDays(1)); }
        sql.append(" ORDER BY d.updated_at DESC LIMIT 20");
        var resultVersions = new LinkedHashMap<String,String>();
        var documents = db.query(sql.toString(), (r,n) -> {
            resultVersions.put(r.getString("id"),r.getString("version_id")); return map(r,n);
        }, args.toArray());
        return new SearchAnswer(documents.isEmpty() ? "未找到符合范围的已批准资料。可尝试更短的关键词或手动导入资料。" : "关键词检索结果如下。阶段 2 未调用 LLM，不生成问答或选型结论。", List.of(), documents, false,resultVersions);
    }
    private static java.time.OffsetDateTime date(String text) {
        try { return java.time.LocalDate.parse(text).atStartOfDay().atOffset(java.time.ZoneOffset.UTC); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("日期必须为 YYYY-MM-DD（UTC）"); }
    }
    public Dashboard dashboard() {
        var activities=db.query("""
            SELECT id,type,title,detail,time,status FROM (
              SELECT v.id::text id,'import' type,v.title,'版本 '||v.sequence_no||' 导入' detail,v.collected_at time,
                CASE WHEN v.review_status='PENDING_REVIEW' THEN 'warning' ELSE 'success' END status FROM atlas_document_version v
              UNION ALL
              SELECT r.id::text,'review',v.title,r.decision||'：'||r.note,r.reviewed_at,
                CASE WHEN r.decision='REJECTED' THEN 'danger' ELSE 'success' END
                FROM atlas_review r JOIN atlas_document_version v ON v.id=r.version_id
            ) events ORDER BY time DESC LIMIT 10
            """,(r,n)->new Activity(r.getString("id"),r.getString("type"),r.getString("title"),r.getString("detail"),r.getTimestamp("time").toInstant().toString(),r.getString("status")));
        var trend=db.query("""
            SELECT day::date::text date,count(v.id) discovered,count(v.id) FILTER(WHERE v.index_status='INDEXED') indexed
            FROM generate_series((now() AT TIME ZONE 'UTC')::date-6,(now() AT TIME ZONE 'UTC')::date,interval '1 day') day
            LEFT JOIN atlas_document_version v ON (v.collected_at AT TIME ZONE 'UTC')::date=day::date
            GROUP BY day ORDER BY day
            """,(r,n)->new TrendPoint(r.getString("date"),r.getInt("discovered"),r.getInt("indexed")));
        return new Dashboard(count("atlas_document"),count("atlas_chunk"),reviews().size(),0,0,null,activities,trend);
    }
    private int count(String table) { return db.queryForObject("SELECT count(*) FROM " + table,Integer.class); }
    @Transactional
    public void archive(String id, boolean archived) { document(id); db.update("UPDATE atlas_document SET archived=?,updated_at=now() WHERE id=?",archived,uuid(id)); }
    @Transactional
    public void delete(String id) { document(id); db.update("DELETE FROM atlas_document WHERE id=?",uuid(id)); }
}
