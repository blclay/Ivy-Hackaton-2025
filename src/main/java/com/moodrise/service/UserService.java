// src/main/java/com/moodrise/service/UserService.java
package com.moodrise.service;

import com.moodrise.dto.LimitStatus;
import com.moodrise.model.DaySummary;
import com.moodrise.model.UserState;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final long DAILY_CAP_MILLIS = 60L * 60L * 1000L;   // 60 minutes
    private static final long FIRST_RECHECK_SEC = 5L * 60L;           // 5 minutes
    private static final long NEXT_RECHECK_MIN_SEC = 15L * 60L;       // 15 minutes
    private static final long NEXT_RECHECK_MAX_SEC = 20L * 60L;       // 20 minutes

    private final Map<String, UserState> users = new ConcurrentHashMap<>();
    private final Random rand = new Random();

    public UserState getOrCreate(String userId) {
        return users.compute(userId, (k, s) -> {
            LocalDate today = LocalDate.now();
            if (s == null) {
                return UserState.builder()
                        .userId(userId)
                        .currentDay(today)
                        .hiddenItemIds(new HashSet<>())
                        .calendar(new HashMap<>())
                        .usageTodayMillis(0L)
                        .goodMoodStreakDays(0)
                        .build();
            }
            // Reset daily stats if day rolled over
            if (!today.equals(s.getCurrentDay())) {
                s.setCurrentDay(today);
                s.setUsageTodayMillis(0);
                s.setMoodStart(null);
                s.setMoodCurrent(null);
                s.setSessionStartTs(null);
                s.setLastInteractionTs(null);
                s.setNextCheckTs(null);
            }
            return s;
        });
    }

    public void startSession(String userId, int moodStart) {
        UserState u = getOrCreate(userId);
        Instant now = Instant.now();

        // Enforce daily cap quickly: if over, don't start
        if (u.getUsageTodayMillis() >= DAILY_CAP_MILLIS) return;

        u.setSessionStartTs(now);
        u.setLastInteractionTs(now);
        u.setMoodStart(moodStart);
        u.setMoodCurrent(moodStart);

        // schedule first recheck at +5 minutes
        u.setNextCheckTs(now.plusSeconds(FIRST_RECHECK_SEC));

        // init today in calendar
        u.getCalendar().computeIfAbsent(LocalDate.now(), d ->
                DaySummary.builder().moodStart(moodStart).moodEnd(null).usageMillis(0L).build());
    }

    public void recordInteraction(String userId) {
        UserState u = getOrCreate(userId);
        Instant now = Instant.now();

        // Update usage delta since last interaction
        if (u.getLastInteractionTs() != null) {
            long delta = Duration.between(u.getLastInteractionTs(), now).toMillis();
            long newTotal = Math.min(DAILY_CAP_MILLIS, u.getUsageTodayMillis() + Math.max(0, delta));
            u.setUsageTodayMillis(newTotal);
            // update calendar usage
            DaySummary ds = u.getCalendar().computeIfAbsent(LocalDate.now(), d ->
                    DaySummary.builder().moodStart(u.getMoodStart()).moodEnd(u.getMoodCurrent()).usageMillis(0L).build());
            ds.setUsageMillis(newTotal);
        }
        u.setLastInteractionTs(now);
    }

    public void moodCheck(String userId, int mood) {
        UserState u = getOrCreate(userId);
        u.setMoodCurrent(mood);
        recordInteraction(userId);

        // schedule next recheck 15–20 min from now
        long next = NEXT_RECHECK_MIN_SEC + rand.nextInt((int)(NEXT_RECHECK_MAX_SEC - NEXT_RECHECK_MIN_SEC + 1));
        u.setNextCheckTs(Instant.now().plusSeconds(next));
    }

    public Map<String, Object> endSession(String userId, int moodEnd) {
        UserState u = getOrCreate(userId);
        recordInteraction(userId);

        u.setMoodCurrent(moodEnd);
        // finalize day summary
        DaySummary ds = u.getCalendar().computeIfAbsent(LocalDate.now(), d ->
                DaySummary.builder().moodStart(u.getMoodStart()).moodEnd(moodEnd).usageMillis(u.getUsageTodayMillis()).build());
        if (ds.getMoodStart() == null) ds.setMoodStart(u.getMoodStart());
        ds.setMoodEnd(moodEnd);
        ds.setUsageMillis(u.getUsageTodayMillis());

        // streaks: consider "good mood" as moodEnd >= 4
        // If yesterday had good mood and today good too -> increment
        LocalDate today = u.getCurrentDay();
        LocalDate yesterday = today.minusDays(1);
        DaySummary ys = u.getCalendar().get(yesterday);
        boolean yesterdayGood = (ys != null && ys.getMoodEnd() != null && ys.getMoodEnd() >= 4);
        boolean todayGood = (moodEnd >= 4);
        if (todayGood) {
            u.setGoodMoodStreakDays((yesterdayGood ? u.getGoodMoodStreakDays() + 1 : Math.max(1, u.getGoodMoodStreakDays())));
        } else {
            u.setGoodMoodStreakDays(0);
        }

        // stop session markers
        u.setSessionStartTs(null);
        u.setNextCheckTs(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("moodStart", u.getMoodStart());
        resp.put("moodEnd", moodEnd);
        Integer start = u.getMoodStart();
        if (start != null) resp.put("delta", moodEnd - start);
        resp.put("tip", tipForDelta(start, moodEnd));
        return resp;
    }

    private String tipForDelta(Integer start, Integer end) {
        if (start == null || end == null) return "Nice work taking a mindful break today.";
        int d = end - start;
        if (d >= 2) return "Great lift! Keep it going—consider a quick walk or water break.";
        if (d >= 1) return "Nice improvement. Try a 2-minute stretch next.";
        if (d == 0) return "Steady is good. Maybe switch tabs or try a brief breathing exercise.";
        return "Tough session—consider a short rest from screens, a walk, or talk to a friend.";
    }

    public LimitStatus limitStatus(String userId) {
        UserState u = getOrCreate(userId);
        long remaining = Math.max(0, DAILY_CAP_MILLIS - u.getUsageTodayMillis());
        return LimitStatus.builder()
                .allowed(remaining > 0)
                .remainingMillisToday(remaining)
                .usedMillisToday(u.getUsageTodayMillis())
                .dailyCapMillis(DAILY_CAP_MILLIS)
                .build();
    }

    public Instant nextCheckTs(String userId) {
        return getOrCreate(userId).getNextCheckTs();
    }

    public Set<String> hidden(String userId) {
        return getOrCreate(userId).getHiddenItemIds();
    }

    public void hide(String userId, String itemId) {
        getOrCreate(userId).getHiddenItemIds().add(itemId);
        recordInteraction(userId);
    }

    public Map<LocalDate, DaySummary> calendar(String userId) {
        return getOrCreate(userId).getCalendar();
    }

    public int goodMoodStreak(String userId) {
        return getOrCreate(userId).getGoodMoodStreakDays();
    }
}
