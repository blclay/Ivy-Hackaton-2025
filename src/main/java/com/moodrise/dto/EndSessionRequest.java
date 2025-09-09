// src/main/java/com/moodrise/dto/EndSessionRequest.java
package com.moodrise.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EndSessionRequest {
    private String userId;
    private int moodEnd; // 1..5
}
