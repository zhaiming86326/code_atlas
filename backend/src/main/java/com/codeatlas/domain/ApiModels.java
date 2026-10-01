package com.codeatlas.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {}

    public record Health(String status, String service, String version, boolean demoMode, Instant timestamp) {}

    public record Dashboard(
            int documents,
            int chunks,
            int pendingReviews,
            int activePolicies,
            int failedTasks,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS) Double monthlyCost,
            List<Activity> recentActivities,
            List<TrendPoint> ingestionTrend) {}

    public record Activity(String id, String type, String title, String detail, String time, String status) {}
    public record TrendPoint(String date, int discovered, int indexed) {}

    public record Document(
            String id,
            String title,
            String sourceType,
            String sourceName,
            String url,
            String technology,
            String version,
            String language,
            List<String> tags,
            String status,
            String license,
            String updatedAt,
            int chunks,
            String excerpt,
            String content) {}

    public record CollectionPolicy(
            String id,
            String name,
            List<String> topics,
            List<String> sources,
            String schedule,
            String reviewMode,
            int dailyLimit,
            double monthlyBudget,
            boolean enabled,
            String lastRun,
            String nextRun) {}

    public record CollectionTask(
            String id,
            String policy,
            String source,
            String status,
            String startedAt,
            int discovered,
            int accepted,
            String duration,
            String message) {}

    public record ReviewItem(
            String id,
            String title,
            String source,
            String technology,
            String version,
            double relevance,
            String discoveredAt,
            String reason,
            List<String> changes) {}

    public record SearchRequest(String query, List<String> technologies, String version, String searchMode) {}
    public record Citation(String id, String title, String url, String section, String quote, double score) {}
    public record SearchAnswer(String answer, List<Citation> citations, List<Document> results, boolean demo, Map<String,String> resultVersions) {
        public SearchAnswer(String answer, List<Citation> citations, List<Document> results, boolean demo) {
            this(answer,citations,results,demo,Map.of());
        }
    }

    public record PagedResponse<T>(List<T> items, int page, int pageSize, long total) {}
    public record ApiError(String code, String message, Instant timestamp, Map<String, String> details) {}
}

