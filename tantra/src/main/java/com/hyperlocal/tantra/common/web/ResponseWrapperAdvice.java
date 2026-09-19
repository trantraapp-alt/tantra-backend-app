package com.hyperlocal.tantra.common.web;

import com.hyperlocal.tantra.common.dto.ApiResponse;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;

/**
 * Wraps every controller return value in the standard {@link ApiResponse} envelope so controllers
 * can just return their data. Already-wrapped responses, plain strings and binary/resource bodies
 * pass through untouched; every envelope gets the traceId + timestamp stamped.
 */
@RestControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            stamp((ApiResponse<?>) body);
            return body;
        }
        // Strings (String converter) and binary payloads are left as-is.
        if (body instanceof String || body instanceof byte[] || body instanceof Resource) {
            return body;
        }

        ApiResponse<Object> wrapped = ApiResponse.ok(body);
        stamp(wrapped);
        return wrapped;
    }

    private void stamp(ApiResponse<?> api) {
        if (api.getTraceId() == null) api.setTraceId(MDC.get(TraceIdFilter.TRACE_ID));
        if (api.getTimestamp() == null) api.setTimestamp(LocalDateTime.now().toString());
    }
}
