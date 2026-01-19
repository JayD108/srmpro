package com.example.srmistpro;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class SemForecastActivity extends AppCompatActivity {

    private List<AppData.Subject> subjects;
    private LinearLayout resultsContainer;
    private TextView lblTarget;
    private int targetPct = 75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sem_forecast);

        // 1. Load Data
        String json = getIntent().getStringExtra("data");
        if (json != null) {
            subjects = new Gson().fromJson(json, new TypeToken<List<AppData.Subject>>(){}.getType());
        } else {
            subjects = new ArrayList<>();
        }

        // 2. Initialize Views
        resultsContainer = findViewById(R.id.resultsContainer);
        lblTarget = findViewById(R.id.lblTarget);
        SeekBar seekBar = findViewById(R.id.seekTarget);
        Button btnSchedule = findViewById(R.id.btnSubjectSchedule);

        // 3. Initial Run
        runForecast();

        // 4. Target Slider Listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                targetPct = p;
                lblTarget.setText("Required Goal: " + p + "%");
                runForecast(); // Update cards in real-time
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // 5. Subject Schedule Button Listener
        btnSchedule.setOnClickListener(v -> showSubjectSelectionDialog());
    }

    // --- CORE FORECAST LOGIC ---
    private void runForecast() {
        resultsContainer.removeAllViews();
        LocalDate today = LocalDate.now();

        // Get count of all future official classes (excluding holidays)
        Map<String, Integer> futureOfficial = AppData.getOfficialScheduleCounts(today.plusDays(1), AppData.SEMESTER_END);

        for (AppData.Subject s : subjects) {
            int futureClasses = futureOfficial.getOrDefault(s.code, 0);
            int grandTotal = s.total + futureClasses;

            // Formula: Max Absent = Total * (100 - Target)%
            int maxBunksAllowed = (int) (grandTotal * (1.0 - (targetPct / 100.0)));
            int currentAbsent = s.total - s.attended;
            int skipsLeft = maxBunksAllowed - currentAbsent;

            addSubjectCard(s, grandTotal, skipsLeft);
        }
    }

    // --- UI: FORECAST CARD GENERATOR ---
    private void addSubjectCard(AppData.Subject s, int grandTotal, int skipsLeft) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        // Use the Glass Background Drawable
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_glass_card));
        card.setPadding(40, 30, 40, 30);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 20);
        card.setLayoutParams(lp);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(s.name);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        // Status Message
        TextView tvStatus = new TextView(this);
        String msg;
        int color;

        if (skipsLeft > 0) {
            msg = "✅ Safe! Can skip " + skipsLeft + " more.";
            color = Color.parseColor("#00E676"); // Neon Green
        } else if (skipsLeft == 0) {
            msg = "⚠️ Borderline! No skips left.";
            color = Color.parseColor("#FFD740"); // Amber
        } else {
            msg = "❌ Danger! Over limit by " + Math.abs(skipsLeft);
            color = Color.parseColor("#FF5252"); // Neon Red
        }

        tvStatus.setText(msg);
        tvStatus.setTextColor(color);
        tvStatus.setTextSize(14f);
        tvStatus.setPadding(0, 10, 0, 0);

        // Footer Stats
        TextView tvStats = new TextView(this);
        tvStats.setText("Total Sem Classes: " + grandTotal);
        tvStats.setTextColor(Color.parseColor("#AAAAAA")); // Grey
        tvStats.setTextSize(12f);
        tvStats.setPadding(0, 8, 0, 0);

        card.addView(tvTitle);
        card.addView(tvStatus);
        card.addView(tvStats);

        resultsContainer.addView(card);
    }

    // --- NEW FEATURE: SUBJECT SCHEDULE SELECTION ---
    private void showSubjectSelectionDialog() {
        String[] subjectNames = new String[subjects.size()];
        for(int i=0; i<subjects.size(); i++) {
            subjectNames[i] = subjects.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setCustomTitle(createCustomTitle("Select Subject"))
                .setItems(subjectNames, (dialog, which) -> {
                    AppData.Subject selected = subjects.get(which);
                    showDateAnalysis(selected);
                })
                .show();
    }

    // --- ADVANCED SCHEDULE ANALYZER (Month-wise + Totals) ---
// --- FIXED SCHEDULE VIEW (Counts Double Slots Correctly) ---
    private void showDateAnalysis(AppData.Subject subject) {
        // 1. Prepare Data Containers
        java.util.LinkedHashMap<String, List<String>> monthGroups = new java.util.LinkedHashMap<>();
        int totalClasses = 0;
        int totalHolidays = 0;

        LocalDate curr = LocalDate.now().plusDays(1);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE, dd");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");

        // 2. Calculation Loop
        while (!curr.isAfter(AppData.SEMESTER_END)) {
            if (AppData.TIMETABLE.containsKey(curr.getDayOfWeek())) {
                List<String> dailyClasses = AppData.TIMETABLE.get(curr.getDayOfWeek());

                // FIX: Count HOW MANY times the subject appears (e.g. 2 for Lab)
                int slotsToday = 0;
                for (String code : dailyClasses) {
                    if (code.equals(subject.code)) {
                        slotsToday++;
                    }
                }

                if (slotsToday > 0) {
                    String monthKey = curr.format(monthFmt);
                    if (!monthGroups.containsKey(monthKey)) {
                        monthGroups.put(monthKey, new ArrayList<>());
                    }

                    String dateStr;
                    if (AppData.HOLIDAYS.contains(curr)) {
                        // If holiday, we save ALL slots for that day
                        dateStr = "🎉 " + curr.format(dayFmt) + " (Holiday)";
                        totalHolidays += slotsToday;
                    } else {
                        // Add suffix if multiple slots (e.g. "Fri, 24 (2 slots)")
                        String suffix = (slotsToday > 1) ? " (" + slotsToday + " slots)" : "";
                        dateStr = "📅 " + curr.format(dayFmt) + suffix;
                        totalClasses += slotsToday; // Add ALL slots to total
                    }
                    monthGroups.get(monthKey).add(dateStr);
                }
            }
            curr = curr.plusDays(1);
        }

        // 3. Build Custom View (Theme: Deep Space)
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 20, 40, 40);
        mainLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Iterate through Months
        for (Map.Entry<String, List<String>> entry : monthGroups.entrySet()) {
            // Month Header
            TextView header = new TextView(this);
            header.setText(entry.getKey());
            header.setTextColor(Color.parseColor("#00E5FF")); // Cyan
            header.setTextSize(16f);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setPadding(0, 30, 0, 10);
            mainLayout.addView(header);

            // Date Items
            for (String dateItem : entry.getValue()) {
                TextView item = new TextView(this);
                item.setText(dateItem);
                item.setTextSize(14f);
                item.setPadding(20, 5, 0, 5);

                if (dateItem.contains("Holiday")) {
                    item.setTextColor(Color.parseColor("#FFAB91"));
                } else {
                    item.setTextColor(Color.parseColor("#E0E0E0"));
                }
                mainLayout.addView(item);
            }

            // Divider
            android.view.View divider = new android.view.View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(-1, 2));
            divider.setBackgroundColor(Color.parseColor("#22FFFFFF"));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) divider.getLayoutParams();
            lp.setMargins(0, 15, 0, 0);
            divider.setLayoutParams(lp);
            mainLayout.addView(divider);
        }

        // 4. Footer Summary (Matches Forecast Card Now)
        TextView footer = new TextView(this);
        footer.setText("\nTOTAL SUMMARY");
        footer.setTextColor(Color.WHITE);
        footer.setTextSize(16f);
        footer.setTypeface(null, android.graphics.Typeface.BOLD);
        footer.setGravity(android.view.Gravity.CENTER);
        footer.setPadding(0, 30, 0, 5);
        mainLayout.addView(footer);

        TextView stats = new TextView(this);
        // This total will now match the card (e.g., 53 -> 66)
        stats.setText("Classes: " + totalClasses + " | Holidays: " + totalHolidays);
        stats.setTextColor(Color.parseColor("#D500F9"));
        stats.setTextSize(15f);
        stats.setGravity(android.view.Gravity.CENTER);
        mainLayout.addView(stats);

        // 5. Scroll Wrapper
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(mainLayout);

        // 6. Show Dialog
        new AlertDialog.Builder(this)
                .setCustomTitle(createCustomTitle(subject.code))
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    // Helper for Styled Dialog Titles
    private TextView createCustomTitle(String title) {
        TextView tv = new TextView(this);
        tv.setText("Schedule: " + title);
        tv.setPadding(50, 40, 50, 20);
        tv.setTextSize(20f);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#1E1E1E"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }
}