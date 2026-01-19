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
    public static final LocalDate SEMESTER_END = LocalDate.of(2026, 5, 6);

    // --- 2. OFFICIAL HOLIDAYS (College Closed) ---
    public static final List<LocalDate> HOLIDAYS = new ArrayList<>();

    // --- 3. PLANNED LEAVES (College Open, You Absent) ---
    public static List<PlannedLeave> plannedLeaves = new ArrayList<>();

    static {
        // Timetable Initialization
        TIMETABLE.put(DayOfWeek.MONDAY, Arrays.asList("21CSC206T", "21CSC204J", "21CSC205P", "21MAB204T", "21CSE251T", "21PDH209T"));
        TIMETABLE.put(DayOfWeek.TUESDAY, Arrays.asList("21MAB204T", "21CSC205P", "21CSC206T", "21CSC204J", "21CSE251T", "21PDH209T"));
        TIMETABLE.put(DayOfWeek.WEDNESDAY, Arrays.asList("21CSC205P", "21MAB204T", "21CSE251T", "21CSC206T"));
        TIMETABLE.put(DayOfWeek.THURSDAY, Arrays.asList("21CSC205P", "21CSC206T", "21CSC204J", "21CSE251T", "21MAB204T"));
        TIMETABLE.put(DayOfWeek.FRIDAY, Arrays.asList("21CSC206T", "21MAB204T", "21LEM202T", "21CSC204J"));

        // Mock Holidays (Example)
        HOLIDAYS.add(LocalDate.of(2026, 1, 26)); // Republic Day
        HOLIDAYS.add(LocalDate.of(2026, 3, 25)); // Holi
    }

    // --- 4. SUBJECT METADATA ---
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

    // --- HELPER: Is a specific date a booked leave? ---
    public static boolean isDateBooked(LocalDate date) {
        for (PlannedLeave l : plannedLeaves) {
            if (!date.isBefore(l.start) && !date.isAfter(l.end)) return true;
        }
        return false;
    }

    // --- METHOD 1: OFFICIAL SCHEDULE (The missing method!) ---
    // Counts classes that exist in the college schedule (ignores your personal leave)
    public static Map<String, Integer> getOfficialScheduleCounts(LocalDate start, LocalDate end) {
        Map<String, Integer> counts = new HashMap<>();
        if (start.isAfter(end)) return counts;

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            // Only skip if it's an OFFICIAL HOLIDAY. Count it even if you booked a leave.
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

    // --- METHOD 2: ACTUAL ATTENDANCE (Used for Time Machine) ---
    // Counts classes you can actually attend (ignores Holidays AND your Booked Leaves)
    public static Map<String, Integer> getClassCountsInRange(LocalDate start, LocalDate end) {
        Map<String, Integer> counts = new HashMap<>();
        if (start.isAfter(end)) return counts;

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            // Skip if Holiday OR Booked Leave
            if (!HOLIDAYS.contains(curr) && !isDateBooked(curr) && TIMETABLE.containsKey(curr.getDayOfWeek())) {
                List<String> classes = TIMETABLE.get(curr.getDayOfWeek());
                for (String code : classes) {
                    counts.put(code, counts.getOrDefault(code, 0) + 1);
                }
            }
            curr = curr.plusDays(1);
        }
        return counts;
    }

    // --- DATA MODELS ---
    public static class Subject implements java.io.Serializable {
        public String code;
        public String name;
        public int attended;
        public int total;

        public Subject(String code, String name, int attended, int total) {
            this.code = code;
            this.name = name;
            this.attended = attended;
            this.total = total;
        }

        public float getPercentage() {
            return total == 0 ? 0 : ((float) attended / total) * 100;
        }
    }

    public static class PlannedLeave {
        public LocalDate start;
        public LocalDate end;
        public String reason;

        public PlannedLeave(LocalDate start, LocalDate end, String reason) {
            this.start = start;
            this.end = end;
            this.reason = reason;
        }
    }
}