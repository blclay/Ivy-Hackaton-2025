// src/main/java/com/moodrise/dto/MoodCheckRequest.java
package com.moodrise.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MoodCheckRequest {
    private String userId;
    private int mood; // 1..5
}
