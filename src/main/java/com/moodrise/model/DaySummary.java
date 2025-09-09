// src/main/java/com/moodrise/model/DaySummary.java
package com.moodrise.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DaySummary {
    private Integer moodStart;     // first mood of the day
    private Integer moodEnd;       // last mood recorded that day
    private long usageMillis;      // total usage today
}
