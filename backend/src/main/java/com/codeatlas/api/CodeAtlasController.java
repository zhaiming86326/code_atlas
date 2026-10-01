package com.codeatlas.api;

import com.codeatlas.domain.ApiModels.*;
import com.codeatlas.service.DemoDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@org.springframework.context.annotation.Profile("demo")
@RequestMapping("/api")
public class CodeAtlasController {
    private final DemoDataService data;

    public CodeAtlasController(DemoDataService data) {
        this.data = data;
    }

    @GetMapping("/health")
    public Health health() { return data.health(); }

    @GetMapping("/dashboard")
    public Dashboard dashboard() { return data.dashboard(); }

    @GetMapping("/documents")
    public PagedResponse<Document> documents() {
        var items = data.documents();
        return new PagedResponse<>(items, 1, 20, items.size());
    }

    @GetMapping("/documents/{id}")
    public Document document(@PathVariable String id) { return data.document(id); }

    @GetMapping("/collection-policies")
    public List<CollectionPolicy> policies() { return data.policies(); }

    @GetMapping("/collection-tasks")
    public List<CollectionTask> tasks() { return data.tasks(); }

    @GetMapping("/reviews")
    public List<ReviewItem> reviews() { return data.reviews(); }

    @PostMapping("/search/answer")
    public SearchAnswer answer(@Valid @RequestBody SearchPayload request) {
        return data.search(new SearchRequest(request.query(), request.technologies(), request.version(), request.searchMode()));
    }

    public record SearchPayload(
            @NotBlank(message = "问题不能为空") String query,
            List<String> technologies,
            String version,
            String searchMode, String sourceType, String language, String from, String to) {}
}

