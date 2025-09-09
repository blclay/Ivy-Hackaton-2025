// src/main/java/com/moodrise/model/UserState.java
package com.moodrise.model;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserState {
    private String userId;

    private LocalDate currentDay;
    private Integer moodStart;         // session/day start mood
    private Integer moodCurrent;       // most recent mood
    private Instant sessionStartTs;    // when current session began
    private Instant lastInteractionTs; // last any activity
    private Instant nextCheckTs;       // next scheduled check time (server-side clock basis)
    private long usageTodayMillis;     // accumulated active time today

    private Set<String> hiddenItemIds; // per-user hide list (demo scope)
    private Map<LocalDate, DaySummary> calendar; // by date
    private int goodMoodStreakDays;    // consecutive days with moodEnd >= 4
}
