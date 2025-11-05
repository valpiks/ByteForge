package io.byteforge.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;


public class ExecutionDto {

    @Data
    @Builder
    public static class CodeExecutionRequest {
        @NotBlank
        private String code;
        private int time_limit = 10;
        private int memory_limit = 256;

        public static CodeExecutionRequest toDto(String code, int timeLimit, int memoryLimit) {
            return CodeExecutionRequest.builder()
                    .code(code)
                    .time_limit(timeLimit)
                    .memory_limit(memoryLimit)
                    .build();
        }

    }
}
