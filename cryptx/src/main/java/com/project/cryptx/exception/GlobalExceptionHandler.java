package com.project.cryptx.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.cryptx.config.SingletonLogger;
import com.razorpay.RazorpayException;
import io.sentry.Sentry;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final SingletonLogger log = SingletonLogger.log();

    // 1. Max Upload Size Exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size limit exceeded: {}", ex.getMessage());
        return buildErrorResponse("File size is too large. Please upload an image smaller than 1MB.",
                HttpStatus.CONTENT_TOO_LARGE);
    }

    // 2. Bad Credentials (UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed authentication attempt: {}", ex.getMessage());
        return buildErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    // 3. Resource Not Found (NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found exception: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // 4. Insufficient Balance (BAD_REQUEST)
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Object> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance exception: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 5. Invalid Amount (BAD_REQUEST)
    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<Object> handleInvalidAmount(InvalidAmountException ex) {
        log.warn("Invalid amount exception: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 6. User Already Exists (CONFLICT)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("Conflict: User already exists: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // 7. Invalid Token (UNAUTHORIZED or BAD_REQUEST)
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Token validation failed: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // 8. External Service Error (BAD_GATEWAY)
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Object> handleExternalService(ExternalServiceException ex) {
        log.error("External service failure: {}", ex.getMessage(), ex);
        Sentry.captureException(ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    // 9. Generic Bad Request (BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request exception: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 10. Spring Validation Errors (BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", details);
        return buildErrorResponse("Validation failed: " + details, HttpStatus.BAD_REQUEST);
    }

    // 11. Http Message Not Readable (e.g., malformed JSON body)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON payload received: {}", ex.getMessage());
        return buildErrorResponse("Malformed JSON request body", HttpStatus.BAD_REQUEST);
    }

    // 12. Missing Servlet Request Parameter (BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return buildErrorResponse("Required request parameter '" + ex.getParameterName() + "' is missing",
                HttpStatus.BAD_REQUEST);
    }

    // 13. HTTP Method Not Supported (METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        return buildErrorResponse("Method '" + ex.getMethod() + "' not allowed for this path",
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 14. Data/Database Constraints (CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database integrity violation occurred: {}", ex.getMessage(), ex);
        Sentry.captureException(ex);
        return buildErrorResponse("A database constraint conflict occurred. Please ensure unique inputs.",
                HttpStatus.CONFLICT);
    }

    // 15. Razorpay Integration Exception
    @ExceptionHandler(RazorpayException.class)
    public ResponseEntity<Object> handleRazorpayException(RazorpayException ex) {
        log.error("RazorPay API Error: {}", ex.getMessage(), ex);
        Sentry.captureException(ex);
        return buildErrorResponse("Payment gateway error: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    // 16. Fallback for any other general exceptions (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.error("An unexpected internal error occurred: ", ex);
        Sentry.captureException(ex);
        return buildErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", message);
        body.put("status", status.value());

        return new ResponseEntity<>(body, status);
    }
}
