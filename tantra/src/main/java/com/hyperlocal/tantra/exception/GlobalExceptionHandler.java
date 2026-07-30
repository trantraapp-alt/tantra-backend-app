package com.hyperlocal.tantra.exception;

import com.hyperlocal.tantra.common.dto.ApiResponse;
import com.hyperlocal.tantra.common.dto.ErrorDetail;
import com.hyperlocal.tantra.common.error.ErrorCode;
import com.hyperlocal.tantra.common.web.TraceIdFilter;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/**
 * Turns every exception into the standard {@link ApiResponse} error envelope with a stable code,
 * bilingual message and the request traceId. Client mistakes (4xx) are logged briefly without a stack
 * trace; only genuinely unexpected server errors (5xx) log a full stack trace. Internals never leak.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------- app / domain errors ----------------

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalized(LocalizedException ex) {
        return build(HttpStatus.valueOf(ex.getStatus()), ex.getCode(), ex.getLocalized());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                LocalizedText.of(safe(ex.getMessage(), "Invalid request."),
                        safe(ex.getMessage(), "अमान्य अनुरोध।")));
    }

    // ---------------- request validation / binding (400) ----------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, LocalizedText.of(message, message));
    }

    /** Malformed / missing JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                LocalizedText.of("Malformed or missing request body.",
                        "अनुरोध का मुख्य भाग गलत या अनुपस्थित है।"));
    }

    /** Missing required query/form parameter. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "Missing required parameter: " + ex.getParameterName();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                LocalizedText.of(msg, "आवश्यक पैरामीटर अनुपस्थित: " + ex.getParameterName()));
    }

    /** Wrong type for a path/query param (e.g. a non-numeric id). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Invalid value for '" + ex.getName() + "'.";
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST,
                LocalizedText.of(msg, "'" + ex.getName() + "' के लिए अमान्य मान।"));
    }

    // ---------------- routing / method / media type ----------------

    /** Unknown route (no controller and no static resource) → clean 404 instead of an "unhandled" stack trace. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        log.debug("No resource for [{}] (traceId={})", ex.getResourcePath(), MDC.get(TraceIdFilter.TRACE_ID));
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                LocalizedText.of("The requested resource was not found.",
                        "अनुरोधित संसाधन नहीं मिला।"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED,
                LocalizedText.of("HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.",
                        "इस एंडपॉइंट के लिए '" + ex.getMethod() + "' मेथड समर्थित नहीं है।"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                LocalizedText.of("Unsupported content type.", "असमर्थित कंटेंट प्रकार।"));
    }

    // ---------------- security ----------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                LocalizedText.of("Authentication required or invalid.", "प्रमाणीकरण आवश्यक या अमान्य।"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                LocalizedText.of("Access denied.", "पहुँच अस्वीकृत।"));
    }

    // ---------------- data integrity (409) ----------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation (traceId={}): {}", MDC.get(TraceIdFilter.TRACE_ID), rootMessage(ex));
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                LocalizedText.of("This operation conflicts with existing data.",
                        "यह क्रिया मौजूदा डेटा से टकराती है।"));
    }

    // ---------------- catch-all (500) ----------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception [traceId={}]", MDC.get(TraceIdFilter.TRACE_ID), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL,
                LocalizedText.of("Something went wrong. Please try again.",
                        "कुछ गड़बड़ हो गई। कृपया पुनः प्रयास करें।"));
    }

    // ---------------- helpers ----------------

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, LocalizedText message) {
        ApiResponse<Void> body = ApiResponse.fail(new ErrorDetail(code, message));
        body.setTraceId(MDC.get(TraceIdFilter.TRACE_ID));
        body.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    private String safe(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage();
    }
}
