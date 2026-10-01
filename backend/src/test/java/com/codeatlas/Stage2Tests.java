package com.codeatlas;

import com.codeatlas.service.ContentParser;
import com.codeatlas.service.SafeWebFetcher;
import com.codeatlas.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class Stage2Tests {
    @Test void networkErrorsExplainBackendConnectivity() {
        var wrapped = new java.io.IOException("request failed", new java.net.NoRouteToHostException("No route to host: getsockopt"));
        assertThat(com.codeatlas.api.ApiExceptionHandler.networkMessage(wrapped)).contains("后端无法连接", "浏览器代理");
        assertThat(com.codeatlas.api.ApiExceptionHandler.networkMessage(new java.net.UnknownHostException("禁止访问内网或保留地址"))).contains("已阻止访问");
        assertThat(com.codeatlas.api.ApiExceptionHandler.networkMessage(new java.net.SocketTimeoutException())).contains("超时");
        assertThat(com.codeatlas.api.ApiExceptionHandler.networkMessage(new java.io.IOException("来源返回 HTTP 403"))).contains("HTTP 403");
    }
    @Test void parserPreservesCodeAndRemovesNavigation() {
        var p = ContentParser.parse("<title>真实资料</title><nav>导航广告</nav><main><h1>配置</h1><p>中文检索</p><pre>if (x) {\n  run();\n}</pre><script>alert(1)</script></main>".getBytes(StandardCharsets.UTF_8), "page", true);
        assertThat(p.title()).isEqualTo("真实资料");
        assertThat(p.content()).contains("中文检索", "  run();").doesNotContain("导航广告", "alert(1)");
        assertThat(ContentParser.chunks("# Java\n## 线程\n```java\n# not a heading\n  run();\n```\n")).hasSize(2);
        assertThat(ContentParser.chunks("# Java\n## 线程\n```java\n# not a heading\n  run();\n```\n").getLast().content()).contains("  run();");
        assertThatThrownBy(() -> ContentParser.parse(new byte[]{(byte)0xff},"bad.txt",false)).isInstanceOf(IllegalArgumentException.class);
        assertThat(KnowledgeService.pattern("A_%\\")).isEqualTo("%A\\_\\%\\\\%");
    }
    @Test void fetchRejectsPrivateNetworksAndUnsafeUrls() throws Exception {
        for (String ip : new String[]{"127.0.0.1","10.0.0.1","172.16.0.1","192.168.1.1","169.254.169.254","100.64.0.1","::1","fc00::1","fe80::1","::ffff:127.0.0.1"})
            assertThat(SafeWebFetcher.publicAddress(InetAddress.getByName(ip))).as(ip).isFalse();
        assertThat(SafeWebFetcher.publicAddress(InetAddress.getByName("8.8.8.8"))).isTrue();
        for (String url : new String[]{"file:///etc/passwd","http://user:pass@example.com","http://localhost:8080","ftp://example.com"})
            assertThatThrownBy(() -> SafeWebFetcher.validate(url)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SafeWebFetcher().fetch("http://127.0.0.1")).isInstanceOf(java.io.IOException.class);
    }
}
