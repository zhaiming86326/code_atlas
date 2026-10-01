package com.codeatlas.service;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;
import java.net.*;
import java.io.*;
import java.util.Locale;

@Component
public class SafeWebFetcher {
    public record Fetched(byte[] raw, String url, boolean html) {}
    public static URI validate(String value) {
        URI uri;
        try { uri = URI.create(value); } catch (RuntimeException e) { throw new IllegalArgumentException("URL 无效"); }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80)))
            throw new IllegalArgumentException("仅允许无凭证的 HTTP/HTTPS 标准端口 URL");
        return URI.create(uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())).normalize();
    }
    public static boolean publicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] b = address.getAddress();
        int a = Byte.toUnsignedInt(b[0]);
        if (b.length == 4) {
            int c = Byte.toUnsignedInt(b[1]);
            return a != 0 && a != 127 && a < 224 && !(a == 100 && c >= 64 && c <= 127)
                    && !(a == 169 && c == 254) && !(a == 192 && (c == 0 || c == 168))
                    && !(a == 198 && (c == 18 || c == 19 || c == 51)) && !(a == 203 && c == 0);
        }
        // Allow IPv6 global unicast only; excludes ULA, mapped addresses, NAT64 and tunnels.
        return (a & 0xe0) == 0x20 && !(a == 0x20 && Byte.toUnsignedInt(b[1]) == 0x02)
                && !(a == 0x20 && Byte.toUnsignedInt(b[1]) == 0x01
                && (Byte.toUnsignedInt(b[2]) == 0x0d || Byte.toUnsignedInt(b[2]) == 0));
    }
    public Fetched fetch(String value) throws IOException {
        URI uri = validate(value);
        DnsResolver resolver = new DnsResolver() {
            public InetAddress[] resolve(String host) throws UnknownHostException {
                var addresses = InetAddress.getAllByName(host);
                for (var address : addresses) if (!publicAddress(address)) throw new UnknownHostException("禁止访问内网或保留地址");
                return addresses;
            }
            public String resolveCanonicalHostname(String host) { return host; }
        };
        var manager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(5))
                        .setSocketTimeout(Timeout.ofSeconds(10)).build()).build();
        try (var client = HttpClients.custom().setConnectionManager(manager).disableRedirectHandling()
                .disableAutomaticRetries().disableContentCompression()
                .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(10)).build()).build()) {
            long deadline = System.nanoTime() + 20_000_000_000L;
            for (int hop = 0; hop < 4; hop++) {
                if (System.nanoTime() > deadline) throw new IOException("网页获取总时限已到");
                var get = new HttpGet(uri);
                get.setHeader("User-Agent", "CodeAtlas/0.2 (personal knowledge import)");
                get.setHeader("Accept-Encoding", "identity");
                try (var response = client.execute(get)) {
                    int status = response.getCode();
                    if (status >= 300 && status < 400) {
                        var location = response.getFirstHeader("Location");
                        get.cancel();
                        if (location == null) throw new IOException("重定向缺少目标");
                        uri = validate(uri.resolve(location.getValue()).toString());
                        continue;
                    }
                    if (status != 200) { get.cancel(); throw new IOException("来源返回 HTTP " + status); }
                    var entity = response.getEntity();
                    if (entity == null || entity.getContentLength() > ContentParser.MAX_BYTES) { get.cancel(); throw new IOException("正文为空或超过 2 MiB"); }
                    String type = String.valueOf(entity.getContentType()).toLowerCase(Locale.ROOT);
                    if (!(type.startsWith("text/html") || type.startsWith("text/plain") || type.startsWith("text/markdown"))) {
                        get.cancel(); throw new IOException("仅支持 HTML、TXT、Markdown 网页");
                    }
                    var output = new ByteArrayOutputStream();
                    try (var input = entity.getContent()) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (System.nanoTime() > deadline) { get.cancel(); throw new IOException("网页获取总时限已到"); }
                            if (output.size() + count > ContentParser.MAX_BYTES) { get.cancel(); throw new IOException("下载超过 2 MiB"); }
                            output.write(buffer, 0, count);
                        }
                    }
                    return new Fetched(output.toByteArray(), uri.toString(), type.startsWith("text/html"));
                }
            }
            throw new IOException("重定向次数超过 3 次");
        }
    }
}
