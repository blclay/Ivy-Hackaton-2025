// src/main/java/com/moodrise/dto/FeedbackRequest.java
package com.moodrise.dto;

import com.moodrise.model.Reaction;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FeedbackRequest {
    private String userId;
    private String itemId;
    private Reaction reaction;
}
