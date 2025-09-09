// src/main/java/com/moodrise/service/NotificationsService.java
package com.moodrise.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class NotificationsService {

    private final Random rand = new Random();

    private static final String[] GENERIC_TIPS = new String[] {
            "Mini-tip: stand up, roll your shoulders, breathe in 4-7-8.",
            "Hydration nudge: grab a glass of water.",
            "Fresh air helps: look out a window or step outside for 1 minute.",
            "Sleep reminder: aim for 7-9 hours tonight—prep a wind-down routine.",
            "Study boost: try 25 min focus + 5 min stretch.",
            "Good news: kindness spreads—send a supportive text today."
    };

    private static final String[] MOOD_LOW_TIPS = new String[] {
            "Feeling low? Try 3 slow breaths and a 2-minute walk.",
            "Text a friend a quick hello—connection helps.",
            "Pick ‘Laugh’ for a mood lift in under a minute."
    };

    private static final String[] MOOD_OK_TIPS = new String[] {
            "Nice steadiness. A short stretch keeps it going.",
            "Try ‘Motivate’ for a micro boost to your focus."
    };

    private static final String[] MOOD_HIGH_TIPS = new String[] {
            "Great energy—channel it into a tiny task you’ve been delaying.",
            "Share a kind word; helping others lifts you too."
    };

    public List<String> todayReminders(Integer latestMood) {
        List<String> out = new ArrayList<>();
        // simple schedule buckets; front end can decide when to surface
        LocalTime now = LocalTime.now();
        out.add("[" + now.plusMinutes(30) + "] " + pickByMood(latestMood));
        out.add("[" + now.plusHours(2) + "] " + GENERIC_TIPS[rand.nextInt(GENERIC_TIPS.length)]);
        out.add("[" + now.plusHours(4) + "] " + GENERIC_TIPS[rand.nextInt(GENERIC_TIPS.length)]);
        return out;
    }

    private String pickByMood(Integer mood) {
        if (mood == null) return GENERIC_TIPS[rand.nextInt(GENERIC_TIPS.length)];
        if (mood <= 2) return MOOD_LOW_TIPS[rand.nextInt(MOOD_LOW_TIPS.length)];
        if (mood == 3) return MOOD_OK_TIPS[rand.nextInt(MOOD_OK_TIPS.length)];
        return MOOD_HIGH_TIPS[rand.nextInt(MOOD_HIGH_TIPS.length)];
    }
}
