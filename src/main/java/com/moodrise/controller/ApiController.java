// src/main/java/com/moodrise/controller/ApiController.java
package com.moodrise.controller;

import com.moodrise.dto.*;
import com.moodrise.model.Category;
import com.moodrise.model.ContentItem;
import com.moodrise.model.Reaction;
import com.moodrise.service.ContentService;
import com.moodrise.service.NotificationsService;
import com.moodrise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // open CORS for hackathon demo
public class ApiController {

    private final ContentService content;
    private final UserService users;
    private final NotificationsService notifications;

    // ---- Session & Mood ----

    @PostMapping("/session/start")
    public ResponseEntity<?> start(@RequestBody StartSessionRequest req) {
        users.startSession(req.getUserId(), req.getMoodStart());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/session/check")
    public ResponseEntity<?> check(@RequestBody MoodCheckRequest req) {
        users.moodCheck(req.getUserId(), req.getMood());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "nextCheckTs", users.nextCheckTs(req.getUserId())
        ));
    }

    @PostMapping("/session/end")
    public ResponseEntity<?> end(@RequestBody EndSessionRequest req) {
        Map<String, Object> summary = users.endSession(req.getUserId(), req.getMoodEnd());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/session/next-check")
    public ResponseEntity<?> nextCheck(@RequestParam String userId) {
        Instant ts = users.nextCheckTs(userId);
        return ResponseEntity.ok(Map.of("nextCheckTs", ts));
    }

    // ---- Usage Limit ----

    @GetMapping("/limit/status")
    public LimitStatus limit(@RequestParam String userId) {
        return users.limitStatus(userId);
    }

    // ---- Feed & Feedback ----

    @GetMapping("/content")
    public List<ContentItem> feed(
            @RequestParam String userId,
            @RequestParam int mood,
            @RequestParam Category tab,
            @RequestParam(defaultValue = "10") int limit
    ) {
        users.recordInteraction(userId);
        return content.curated(mood, tab, limit, users.hidden(userId));
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> feedback(@RequestBody FeedbackRequest req) {
        users.recordInteraction(req.getUserId());
        content.applyFeedback(req.getItemId(), req.getReaction());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/hide")
    public ResponseEntity<?> hide(@RequestBody HideRequest req) {
        users.hide(req.getUserId(), req.getItemId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---- Notifications (wellness nudges / good news–style tips) ----

    @GetMapping("/notifications/today")
    public List<String> today(@RequestParam String userId) {
        Integer latestMood = users.getOrCreate(userId).getMoodCurrent();
        users.recordInteraction(userId);
        return notifications.todayReminders(latestMood);
    }

    // ---- Calendar & Streak ----

    @GetMapping("/calendar")
    public Map<LocalDate, ?> calendar(@RequestParam String userId) {
        users.recordInteraction(userId);
        return users.calendar(userId);
    }

    @GetMapping("/calendar/streak")
    public Map<String, Object> streak(@RequestParam String userId) {
        users.recordInteraction(userId);
        return Map.of("goodMoodStreakDays", users.goodMoodStreak(userId));
    }
}
