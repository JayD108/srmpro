package com.example.srmistpro;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MathEngine {

    // --- MOCK OFFLINE DATA (Replace this with your real academic calendar logic) ---
    public static final LocalDate SEMESTER_END = LocalDate.of(2026, 5, 20);
    public static final List<LocalDate> HOLIDAYS = new ArrayList<>();

    // Map: "Monday" -> List of Subject Codes
    public static final Map<String, List<String>> OFFLINE_TIMETABLE = new HashMap<>();

    static {
        // Initialize Dummy Timetable for Logic Check
        List<String> mon = new ArrayList<>(); mon.add("18CSC305J"); mon.add("18CSC304J");
        OFFLINE_TIMETABLE.put("MONDAY", mon);

        List<String> tue = new ArrayList<>(); tue.add("18CSC305J");
        OFFLINE_TIMETABLE.put("TUESDAY", tue);
    }

    // --- FORECAST LOGIC (Replaces calculate_forecast_offline) ---
    public static List<SubjectForecast> calculateForecast(List<SubjectModel> currentData, int targetPct) {
        List<SubjectForecast> forecasts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Calculate Remaining Classes Map
        Map<String, Integer> remainingClasses = new HashMap<>();
        LocalDate curr = today.plusDays(1);

        while (!curr.isAfter(SEMESTER_END)) {
            if (!isHoliday(curr) && curr.getDayOfWeek() != DayOfWeek.SUNDAY) {
                String dayName = curr.getDayOfWeek().toString(); // "MONDAY"
                if (OFFLINE_TIMETABLE.containsKey(dayName)) {
                    for (String code : OFFLINE_TIMETABLE.get(dayName)) {
                        remainingClasses.put(code, remainingClasses.getOrDefault(code, 0) + 1);
                    }
                }
            }
            curr = curr.plusDays(1);
        }

        // 2. Build Forecast Objects
        for (SubjectModel s : currentData) {
            int futureCount = remainingClasses.getOrDefault(s.code, 0);
            int totalProjected = s.maxHours + futureCount;

            // Formula from your Python script: max_bunks = total * (1 - target/100)
            int maxBunks = (int) (totalProjected * (1.0 - (targetPct / 100.0)));
            int currentAbsent = s.maxHours - s.attendedHours;
            int skipsLeft = maxBunks - currentAbsent;

            forecasts.add(new SubjectForecast(s.name, s.code, s.currentPct, futureCount, Math.max(0, skipsLeft)));
        }
        return forecasts;
    }

    private static boolean isHoliday(LocalDate date) {
        // Add your holiday check logic here
        return HOLIDAYS.contains(date);
    }

    // Data Models
    public static class SubjectModel {
        String code, name;
        int attendedHours, maxHours;
        float currentPct;

        public SubjectModel(String code, String name, int attendedHours, int maxHours) {
            this.code = code;
            this.name = name;
            this.attendedHours = attendedHours;
            this.maxHours = maxHours;
            this.currentPct = (maxHours > 0) ? ((float)attendedHours/maxHours)*100 : 0;
        }
    }

    public static class SubjectForecast {
        String name, code;
        float currentPct;
        int futureClasses, skipsLeft;
        boolean isSafe;

        public SubjectForecast(String name, String code, float currentPct, int futureClasses, int skipsLeft) {
            this.name = name;
            this.code = code;
            this.currentPct = currentPct;
            this.futureClasses = futureClasses;
            this.skipsLeft = skipsLeft;
            this.isSafe = skipsLeft > 0;
        }
    }
}