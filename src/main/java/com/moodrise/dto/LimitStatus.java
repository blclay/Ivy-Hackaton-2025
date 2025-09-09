// src/main/java/com/moodrise/dto/LimitStatus.java
package com.moodrise.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LimitStatus {
    private boolean allowed;             // can the user keep using app now?
    private long remainingMillisToday;   // until daily cap
    private long usedMillisToday;        // already used today
    private long dailyCapMillis;         // constant: 60m
}
