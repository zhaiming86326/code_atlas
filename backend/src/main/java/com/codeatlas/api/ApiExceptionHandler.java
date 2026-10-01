package com.codeatlas.api;

import com.codeatlas.domain.ApiModels.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError malformed(Exception ex) {
        return new ApiError("INVALID_BODY", "请求格式无效或超过大小限制，请检查 JSON、文件与参数", Instant.now(), Map.of());
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiError tooLarge(Exception ex) {
        return new ApiError("TOO_LARGE", "上传文件最大 2 MiB", Instant.now(), Map.of());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError notFound(IllegalArgumentException ex) {
        return new ApiError("INVALID_REQUEST", ex.getMessage(), Instant.now(), Map.of());
    }

    @ExceptionHandler(java.io.IOException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError fetch(java.io.IOException ex) {
        return new ApiError("FETCH_FAILED", networkMessage(ex), Instant.now(), Map.of());
    }
    public static String networkMessage(java.io.IOException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = java.util.Objects.toString(cause.getMessage(), "").toLowerCase(java.util.Locale.ROOT);
            if (cause instanceof java.net.NoRouteToHostException || message.contains("no route to host"))
                return "后端无法连接资料来源（No route to host）。请检查后端所在主机的网络路由、防火墙及 IPv4/IPv6 连通性；浏览器能访问不代表 Java 后端能访问，浏览器代理不会自动用于网页导入。也可先下载文件，再通过文件导入。";
            if (cause instanceof java.net.UnknownHostException)
                return message.contains("禁止访问") ? "资料来源解析到内网或保留地址，已阻止访问" : "后端无法解析资料来源域名，请检查 URL 与后端 DNS 配置";
            if (cause instanceof java.net.SocketTimeoutException || message.contains("connect timed out"))
                return "后端获取资料来源超时，请检查网络连通性或稍后重试";
            if (cause instanceof java.net.ConnectException)
                return "后端无法建立到资料来源的连接，请检查来源服务、后端网络及防火墙";
            if (cause instanceof javax.net.ssl.SSLException)
                return "资料来源 TLS 连接失败，请检查来源证书与后端 Java 信任证书配置";
        }
        return java.util.Objects.toString(ex.getMessage(), "网页获取失败");
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError database(org.springframework.dao.DataAccessException ex) {
        return new ApiError("DATABASE_ERROR", "数据库操作失败，请检查连接、权限和数据库迁移 SQL 是否已导入", Instant.now(), Map.of());
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiError> status(org.springframework.web.server.ResponseStatusException ex) {
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(new ApiError("REQUEST_FAILED",ex.getReason(),Instant.now(),Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("请求参数不正确");
        return new ApiError("VALIDATION_ERROR", message, Instant.now(), Map.of());
    }
}

