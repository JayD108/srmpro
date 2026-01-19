package com.example.srmistpro;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppData {

    // --- 1. TIMETABLE ---
    public static Map<DayOfWeek, List<String>> TIMETABLE = new HashMap<>();

    // Semester Ends: May 6, 2026 (From your config)
    public static final LocalDate SEMESTER_END = LocalDate.of(2026, 5, 6);

    // --- 2. OFFICIAL ACADEMIC HOLIDAYS (2026) ---
    public static final List<LocalDate> HOLIDAYS = new ArrayList<>();

    // --- 3. YOUR PLANNED LEAVES ---
    public static List<PlannedLeave> plannedLeaves = new ArrayList<>();

    static {
        // --- TIMETABLE LOAD (From your offline_data.py) ---
        TIMETABLE.put(DayOfWeek.MONDAY, Arrays.asList("21CSC206T", "21CSC204J", "21CSC205P", "21MAB204T", "21CSE251T", "21PDH209T"));
        TIMETABLE.put(DayOfWeek.TUESDAY, Arrays.asList("21MAB204T", "21CSC205P", "21CSC206T", "21CSC204J", "21CSE251T", "21PDH209T"));
        TIMETABLE.put(DayOfWeek.WEDNESDAY, Arrays.asList("21CSC205P", "21MAB204T", "21CSE251T", "21CSC206T")); // Removed 'PRP' as it's usually placement
        TIMETABLE.put(DayOfWeek.THURSDAY, Arrays.asList("21CSC205P", "21CSC205P", "21CSC206T", "21CSC204J", "21CSE251T", "21MAB204T"));
        TIMETABLE.put(DayOfWeek.FRIDAY, Arrays.asList("21CSC206T", "21MAB204T", "21LEM202T", "21LEM202T", "21CSC204J", "21CSC204J"));

        // --- HOLIDAY LIST (Exact matches from your Python file) ---
        HOLIDAYS.add(LocalDate.of(2026, 1, 1));  // New Year
        HOLIDAYS.add(LocalDate.of(2026, 1, 14)); // Pongal
        HOLIDAYS.add(LocalDate.of(2026, 1, 26)); // Republic Day
        HOLIDAYS.add(LocalDate.of(2026, 2, 15)); // Mahashivratri
        HOLIDAYS.add(LocalDate.of(2026, 3, 2));  // Holi Day 1
        HOLIDAYS.add(LocalDate.of(2026, 3, 3));  // Holi Day 2
        HOLIDAYS.add(LocalDate.of(2026, 3, 4));  // Holi Day 3
        HOLIDAYS.add(LocalDate.of(2026, 3, 21)); // Id-Ul-Fitr
        HOLIDAYS.add(LocalDate.of(2026, 3, 26)); // Ram Navami
        HOLIDAYS.add(LocalDate.of(2026, 3, 31)); // Mahavir Jayanti
        HOLIDAYS.add(LocalDate.of(2026, 4, 3));  // Good Friday
        HOLIDAYS.add(LocalDate.of(2026, 4, 14)); // Ambedkar Jayanti
        HOLIDAYS.add(LocalDate.of(2026, 5, 1));  // Buddha Poornima
    }

    public static List<Subject> getInitialSubjects() {
        List<Subject> list = new ArrayList<>();
        list.add(new Subject("21CSC204J", "Design & Analysis of Algo", 0, 0));
        list.add(new Subject("21CSC205P", "DBMS", 0, 0));
        list.add(new Subject("21CSC206T", "Artificial Intelligence", 0, 0));
        list.add(new Subject("21CSE251T", "Digital Image Processing", 0, 0));
        list.add(new Subject("21LEM202T", "Universal Human Values", 0, 0));
        list.add(new Subject("21MAB204T", "Prob. & Queueing Theory", 0, 0));
        list.add(new Subject("21PDH209T", "Social Engineering", 0, 0));
        return list;
    }

    public static boolean isDateBooked(LocalDate date) {
        for (PlannedLeave l : plannedLeaves) {
            if (!date.isBefore(l.start) && !date.isAfter(l.end)) return true;
        }
        return false;
    }

    // --- OFFICIAL SCHEDULE COUNTER ---
    public static Map<String, Integer> getOfficialScheduleCounts(LocalDate start, LocalDate end) {
        Map<String, Integer> counts = new HashMap<>();
        if (start.isAfter(end)) return counts;

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            // Priority: Holidays > Timetable
            if (!HOLIDAYS.contains(curr) && TIMETABLE.containsKey(curr.getDayOfWeek())) {
                List<String> classes = TIMETABLE.get(curr.getDayOfWeek());
                for (String code : classes) {
                    counts.put(code, counts.getOrDefault(code, 0) + 1);
                }
            }
            curr = curr.plusDays(1);
        }
        return counts;
    }

    public static class Subject implements java.io.Serializable {
        public String code, name;
        public int attended, total;
        public Subject(String code, String name, int attended, int total) {
            this.code = code; this.name = name; this.attended = attended; this.total = total;
        }
        public float getPercentage() { return total == 0 ? 0 : ((float) attended / total) * 100; }
    }

    public static class PlannedLeave {
        public LocalDate start, end;
        public String reason;
        public PlannedLeave(LocalDate start, LocalDate end, String reason) {
            this.start = start; this.end = end; this.reason = reason;
        }
    }
}