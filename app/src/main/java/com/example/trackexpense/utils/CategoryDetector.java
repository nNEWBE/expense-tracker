package com.example.trackexpense.utils;

import java.util.HashMap;
import java.util.Map;

public class CategoryDetector {

    private static final Map<String, String> KEYWORDS_TO_CATEGORY = new HashMap<>();

    static {
        // Food related
        KEYWORDS_TO_CATEGORY.put("lunch", "Food");
        KEYWORDS_TO_CATEGORY.put("dinner", "Food");
        KEYWORDS_TO_CATEGORY.put("breakfast", "Food");
        KEYWORDS_TO_CATEGORY.put("coffee", "Food");
        KEYWORDS_TO_CATEGORY.put("restaurant", "Food");
        KEYWORDS_TO_CATEGORY.put("pizza", "Food");
        KEYWORDS_TO_CATEGORY.put("burger", "Food");
        KEYWORDS_TO_CATEGORY.put("food", "Food");
        KEYWORDS_TO_CATEGORY.put("snack", "Food");
        KEYWORDS_TO_CATEGORY.put("grocery", "Food");

        // Transport related
        KEYWORDS_TO_CATEGORY.put("uber", "Transport");
        KEYWORDS_TO_CATEGORY.put("taxi", "Transport");
        KEYWORDS_TO_CATEGORY.put("bus", "Transport");
        KEYWORDS_TO_CATEGORY.put("train", "Transport");
        KEYWORDS_TO_CATEGORY.put("fuel", "Transport");
        KEYWORDS_TO_CATEGORY.put("gas", "Transport");
        KEYWORDS_TO_CATEGORY.put("petrol", "Transport");
        KEYWORDS_TO_CATEGORY.put("rickshaw", "Transport");
        KEYWORDS_TO_CATEGORY.put("cng", "Transport");

        // Shopping related
        KEYWORDS_TO_CATEGORY.put("shopping", "Shopping");
        KEYWORDS_TO_CATEGORY.put("clothes", "Shopping");
        KEYWORDS_TO_CATEGORY.put("shirt", "Shopping");
        KEYWORDS_TO_CATEGORY.put("shoes", "Shopping");
        KEYWORDS_TO_CATEGORY.put("amazon", "Shopping");
        KEYWORDS_TO_CATEGORY.put("daraz", "Shopping");

        // Entertainment
        KEYWORDS_TO_CATEGORY.put("movie", "Entertainment");
        KEYWORDS_TO_CATEGORY.put("netflix", "Entertainment");
        KEYWORDS_TO_CATEGORY.put("spotify", "Entertainment");
        KEYWORDS_TO_CATEGORY.put("game", "Entertainment");
        KEYWORDS_TO_CATEGORY.put("concert", "Entertainment");

        // Health
        KEYWORDS_TO_CATEGORY.put("medicine", "Health");
        KEYWORDS_TO_CATEGORY.put("doctor", "Health");
        KEYWORDS_TO_CATEGORY.put("hospital", "Health");
        KEYWORDS_TO_CATEGORY.put("pharmacy", "Health");
        KEYWORDS_TO_CATEGORY.put("gym", "Health");

        // Bills
        KEYWORDS_TO_CATEGORY.put("electricity", "Bills");
        KEYWORDS_TO_CATEGORY.put("water", "Bills");
        KEYWORDS_TO_CATEGORY.put("internet", "Bills");
        KEYWORDS_TO_CATEGORY.put("phone", "Bills");
        KEYWORDS_TO_CATEGORY.put("rent", "Bills");
        KEYWORDS_TO_CATEGORY.put("wifi", "Bills");

        // Education
        KEYWORDS_TO_CATEGORY.put("book", "Education");
        KEYWORDS_TO_CATEGORY.put("course", "Education");
        KEYWORDS_TO_CATEGORY.put("tuition", "Education");
        KEYWORDS_TO_CATEGORY.put("school", "Education");
        KEYWORDS_TO_CATEGORY.put("college", "Education");
        KEYWORDS_TO_CATEGORY.put("university", "Education");
    }

    public static String detectCategory(String note) {
        if (note == null || note.isEmpty()) {
            return "Other";
        }

        String lowerNote = note.toLowerCase();

        for (Map.Entry<String, String> entry : KEYWORDS_TO_CATEGORY.entrySet()) {
            if (lowerNote.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Other";
    }

    public static double extractAmount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Pattern to find numbers (handles "200", "200.50", "1,000")
        String cleanText = text.replaceAll("[^0-9.,]", " ");
        String[] parts = cleanText.trim().split("\\s+");

        for (String part : parts) {
            try {
                String cleaned = part.replace(",", "");
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException ignored) {
            }
        }

        return 0;
    }
}
