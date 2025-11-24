package com.hcmute.codesphere_server.model.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    private String content; // Required cho TEXT, null cho IMAGE
    
    private String messageType; // TEXT hoặc IMAGE
    
    private String imageUrl; // Required cho IMAGE
}

