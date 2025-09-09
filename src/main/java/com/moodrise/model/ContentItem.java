// src/main/java/com/moodrise/model/ContentItem.java
package com.moodrise.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContentItem {
    private String id;               // e.g., "laugh_01"
    private Category category;       // Educate | Laugh | Motivate
    private ContentType type;        // text | image | video | article
    private String url;              // for image/video/article
    private String text;             // for text/overlay
    private int score;               // reinforcement from reactions
}
