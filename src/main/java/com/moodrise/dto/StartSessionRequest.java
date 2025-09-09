// src/main/java/com/moodrise/dto/StartSessionRequest.java
package com.moodrise.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StartSessionRequest {
    private String userId;
    private int moodStart; // 1..5
}
