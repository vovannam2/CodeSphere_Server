package com.hcmute.codesphere_server.model.payload.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestCaseRequest {
    // Các trường optional để cập nhật một phần
    private String input;
    private String expectedOutput;
    private Boolean isSample;
    private Boolean isHidden;
    private Integer weight;
}