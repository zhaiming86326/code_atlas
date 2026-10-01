package com.codeatlas.api;

import com.codeatlas.domain.ApiModels.*;
import com.codeatlas.service.*;
import com.codeatlas.service.KnowledgeService.Metadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.*;
import java.io.IOException;

@RestController
@Profile("postgres")
@RequestMapping("/api")
public class KnowledgeController {
    private final KnowledgeService knowledge;
    private final SafeWebFetcher fetcher;
    public KnowledgeController(KnowledgeService knowledge, SafeWebFetcher fetcher) { this.knowledge=knowledge;this.fetcher=fetcher; }
    @GetMapping("/health") public Health health() { return new Health("ok","code-atlas","0.3.0",false,Instant.now()); }
    @GetMapping("/dashboard") public Dashboard dashboard() { return knowledge.dashboard(); }
    @GetMapping("/documents") public PagedResponse<Document> documents(@RequestParam(defaultValue="") String query,
            @RequestParam(defaultValue="") String technology,@RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize) {
        return knowledge.documents(query,technology,status,page,pageSize);
    }
    @GetMapping("/documents/{id}") public Document document(@PathVariable String id) { return knowledge.document(id); }
    @GetMapping("/documents/{id}/versions") public List<Map<String,Object>> versions(@PathVariable String id) { return knowledge.versions(id); }
    @GetMapping("/documents/{id}/versions/{versionId}") public Map<String,Object> version(@PathVariable String id,@PathVariable String versionId) { return knowledge.version(id,versionId); }
    public record VersionPayload(@NotBlank String content,String format,Metadata metadata) {}
    @PostMapping("/documents/{id}/versions") public KnowledgeService.Imported appendVersion(@PathVariable String id,@Valid @RequestBody VersionPayload v) throws java.sql.SQLException {
        String format=Objects.requireNonNullElse(v.format(),"MARKDOWN").toUpperCase(Locale.ROOT);
        if(!List.of("MARKDOWN","TXT","HTML").contains(format)) throw new IllegalArgumentException("format 必须为 MARKDOWN、TXT 或 HTML");
        return knowledge.appendVersion(id,v.content().getBytes(java.nio.charset.StandardCharsets.UTF_8),format.equals("HTML"),v.metadata());
    }
    @GetMapping("/documents/{id}/versions/{versionId}/chunks") public PagedResponse<Map<String,Object>> chunks(@PathVariable String id,@PathVariable String versionId,
            @RequestParam(defaultValue="") String query,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize) {
        return knowledge.chunks(id,versionId,query,page,pageSize);
    }
    @GetMapping("/chunks/{id}") public Map<String,Object> chunk(@PathVariable String id) { return knowledge.chunk(id); }
    @GetMapping("/review-records") public PagedResponse<Map<String,Object>> reviewRecords(@RequestParam(required=false) String documentId,
            @RequestParam(required=false) String versionId,@RequestParam(defaultValue="") String decision,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize) {
        return knowledge.reviewRecords(documentId,versionId,decision,page,pageSize);
    }
    @GetMapping("/review-records/{id}") public Map<String,Object> reviewRecord(@PathVariable String id) { return knowledge.reviewRecord(id); }
    @GetMapping("/documents/{id}/metadata") public Map<String,Object> metadata(@PathVariable String id) { return knowledge.metadata(id); }
    @GetMapping("/documents/{id}/versions/{versionId}/raw") public ResponseEntity<byte[]> raw(@PathVariable String id,@PathVariable String versionId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition","attachment; filename=snapshot.bin").body(knowledge.raw(id,versionId));
    }
    public record UrlImport(@NotBlank String url, Metadata metadata) {}
    public record NoteImport(@NotBlank String title,@NotBlank String content,Metadata metadata) {}
    private static Metadata defaults(Metadata m) { return m == null ? new Metadata(null,null,null,null,List.of(),null,null,null) : m; }
    @PostMapping("/imports/url") public KnowledgeService.Imported url(@Valid @RequestBody UrlImport request) throws IOException {
        var result = fetcher.fetch(request.url());
        return knowledge.ingest(result.raw(),result.url(),"WEB_PAGE",result.url(),result.html(),defaults(request.metadata()));
    }
    @PostMapping(value="/imports/file", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeService.Imported file(@RequestPart MultipartFile file,@RequestPart(required=false) Metadata metadata) throws IOException {
        String name=Objects.requireNonNullElse(file.getOriginalFilename(),"document.txt").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1);
        if(!name.toLowerCase(Locale.ROOT).matches(".*\\.(md|markdown|txt|html|htm)$")) throw new IllegalArgumentException("仅支持 Markdown、TXT、HTML 文件");
        return knowledge.ingest(file.getBytes(),name,"FILE",null,name.toLowerCase(Locale.ROOT).matches(".*\\.html?$"),defaults(metadata));
    }
    @PostMapping("/imports/note") public KnowledgeService.Imported note(@Valid @RequestBody NoteImport request) {
        return knowledge.ingest(request.content().getBytes(java.nio.charset.StandardCharsets.UTF_8),request.title(),"MANUAL_NOTE",null,false,defaults(request.metadata()));
    }
    @PutMapping("/documents/{id}") public Document edit(@PathVariable String id,@RequestBody Metadata m) { return knowledge.updateMetadata(id,m); }
    public record Decision(@NotBlank String versionId,boolean approved,String note) {}
    @PostMapping("/documents/{id}/review") public Document review(@PathVariable String id,@Valid @RequestBody Decision d) { return knowledge.review(id,d.versionId(),d.approved(),d.note()); }
    @PostMapping("/documents/{id}/reindex") public Document reindex(@PathVariable String id) { return knowledge.reindex(id); }
    @PostMapping("/documents/{id}/archive") public void archive(@PathVariable String id,@RequestParam(defaultValue="true") boolean archived) { knowledge.archive(id,archived); }
    @DeleteMapping("/documents/{id}") public void delete(@PathVariable String id) { knowledge.delete(id); }
    @GetMapping("/reviews") public List<ReviewItem> reviews() { return knowledge.reviews(); }
    @PostMapping("/search/answer") public SearchAnswer search(@Valid @RequestBody CodeAtlasController.SearchPayload r) { return knowledge.search(new SearchRequest(r.query(),r.technologies(),r.version(),r.searchMode()),r.sourceType(),r.language(),r.from(),r.to()); }
    @GetMapping("/collection-policies") public List<CollectionPolicy> policies() { return List.of(); }
    @GetMapping("/collection-tasks") public List<CollectionTask> tasks() { return List.of(); }
}
