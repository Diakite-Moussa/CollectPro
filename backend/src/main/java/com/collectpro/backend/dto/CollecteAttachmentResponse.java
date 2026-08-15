package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CollecteAttachmentResponse {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String url;
}
