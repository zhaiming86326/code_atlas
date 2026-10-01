package com.codeatlas.api;
import com.codeatlas.service.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @Profile("postgres") @RequestMapping("/api")
public class RagController {
    private final RagService rag; private final ModelService models;
    public RagController(RagService rag,ModelService models){this.rag=rag;this.models=models;}
    @GetMapping("/models") public Object models(){return rag.state();}
    @PutMapping("/models") public Object save(@RequestBody List<ModelService.Settings> settings){models.save(settings);return rag.state();}
    @PostMapping("/rag/retrieve") public Object retrieve(@RequestBody RagService.Query query){return rag.retrieve(query);}
    @PostMapping("/rag/answer") public Object answer(@RequestBody RagService.Query query) throws java.io.IOException{return rag.answer(query);}
    @GetMapping("/rag/history") public Object history(){return rag.history();}
    @GetMapping("/documents/{id}/embedding-index") public Object indexes(@PathVariable UUID id){return Map.of("vectorReady",rag.vectorReady(),"items",rag.indexes(id));}
    @PostMapping("/documents/{id}/embedding-index") public Object index(@PathVariable UUID id){return rag.index(id);}
}
