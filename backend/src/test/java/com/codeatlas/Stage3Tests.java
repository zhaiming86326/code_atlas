package com.codeatlas;
import com.codeatlas.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class Stage3Tests {
 @Test void localEmbeddingConfigurationWithoutTestFlag(){
  var service=new ModelService(mock(JdbcTemplate.class),new MockEnvironment());
  var llm=new ModelService.Settings("LLM","https://api.deepseek.com","model",1024,"1",null,null,"未知",true);
  for(String url:List.of("http://127.0.0.1:11434/v1","http://localhost:11434/v1"))
   assertDoesNotThrow(()->service.save(List.of(llm,new ModelService.Settings("EMBEDDING",url,"qwen3-embedding:0.6b",1024,"1",null,null,"未知",true))));
  for(String url:List.of("http://example.com/v1","http://192.168.1.5:11434/v1","http://127.0.0.1.evil.test/v1","http://user:password@127.0.0.1:11434/v1","http://127.0.0.1:11434/v1?key=value","http://localhost:11434/v1#fragment"))
   assertThrows(IllegalArgumentException.class,()->service.save(List.of(llm,new ModelService.Settings("EMBEDDING",url,"model",1024,"1",null,null,"未知",true))));
  assertThrows(IllegalArgumentException.class,()->service.save(List.of(new ModelService.Settings("LLM","http://127.0.0.1:11434/v1","model",1024,"1",null,null,"未知",true),new ModelService.Settings("EMBEDDING","http://127.0.0.1:11434/v1","model",1024,"1",null,null,"未知",true))));
 }
 @Test void retrievalAndCitationBoundaries(){
  assertTrue(RagService.terms("Java 虚拟线程适合什么场景？").contains("线程"));assertTrue(RagService.terms("Class.forName API").contains("Class.forName"));
  var h=new RagService.Hit("c","d","v","标题",null,"Java","21","章节","虚拟线程适用于等待 I/O 的任务。",1);
  assertEquals(1,RagService.fuse(List.of(h),List.of(h)).size());
  var json=new ObjectMapper();String output="{\"supported\":true,\"answer\":\"适用于等待 I/O 的任务。[1]\",\"citations\":[{\"number\":1,\"quote\":\"虚拟线程适用于等待 I/O 的任务。\"}]}";
  assertEquals("ANSWERED",RagService.validate(output,List.of(h),false,"KEYWORD",json).status());
  String serverQuote="{\"supported\":true,\"answer\":\"适用于等待任务。[1]\",\"citations\":[{\"number\":1}]}";
  assertEquals(h.content(),RagService.validate(serverQuote,List.of(h),false,"KEYWORD",json).citations().getFirst().quote());
  assertEquals("ANSWERED",RagService.validate("```json\n"+serverQuote+"\n```",List.of(h),false,"KEYWORD",json).status());
  assertEquals("ANSWERED",RagService.validate(serverQuote.replace("适用于等待任务。", "代码示例 `array[0]`。"),List.of(h),false,"KEYWORD",json).status());
  assertThrows(RuntimeException.class,()->RagService.validate(serverQuote+" {}",List.of(h),false,"KEYWORD",json));
  assertThrows(RuntimeException.class,()->RagService.validate(serverQuote.replace("[1]","[9]"),List.of(h),false,"KEYWORD",json));
  assertThrows(RuntimeException.class,()->RagService.validate(output.replace("[1]","[2]"),List.of(h),false,"KEYWORD",json));
  assertThrows(RuntimeException.class,()->RagService.validate(output.replace("虚拟线程适用于","模型编造的内容"),List.of(h),false,"KEYWORD",json));
  assertThrows(IllegalArgumentException.class,()->ModelService.validateVector(new float[]{0,0},2));
  assertThrows(IllegalArgumentException.class,()->ModelService.validateVector(new float[]{Float.NaN,1},2));
  assertThrows(IllegalArgumentException.class,()->ModelService.validateVector(new float[]{1,2},3));
 }
 @Test void actualSpringAiCompatibleHttp() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/v1/embeddings",exchange->{
   String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
   if(request.contains("missing-model")){byte[] error="private-upstream-body".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(404,error.length);exchange.getResponseBody().write(error);exchange.close();return;}
   String body="{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"embedding\":[1,2],\"index\":0}],\"model\":\"test-only\",\"usage\":{\"prompt_tokens\":4,\"total_tokens\":4}}";byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});
  server.createContext("/v1/chat/completions",exchange->{String body="{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"test-only\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"测试回复\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":4,\"completion_tokens\":2,\"total_tokens\":6}}";byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});
  server.start();try{
   var db=mock(JdbcTemplate.class);var env=new MockEnvironment().withProperty("LLM_API_KEY","test-key").withProperty("EMBEDDING_API_KEY","test-key");var service=new ModelService(db,env);
   String url="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";
   var embedding=new ModelService.Settings("EMBEDDING",url,"test-only",2,"1",null,null,"未知",true);
   assertArrayEquals(new float[]{1,2},service.embed(embedding,"测试输入"));
   var missing=new ModelService.Settings("EMBEDDING",url,"missing-model",2,"1",null,null,"未知",true);
   var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.embed(missing,"测试输入"));
   assertTrue(error.getReason().contains("HTTP 404"));assertFalse(error.getReason().contains("private-upstream-body"));
   assertEquals("Embedding 维度不匹配",ModelService.failureReason(new IllegalArgumentException("Embedding 维度不匹配")));
   assertTrue(ModelService.failureReason(new RuntimeException("private",new java.net.ConnectException())).contains("无法连接"));
   env.withProperty("code-atlas.models.allow-local-test-provider","true");
   var llm=new ModelService.Settings("LLM",url,"test-only",2,"1",BigDecimal.ONE,BigDecimal.ONE,"测试价格",true);
   assertEquals("测试回复",service.chat(llm,"系统指令","问题"));
   assertNotEquals(service.profile(embedding),service.profile(new ModelService.Settings("EMBEDDING",url,"other-model",2,"1",null,null,"未知",true)));
   verify(db,atLeast(4)).update(anyString(),any(Object[].class));
  }finally{server.stop(0);}
 }
}
