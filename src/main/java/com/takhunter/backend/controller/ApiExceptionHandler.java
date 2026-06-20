package com.takhunter.backend.controller;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(Map.of(
                "message", exception.getReason() != null ? exception.getReason() : status.getReasonPhrase(),
                "status", status.value()
        ));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", resolveBadRequestMessage(exception),
                "status", HttpStatus.BAD_REQUEST.value()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException exception) {
        String message = resolveDataIntegrityMessage(exception);

        return ResponseEntity.badRequest().body(Map.of(
                "message", message,
                "status", HttpStatus.BAD_REQUEST.value()
        ));
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<Map<String, Object>> handleMultipart(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Banner maksimal 2MB",
                "status", HttpStatus.BAD_REQUEST.value()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Terjadi kesalahan pada server.",
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();
        String normalized = detail == null ? "" : detail.toLowerCase();

        if (normalized.contains("nama") || normalized.contains("nama_kegiatan") || normalized.contains("nama_kegiatan")) {
            return "Nama kegiatan sudah digunakan";
        }
        if (normalized.contains("kategori")) {
            return "Kategori tidak valid";
        }
        if (normalized.contains("status_publikasi") || normalized.contains("statuspublikasi")) {
            return "Status publikasi tidak valid";
        }
        if (normalized.contains("tanggal") || normalized.contains("pendaftaran")) {
            return "Tanggal kegiatan atau pendaftaran tidak valid";
        }
        if (normalized.contains("kuota")) {
            return "Kuota peserta tidak valid";
        }

        return "Data kegiatan tidak valid";
    }

    private String resolveBadRequestMessage(Exception exception) {
        if (exception instanceof BindException bindException && bindException.getFieldError() != null) {
            FieldError fieldError = bindException.getFieldError();
            return "Field " + fieldError.getField() + " tidak valid";
        }
        if (exception instanceof MethodArgumentNotValidException validException && validException.getFieldError() != null) {
            FieldError fieldError = validException.getFieldError();
            return "Field " + fieldError.getField() + " tidak valid";
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            return "Field " + mismatchException.getName() + " tidak valid";
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "Format JSON, tanggal, atau waktu tidak valid";
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return "Content-Type tidak didukung";
        }
        if (exception instanceof MissingServletRequestParameterException missingException) {
            return "Field " + missingException.getParameterName() + " wajib diisi";
        }

        return exception.getMessage();
    }
}
