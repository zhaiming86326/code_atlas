package com.codeatlas.service;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

@Service
@Profile("postgres")
public class ModelService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ModelService.class);
    private static final class ProviderHttpException extends RuntimeException {
        private final int status;
        ProviderHttpException(int status) { super("模型 HTTP 请求失败");this.status=status; }
    }
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public record Settings(String role,String baseUrl,String model,int dimensions,String revision,
            BigDecimal inputPricePerMillion,BigDecimal outputPricePerMillion,String priceBasis,boolean keyConfigured) {}
    private final JdbcTemplate db;
    private final Environment env;
    public ModelService(JdbcTemplate db,Environment env) { this.db=db;this.env=env; }
    private String property(String role,String name,String variable,String fallback) {
        return env.getProperty("code-atlas.models."+role.toLowerCase(Locale.ROOT)+"."+name,env.getProperty(variable,fallback));
    }
    private String key(String role) { return property(role,"api-key",role.equals("LLM")?"LLM_API_KEY":"EMBEDDING_API_KEY",""); }
    private BigDecimal price(String value) { return value==null||value.isBlank()?null:new BigDecimal(value); }
    public Settings settings(String role) {
        if(!List.of("LLM","EMBEDDING").contains(role)) throw new IllegalArgumentException("模型角色无效");
        var saved=db.queryForList("SELECT * FROM atlas_model_config WHERE role=?",role);
        if(!saved.isEmpty()) {
            var r=saved.getFirst();
            return new Settings(role,(String)r.get("base_url"),(String)r.get("model"),(Integer)r.get("dimensions"),(String)r.get("revision"),
                    (BigDecimal)r.get("input_price_per_million"),(BigDecimal)r.get("output_price_per_million"),(String)r.get("price_basis"),!key(role).isBlank());
        }
        String prefix=role.equals("LLM")?"LLM":"EMBEDDING";
        return new Settings(role,property(role,"base-url",prefix+"_BASE_URL","https://api.openai.com/v1"),property(role,"model",prefix+"_MODEL",""),
                Integer.parseInt(property(role,"dimensions",prefix+"_DIMENSIONS","1536")),property(role,"revision",prefix+"_REVISION","1"),
                price(property(role,"input-price-per-million",prefix+"_INPUT_PRICE","")),price(property(role,"output-price-per-million",prefix+"_OUTPUT_PRICE","")),
                property(role,"price-basis",prefix+"_PRICE_BASIS","未知，USD"),!key(role).isBlank());
    }
    public List<Settings> settings() { return List.of(settings("LLM"),settings("EMBEDDING")); }
    @org.springframework.transaction.annotation.Transactional
    public void save(List<Settings> settings) {
        if(settings==null || settings.stream().anyMatch(Objects::isNull) || settings.size()!=2 || !settings.stream().map(Settings::role).collect(java.util.stream.Collectors.toSet()).equals(Set.of("LLM","EMBEDDING")))
            throw new IllegalArgumentException("必须同时提供 LLM 与 EMBEDDING 配置");
        for(var s:settings) {
            validateEndpoint(s.baseUrl(),s.role());
            if(s.model()==null || s.model().length()>200 || s.revision()==null || s.revision().isBlank() || s.revision().length()>100 || s.dimensions()<1 || s.dimensions()>16000)
                throw new IllegalArgumentException("模型名称、修订标识或维度无效");
            if((s.inputPricePerMillion()!=null && s.inputPricePerMillion().signum()<0) || (s.outputPricePerMillion()!=null && s.outputPricePerMillion().signum()<0)
                    || s.priceBasis()==null || s.priceBasis().length()>1000) throw new IllegalArgumentException("价格或价格依据无效");
            db.update("""
                INSERT INTO atlas_model_config(role,base_url,model,dimensions,revision,input_price_per_million,output_price_per_million,price_basis)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(role) DO UPDATE SET base_url=excluded.base_url,model=excluded.model,
                dimensions=excluded.dimensions,revision=excluded.revision,input_price_per_million=excluded.input_price_per_million,
                output_price_per_million=excluded.output_price_per_million,price_basis=excluded.price_basis,updated_at=now()
                """,s.role(),s.baseUrl().replaceAll("/+$",""),s.model().strip(),s.dimensions(),s.revision(),s.inputPricePerMillion(),s.outputPricePerMillion(),s.priceBasis());
        }
    }
    private void validateEndpoint(String value,String role) {
        URI u;
        try { u=URI.create(value); } catch(RuntimeException e) { throw new IllegalArgumentException("模型 Base URL 无效"); }
        boolean local="http".equals(u.getScheme()) && u.getHost()!=null
                && Set.of("127.0.0.1","localhost").contains(u.getHost().toLowerCase(Locale.ROOT))
                && ("EMBEDDING".equals(role) || env.getProperty("code-atlas.models.allow-local-test-provider",Boolean.class,false));
        if(value.length()>2048 || u.getHost()==null || u.getUserInfo()!=null || u.getQuery()!=null || u.getFragment()!=null
                || u.getPort()==0 || u.getPort()>65535 || !("https".equals(u.getScheme()) || local))
            throw new IllegalArgumentException("模型地址必须为 HTTPS；本机 Embedding 可使用 http://127.0.0.1 或 http://localhost，不允许凭证、查询参数或片段");
    }
    private OpenAiApi api(Settings s) {
        validateEndpoint(s.baseUrl(),s.role());
        if(s.model().isBlank() || key(s.role()).isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"请在后端配置 "+s.role()+" 模型名与密钥");
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory=new JdkClientHttpRequestFactory(client);factory.setReadTimeout(Duration.ofSeconds(45));
        return OpenAiApi.builder().baseUrl(s.baseUrl()).apiKey(key(s.role())).completionsPath("/chat/completions").embeddingsPath("/embeddings")
                .restClientBuilder(RestClient.builder().requestFactory(factory))
                .responseErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
                    public boolean hasError(org.springframework.http.client.ClientHttpResponse r) throws java.io.IOException { return r.getStatusCode().isError(); }
                    public void handleError(java.net.URI uri,org.springframework.http.HttpMethod method,org.springframework.http.client.ClientHttpResponse r) throws java.io.IOException { throw new ProviderHttpException(r.getStatusCode().value()); }
                }).build();
    }
    private UUID begin(Settings s) {
        UUID id=UUID.randomUUID();db.update("INSERT INTO atlas_model_call(id,role,model,status,price_basis) VALUES (?,?,?,'STARTED',?)",id,s.role(),s.model(),s.priceBasis());
        log.info("模型调用开始 callId={} role={} model={}",id,s.role(),s.model());return id;
    }
    private void finish(UUID id,Settings s,Integer input,Integer output) {
        BigDecimal cost=null;
        if(input!=null && s.inputPricePerMillion()!=null && (!s.role().equals("LLM") || (output!=null && s.outputPricePerMillion()!=null))) {
            cost=s.inputPricePerMillion().multiply(BigDecimal.valueOf(input));
            if(s.role().equals("LLM")) cost=cost.add(s.outputPricePerMillion().multiply(BigDecimal.valueOf(output)));
            cost=cost.divide(BigDecimal.valueOf(1_000_000));
        }
        db.update("UPDATE atlas_model_call SET status='SUCCESS',input_tokens=?,output_tokens=?,estimated_cost=?,completed_at=now() WHERE id=?",input,output,cost,id);
        log.info("模型调用成功 callId={} role={} inputTokens={} outputTokens={} estimatedCost={}",id,s.role(),input,output,cost);
    }
    public static String failureReason(Throwable error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
            if(cause instanceof ProviderHttpException http) return "模型服务返回 HTTP "+http.status+switch(http.status) {
                case 404 -> "：模型未安装、模型名错误或接口路径不存在；本机 Ollama 请先 ollama pull 对应模型";
                case 401,403 -> "：密钥无效或访问权限不足";
                case 429 -> "：限流或额度不足";
                default -> "：请检查模型服务状态";
            };
            if(cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) return "模型请求超时：本机模型首次加载可能较慢，请检查模型服务";
            if(cause instanceof java.net.ConnectException) return "无法连接模型服务，请检查 Ollama 是否启动及 Base URL";
            if(cause instanceof java.net.UnknownHostException) return "模型服务域名解析失败，请检查 DNS";
            if(cause instanceof IllegalArgumentException && Set.of("Embedding 维度不匹配","向量包含非有限值","不能使用零向量","模型回答达到输出上限，JSON 可能被截断","模型未返回最终回答正文").contains(Objects.toString(cause.getMessage(),""))) return cause.getMessage();
        }
        return "模型请求失败（"+error.getClass().getSimpleName()+"），请检查模型服务与响应格式";
    }
    private ResponseStatusException failed(UUID id,RuntimeException error) {
        // Never store upstream response bodies: they can contain credentials, prompts or private source text.
        String reason=failureReason(error);
        db.update("UPDATE atlas_model_call SET status='FAILED',error=?,completed_at=now() WHERE id=?",reason,id);
        log.warn("模型调用失败 callId={} reason={}",id,reason);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,reason);
    }
    public float[] embed(Settings s,String text) {
        var api=api(s);UUID call=begin(s);
        try {
            var options=OpenAiEmbeddingOptions.builder().model(s.model()).build();
            var model=new OpenAiEmbeddingModel(api,MetadataMode.EMBED,options,RetryTemplate.builder().maxAttempts(1).build());
            var response=model.call(new EmbeddingRequest(List.of(text),options));
            float[] vector=response.getResult().getOutput();validateVector(vector,s.dimensions());
            var usage=response.getMetadata().getUsage();finish(call,s,usage==null || usage instanceof org.springframework.ai.chat.metadata.EmptyUsage?null:usage.getPromptTokens(),0);return vector;
        } catch(RuntimeException e) { throw failed(call,e); }
    }
    public String chat(Settings s,String system,String user) {
        var api=api(s);UUID call=begin(s);
        try {
            var options=OpenAiChatOptions.builder().model(s.model()).maxTokens(3000);
            if("api.deepseek.com".equalsIgnoreCase(URI.create(s.baseUrl()).getHost())) {
                options.extraBody(Map.of("thinking",Map.of("type","disabled")));
                options.responseFormat(new org.springframework.ai.openai.api.ResponseFormat(org.springframework.ai.openai.api.ResponseFormat.Type.JSON_OBJECT,null));
            }
            var model=OpenAiChatModel.builder().openAiApi(api).defaultOptions(options.build())
                    .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
            var response=model.call(new Prompt(List.of(new SystemMessage(system),new UserMessage(user))));
            String text=response.getResult().getOutput().getText();
            String finish=response.getResult().getMetadata().getFinishReason();
            log.info("模型回答返回 callId={} finishReason={} answerCharacters={}",call,finish,text==null?0:text.length());
            if("LENGTH".equalsIgnoreCase(finish)) throw new IllegalArgumentException("模型回答达到输出上限，JSON 可能被截断");
            if(text==null || text.isBlank()) throw new IllegalArgumentException("模型未返回最终回答正文");
            var usage=response.getMetadata().getUsage();finish(call,s,usage==null || usage instanceof org.springframework.ai.chat.metadata.EmptyUsage?null:usage.getPromptTokens(),usage==null || usage instanceof org.springframework.ai.chat.metadata.EmptyUsage?null:usage.getCompletionTokens());
            return text;
        } catch(RuntimeException e) { throw failed(call,e); }
    }
    public static void validateVector(float[] values,int dimensions) {
        if(values==null || values.length!=dimensions) throw new IllegalArgumentException("Embedding 维度不匹配");
        double norm=0;for(float v:values) { if(!Float.isFinite(v)) throw new IllegalArgumentException("向量包含非有限值");norm+=(double)v*v; }
        if(norm==0) throw new IllegalArgumentException("不能使用零向量");
    }
    public static String vectorLiteral(float[] values) {
        return Arrays.toString(values);
    }
    public String profile(Settings s) {
        return ContentParser.hash((s.baseUrl()+"\n"+s.model()+"\n"+s.dimensions()+"\n"+s.revision()+"\nembedding-context-v1").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public Map<String,Object> calls() {
        var summary=db.queryForMap("""
            SELECT count(*) calls,count(*) FILTER(WHERE status='FAILED') failed,
              count(*) FILTER(WHERE estimated_cost IS NULL) unknown_cost_calls,sum(estimated_cost) known_cost,
              coalesce(sum(input_tokens),0) input_tokens,coalesce(sum(output_tokens),0) output_tokens
            FROM atlas_model_call WHERE created_at>=date_trunc('month',now())
            """);
        summary.put("items",db.queryForList("SELECT * FROM atlas_model_call ORDER BY created_at DESC LIMIT 50"));return summary;
    }
}
