package com.codeatlas.service;

import com.codeatlas.domain.ApiModels.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@org.springframework.context.annotation.Profile("demo")
public class DemoDataService {
    private final List<Document> documents = List.of(
            new Document("doc-001", "Spring Boot 3.5 Reference Documentation", "OFFICIAL_DOC", "Spring",
                    "https://docs.spring.io/spring-boot/", "Spring Boot", "3.5.x", "en",
                    List.of("Java", "Web", "官方文档"), "INDEXED", "Apache-2.0", "2026-09-29 16:20", 184,
                    "Spring Boot makes it easy to create stand-alone, production-grade Spring applications.",
                    "# Spring Boot 3.5\n\n本文档介绍自动配置、外部化配置、Web 开发、数据访问、测试和生产运维。\n\n## Externalized Configuration\n\nSpring Boot 支持 properties、YAML、环境变量和命令行参数。"),
            new Document("doc-002", "PostgreSQL 16：向量检索与全文检索组合实践", "MANUAL_NOTE", "个人笔记",
                    null, "PostgreSQL", "16", "zh",
                    List.of("pgvector", "混合检索", "RAG"), "INDEXED", "Private", "2026-09-28 21:45", 26,
                    "使用 RRF 合并 ts_rank 与向量相似度结果，兼顾精确词与语义召回。",
                    "# PostgreSQL 混合检索\n\n精确的类名、错误码和 API 名称使用全文检索召回，自然语言问题使用向量召回，最后通过 RRF 合并排名。"),
            new Document("doc-003", "Redis 8.2 Release Notes", "GITHUB_RELEASE", "redis/redis",
                    "https://github.com/redis/redis/releases", "Redis", "8.2", "en",
                    List.of("Release", "升级", "缓存"), "PENDING_REVIEW", "BSD-3-Clause", "2026-09-30 08:12", 0,
                    "New release candidate discovered by the Redis tracking policy.",
                    "尚未批准，批准后才会解析正文并建立正式索引。"),
            new Document("doc-004", "Vue 3 Composition API 常见陷阱", "WEB_PAGE", "Vue.js Blog",
                    "https://vuejs.org/guide/", "Vue", "3.5", "zh",
                    List.of("前端", "Composition API"), "INDEXED", "MIT", "2026-09-27 10:30", 41,
                    "响应式解构、watch 依赖和组件生命周期中的常见问题。",
                    "# Composition API\n\n保留响应式引用时使用 toRefs；watchEffect 适合自动跟踪同步执行期间读取的依赖。"),
            new Document("doc-005", "Java 21 Virtual Threads Guide", "OFFICIAL_DOC", "OpenJDK",
                    "https://openjdk.org/jeps/444", "Java", "21", "en",
                    List.of("并发", "虚拟线程", "JEP 444"), "INDEXED", "GPL-2.0", "2026-09-26 14:10", 33,
                    "Virtual threads are lightweight threads that reduce the effort of writing concurrent applications.",
                    "# Virtual Threads\n\n虚拟线程适合大量以阻塞 I/O 为主的并发任务，不用于让 CPU 密集型任务运行得更快。")
    );

    public Health health() {
        return new Health("ok", "code-atlas-server", "0.1.0", true, Instant.now());
    }

    public Dashboard dashboard() {
        return new Dashboard(1284, 18642, 7, 4, 1, 8.37,
                List.of(
                        new Activity("act-1", "review", "发现 Redis 8.2 发布说明", "等待确认版本相关性", "12 分钟前", "warning"),
                        new Activity("act-2", "index", "Spring Boot 文档完成索引", "新增 184 个分块", "48 分钟前", "success"),
                        new Activity("act-3", "collect", "Java 官方来源检查完成", "发现 3 项，接受 2 项", "2 小时前", "success"),
                        new Activity("act-4", "error", "Vue Blog 抓取失败", "上游返回 429，已安排重试", "3 小时前", "danger")
                ),
                List.of(
                        new TrendPoint("09-24", 18, 12), new TrendPoint("09-25", 24, 19),
                        new TrendPoint("09-26", 15, 14), new TrendPoint("09-27", 31, 22),
                        new TrendPoint("09-28", 20, 18), new TrendPoint("09-29", 42, 35),
                        new TrendPoint("09-30", 28, 21)
                ));
    }

    public List<Document> documents() { return documents; }

    public Document document(String id) {
        return documents.stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到资料：" + id));
    }

    public List<CollectionPolicy> policies() {
        return List.of(
                new CollectionPolicy("policy-1", "Java 后端核心栈", List.of("Java 21", "Spring Boot 3.x", "PostgreSQL", "Redis"),
                        List.of("官方文档", "GitHub Releases", "RSS"), "每天 08:00", "MANUAL", 50, 12, true, "今天 08:04", "明天 08:00"),
                new CollectionPolicy("policy-2", "前端框架动态", List.of("Vue 3", "TypeScript", "Vite"),
                        List.of("官方博客", "GitHub Releases"), "每周一 09:00", "MANUAL", 30, 6, true, "周一 09:02", "下周一 09:00"),
                new CollectionPolicy("policy-3", "AI 工程与 RAG", List.of("Spring AI", "pgvector", "Reranker"),
                        List.of("指定网页", "RSS"), "每 12 小时", "MANUAL", 20, 10, false, "09-26 20:00", "已暂停")
        );
    }

    public List<CollectionTask> tasks() {
        return List.of(
                new CollectionTask("task-2491", "Java 后端核心栈", "GitHub Releases", "SUCCEEDED", "今天 08:00", 12, 7, "2m 18s", "完成"),
                new CollectionTask("task-2490", "Java 后端核心栈", "Spring 官方文档", "SUCCEEDED", "今天 08:00", 8, 2, "1m 46s", "正文哈希发现 2 项变化"),
                new CollectionTask("task-2489", "前端框架动态", "Vue Blog", "RETRYING", "昨天 09:00", 0, 0, "32s", "HTTP 429，将在 15 分钟后重试"),
                new CollectionTask("task-2488", "AI 工程与 RAG", "指定网页", "PAUSED", "09-26 20:00", 16, 9, "4m 09s", "策略已暂停")
        );
    }

    public List<ReviewItem> reviews() {
        return List.of(
                new ReviewItem("review-1", "Redis 8.2 Release Notes", "github.com/redis/redis", "Redis", "8.2", 0.96, "12 分钟前",
                        "命中版本发布、性能与破坏性变更规则", List.of("新增命令行为说明", "弃用旧配置项", "复制性能改进")),
                new ReviewItem("review-2", "Spring Boot 3.5.7 available now", "spring.io/blog", "Spring Boot", "3.5.7", 0.91, "1 小时前",
                        "命中关注版本 3.x 和官方来源", List.of("依赖升级", "安全修复", "配置元数据修正")),
                new ReviewItem("review-3", "PostgreSQL 18 Query Optimizer Deep Dive", "example.dev", "PostgreSQL", "18", 0.78, "3 小时前",
                        "主题相关，但版本不在当前 16—17 范围", List.of("优化器变化", "性能测试"))
        );
    }

    public SearchAnswer search(SearchRequest request) {
        String query = request.query() == null || request.query().isBlank() ? "Spring Boot 外部化配置" : request.query();
        return new SearchAnswer(
                "根据当前知识库，Spring Boot 3.5 支持通过 properties、YAML、环境变量和命令行参数提供外部化配置。配置有明确的属性源优先级，部署时可以使用环境变量覆盖包内默认值。对于包含密钥的配置，应从运行环境注入，避免写入源码。[1]\n\n如果要在容器中部署，建议把非敏感默认值保留在 application.yml，将数据库密码和模型密钥通过环境变量传入。",
                List.of(new Citation("cite-1", "Spring Boot 3.5 Reference Documentation", "https://docs.spring.io/spring-boot/", "Externalized Configuration",
                        "Spring Boot lets you externalize your configuration so that you can work with the same application code in different environments.", 0.94)),
                documents.stream().filter(item -> item.title().toLowerCase().contains(query.toLowerCase()) || item.technology().toLowerCase().contains("spring")).limit(3).toList(),
                true);
    }
}

