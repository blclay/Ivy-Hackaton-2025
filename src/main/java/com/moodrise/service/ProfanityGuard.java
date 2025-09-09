// src/main/java/com/moodrise/service/ProfanityGuard.java
package com.moodrise.service;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class ProfanityGuard {
    // small demo list; expand safely later
    private static final Set<String> banned = Set.of(
            "damn","hell","shit","fuck","bitch","asshole"
    );

    public String cleanse(String text) {
        if (text == null) return null;
        String out = text;
        for (String bad : banned) {
            out = out.replaceAll("(?i)\\b" + bad + "\\b", "•••");
        }
        return out;
    }
}
