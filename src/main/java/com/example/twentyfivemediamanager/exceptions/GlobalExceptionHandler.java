package com.example.twentyfivemediamanager.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        log.warn("Security exception handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request");
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFoundException(FileNotFoundException ex) {
        log.warn("File not found handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<String> handleFileUploadException(FileUploadException ex) {
        log.error("File upload exception handled", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
    }

    @ExceptionHandler(FileDownloadException.class)
    public ResponseEntity<String> handleFileDownloadException(FileDownloadException ex) {
        log.error("File download exception handled", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File download failed");
    }

    @ExceptionHandler(FileDeleteException.class)
    public ResponseEntity<String> handleFileDeleteException(FileDeleteException ex) {
        log.error("File delete exception handled", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File delete failed");
    }

    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<String> handleFileOperationException(FileOperationException ex) {
        log.error("File operation exception handled", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File operation failed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        log.error("Unexpected exception handled", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Operation failed");
    }
}