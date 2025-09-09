// src/main/java/com/moodrise/dto/HideRequest.java
package com.moodrise.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HideRequest {
    private String userId;
    private String itemId;
}
