package io.byteforge.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    private int status;
    private String error;
    private String message;
    private Date timestamp;
    private Map<String, String> details;

    public static ErrorResponseDto of(int status, String error, String message) {
        return new ErrorResponseDto(status, error, message, new Date(), null);
    }

    public static ErrorResponseDto of(int status, String error, String message, Map<String, String> details) {
        return new ErrorResponseDto(status, error, message, new Date(), details);
    }
}