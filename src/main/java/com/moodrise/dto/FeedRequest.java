// src/main/java/com/moodrise/dto/FeedRequest.java
package com.moodrise.dto;

import com.moodrise.model.Category;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FeedRequest {
    private String userId;
    private int mood;        // most recent mood reading
    private Category tab;    // Educate | Laugh | Motivate
    private int limit;       // default 10
}
