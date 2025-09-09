// src/main/java/com/moodrise/service/ContentService.java
package com.moodrise.service;

import com.moodrise.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ProfanityGuard profanityGuard;

    private final List<ContentItem> all = new ArrayList<>();
    private final Map<String, Integer> reinforcement = new ConcurrentHashMap<>();

    @PostConstruct
    public void seed() {
        // Keep it short for demo; add more items as you wish
        all.add(ContentItem.builder().id("edu_01").category(Category.Educate).type(ContentType.text)
                .text("Tip: Try 4-7-8 breathing to calm quickly.").score(0).build());
        all.add(ContentItem.builder().id("edu_02").category(Category.Educate).type(ContentType.article)
                .url("https://www.sleepfoundation.org/how-sleep-works/why-do-we-need-sleep")
                .text("Article: Why consistent sleep improves mood & focus.").score(0).build());
        all.add(ContentItem.builder().id("edu_03").category(Category.Educate).type(ContentType.text)
                .text("Study hack: 25-min focus + 5-min stretch.").score(0).build());

        all.add(ContentItem.builder().id("laugh_01").category(Category.Laugh).type(ContentType.image)
                .url("https://i.imgur.com/8Q3Zt.jpg").text("Otter encouragement 🦦").score(0).build());
        all.add(ContentItem.builder().id("laugh_02").category(Category.Laugh).type(ContentType.video)
                .url("https://example.com/funny-dog-10s.mp4").text("10s dog zoomies 🐶").score(0).build());
        all.add(ContentItem.builder().id("laugh_03").category(Category.Laugh).type(ContentType.text)
                .text("Joke: Why did the web dev stay calm? Because he had async support 😌").score(0).build());

        all.add(ContentItem.builder().id("mot_01").category(Category.Motivate).type(ContentType.text)
                .text("Micro-win: sip water now 💧").score(0).build());
        all.add(ContentItem.builder().id("mot_02").category(Category.Motivate).type(ContentType.text)
                .text("Two minutes of stretching resets your posture.").score(0).build());
        all.add(ContentItem.builder().id("mot_03").category(Category.Motivate).type(ContentType.text)
                .text("A 7-minute walk beats a 7-minute scroll.").score(0).build());

        all.forEach(i -> i.setText(profanityGuard.cleanse(i.getText())));
    }

    public void applyFeedback(String itemId, Reaction reaction) {
        reinforcement.compute(itemId, (k, v) -> {
            int base = (v == null ? 0 : v);
            if (reaction == Reaction.smile) return base + 1;
            if (reaction == Reaction.sad)   return Math.max(-3, base - 1);
            return base;
        });
    }

    private int byReinforcement(ContentItem a, ContentItem b) {
        int sa = reinforcement.getOrDefault(a.getId(), a.getScore());
        int sb = reinforcement.getOrDefault(b.getId(), b.getScore());
        return Integer.compare(sb, sa);
    }

    private Category backupFor(int mood, Category tab) {
        // If low mood: Laugh/Motivate; neutral: Motivate/Educate; high: Educate/Motivate
        List<Category> prefs = (mood <= 2)
                ? List.of(Category.Laugh, Category.Motivate)
                : (mood == 3 ? List.of(Category.Motivate, Category.Educate)
                : List.of(Category.Educate, Category.Motivate));

        for (Category c : prefs) if (c != tab) return c;
        return prefs.get(0);
    }

    public List<ContentItem> curated(int mood, Category tab, int limit, Set<String> hiddenIds) {
        if (limit <= 0) limit = 10;
        Category backup = backupFor(mood, tab);

        List<ContentItem> primary = all.stream()
                .filter(i -> i.getCategory() == tab && !hiddenIds.contains(i.getId()))
                .sorted(this::byReinforcement)
                .collect(Collectors.toList());

        List<ContentItem> cross = all.stream()
                .filter(i -> i.getCategory() == backup && !hiddenIds.contains(i.getId()))
                .sorted(this::byReinforcement)
                .collect(Collectors.toList());

        List<ContentItem> out = new ArrayList<>();
        while (out.size() < limit && (!primary.isEmpty() || !cross.isEmpty())) {
            if (!primary.isEmpty()) out.add(primary.remove(0));
            if (out.size() < limit && !primary.isEmpty()) out.add(primary.remove(0));
            if (out.size() < limit && !cross.isEmpty()) out.add(cross.remove(0));
        }
        out.forEach(i -> i.setText(profanityGuard.cleanse(i.getText())));
        return out;
    }
}
