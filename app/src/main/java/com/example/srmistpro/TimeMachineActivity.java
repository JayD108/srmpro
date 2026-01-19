package com.example.srmistpro;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeMachineActivity extends AppCompatActivity {

    private List<AppData.Subject> subjects;
    private LocalDate simStart, simEnd;
    private int targetPct = 75;

    // UI Elements
    private Button btnStart, btnEnd, btnBook;
    private TextView lblTarget, txtBookSummary;
    private LinearLayout resultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_machine);

        // Load Data
        String json = getIntent().getStringExtra("data");
        subjects = new Gson().fromJson(json, new TypeToken<List<AppData.Subject>>(){}.getType());

        // Init UI
        btnStart = findViewById(R.id.btnStartDate);
        btnEnd = findViewById(R.id.btnEndDate);
        btnBook = findViewById(R.id.btnBookLeave);
        lblTarget = findViewById(R.id.lblTarget);
        txtBookSummary = findViewById(R.id.txtBookedSummary);
        SeekBar seekBar = findViewById(R.id.seekTarget);
        resultsContainer = findViewById(R.id.resultsContainer);

        // Default Dates
        simStart = LocalDate.now().plusDays(1);
        simEnd = LocalDate.now().plusDays(7);
        updateDateLabels();

        // --- LISTENERS ---
        btnStart.setOnClickListener(v -> pickDate(true));
        btnEnd.setOnClickListener(v -> pickDate(false));
        btnBook.setOnClickListener(v -> showBookingDialog());

        findViewById(R.id.btnRunSim).setOnClickListener(v -> runSimulation());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                targetPct = p;
                lblTarget.setText("Target: " + p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    // --- BOOKING DIALOG ---
    private void showBookingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Planned Leave");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final TextView tvInfo = new TextView(this);
        tvInfo.setText("Select dates you plan to miss (e.g. Vacation).");
        layout.addView(tvInfo);

        // Simple Inputs for Start/End (Using Buttons in a real app, simplified here)
        final EditText etReason = new EditText(this);
        etReason.setHint("Reason (e.g. Wedding)");
        layout.addView(etReason);

        builder.setView(layout);

        // Note: For simplicity, this dialog assumes picking dates via a secondary logic or just "Next 2 days".
        // To keep code concise, I will launch a Double Date Picker here.
        builder.setPositiveButton("Select Dates", (dialog, which) -> {
            // Launch Date Picker for Start
            DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
                LocalDate s = LocalDate.of(y, m+1, d);
                // Immediately launch End Date Picker
                new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                    LocalDate e = LocalDate.of(y2, m2+1, d2);
                    String r = etReason.getText().toString();

                    AppData.plannedLeaves.add(new AppData.PlannedLeave(s, e, r.isEmpty() ? "Planned" : r));
                    updateBookingSummary();

                }, s.getYear(), s.getMonthValue()-1, s.getDayOfMonth()).show();
            }, LocalDate.now().getYear(), LocalDate.now().getMonthValue()-1, LocalDate.now().getDayOfMonth());
            dpd.setMessage("Select Start Date");
            dpd.show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateBookingSummary() {
        if (AppData.plannedLeaves.isEmpty()) {
            txtBookSummary.setText("No leaves booked.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (AppData.PlannedLeave l : AppData.plannedLeaves) {
                sb.append("• ").append(l.start).append(" to ").append(l.end).append(" (").append(l.reason).append(")\n");
            }
            txtBookSummary.setText(sb.toString().trim());
        }
    }

    // --- SIMULATION LOGIC ---
    private void runSimulation() {
        resultsContainer.removeAllViews();
        LocalDate today = LocalDate.now();

        // We iterate from Tomorrow until Simulation End
        // Phase 1: Growth (Tomorrow -> SimStart-1) -> We ATTEND these (unless booked or holiday)
        // Phase 2: Simulation (SimStart -> SimEnd) -> We MISS these (unless holiday)

        Map<String, Integer> growthAttended = new HashMap<>();
        Map<String, Integer> bookedMissed = new HashMap<>();
        Map<String, Integer> simMissed = new HashMap<>();
        Map<String, Integer> holidaySaved = new HashMap<>();

        LocalDate curr = today.plusDays(1);

        while (!curr.isAfter(simEnd)) {
            // Check if valid class day
            if (AppData.TIMETABLE.containsKey(curr.getDayOfWeek())) {
                List<String> classes = AppData.TIMETABLE.get(curr.getDayOfWeek());

                boolean isHoliday = AppData.HOLIDAYS.contains(curr);
                boolean isBooked = AppData.isDateBooked(curr);
                boolean isSimRange = !curr.isBefore(simStart) && !curr.isAfter(simEnd);

                for (String code : classes) {
                    if (isHoliday) {
                        holidaySaved.put(code, holidaySaved.getOrDefault(code, 0) + 1);
                    } else if (isSimRange) {
                        // In Simulation Range -> We Miss
                        simMissed.put(code, simMissed.getOrDefault(code, 0) + 1);
                    } else if (isBooked) {
                        // Booked Leave -> We Miss
                        bookedMissed.put(code, bookedMissed.getOrDefault(code, 0) + 1);
                    } else {
                        // Normal Day -> We Attend
                        growthAttended.put(code, growthAttended.getOrDefault(code, 0) + 1);
                    }
                }
            }
            curr = curr.plusDays(1);
        }

        // GENERATE CARDS
        for (AppData.Subject s : subjects) {
            int gVal = growthAttended.getOrDefault(s.code, 0);
            int bVal = bookedMissed.getOrDefault(s.code, 0);
            int sVal = simMissed.getOrDefault(s.code, 0);
            int hVal = holidaySaved.getOrDefault(s.code, 0);

            // Calculate New Stats
            int newAttended = s.attended + gVal;
            // Total increases by everything except holidays (assuming holidays don't count to total classes held)
            int newTotal = s.total + gVal + bVal + sVal;

            float newPct = (newTotal == 0) ? 0 : ((float) newAttended / newTotal) * 100;
            int alreadyMissed = s.total - s.attended;

            // --- UI CARD CREATION ---
            addResultCard(s, newPct, alreadyMissed, hVal, sVal + bVal);
        }
    }

    private void addResultCard(AppData.Subject s, float projPct, int alreadyMissed, int holidays, int totalLeaveMissed) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E1E"));
        card.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 20);
        card.setLayoutParams(lp);

        // Header: Code + Name
        TextView tvHeader = new TextView(this);
        tvHeader.setText(s.code + " : " + s.name); // Using Name from Image
        tvHeader.setTextSize(16f);
        tvHeader.setTextColor(Color.WHITE);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

        // Body: Details
        TextView tvBody = new TextView(this);
        String status = projPct >= targetPct ? "✅" : "❌";

        String sb = String.format(
                "Current: %.1f%%\n" +
                        "Missed Already: %d classes\n" +
                        "Official Holidays: %d\n" +
                        "Classes in this Leave: %d\n" +
                        "Projected: %.1f%%  %s",
                s.getPercentage(), alreadyMissed, holidays, totalLeaveMissed, projPct, status
        );

        tvBody.setText(sb);
        tvBody.setTextColor(Color.LTGRAY);
        tvBody.setTextSize(14f);
        tvBody.setPadding(0, 10, 0, 0);

        card.addView(tvHeader);
        card.addView(tvBody);
        resultsContainer.addView(card);
    }

    private void pickDate(boolean isStart) {
        LocalDate d = isStart ? simStart : simEnd;
        new DatePickerDialog(this, (v, y, m, day) -> {
            LocalDate nd = LocalDate.of(y, m + 1, day);
            if (isStart) simStart = nd; else simEnd = nd;
            updateDateLabels();
        }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
    }

    private void updateDateLabels() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM");
        btnStart.setText("Start: " + simStart.format(f));
        btnEnd.setText("End: " + simEnd.format(f));
    }
}