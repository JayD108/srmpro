package com.example.srmistpro;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.srmistpro.AppData;
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
    private Button btnStart, btnEnd;
    private TextView lblTarget, rocketView;
    private LinearLayout resultsContainer, layoutBookedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_machine);

        String json = getIntent().getStringExtra("data");
        if (json != null) {
            subjects = new Gson().fromJson(json, new TypeToken<List<AppData.Subject>>(){}.getType());
        }

        btnStart = findViewById(R.id.btnStartDate);
        btnEnd = findViewById(R.id.btnEndDate);
        lblTarget = findViewById(R.id.lblTarget);
        layoutBookedList = findViewById(R.id.layoutBookedList);
        resultsContainer = findViewById(R.id.resultsContainer);
        rocketView = findViewById(R.id.rocketView);
        SeekBar seekBar = findViewById(R.id.seekTarget);

        rocketView.setRotation(-45f);

        // Default Simulation Range
        simStart = LocalDate.now().plusDays(1);
        simEnd = LocalDate.now().plusDays(7);
        updateDateLabels();
        refreshBookedList();

        btnStart.setOnClickListener(v -> pickDate(true));
        btnEnd.setOnClickListener(v -> pickDate(false));
        findViewById(R.id.btnBookLeave).setOnClickListener(v -> showBookingDialog());
        findViewById(R.id.btnRunSim).setOnClickListener(v -> launchRocketAnimation());

        // --- INFO BUTTON LOGIC ---
        findViewById(R.id.btnInfo).setOnClickListener(v -> showInfoDialog());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                targetPct = p;
                lblTarget.setText("Safe: " + p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("How Calculation Works 🧮")
                .setMessage("1. We start with your current attendance.\n\n" +
                        "2. We project classes from Tomorrow until your chosen End Date.\n\n" +
                        "3. ASSUMPTION: You attend ALL future classes, EXCEPT:\n" +
                        "   - Dates you explicitly marked as 'Planned Leaves'.\n" +
                        "   - Dates inside the Simulation Range (if checking 'what if I miss this week').\n\n" +
                        "4. HOLIDAYS: If a Planned Leave falls on a College Holiday (e.g. Holi), it is NOT counted as missed. It is 'Saved by Holiday'.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void launchRocketAnimation() {
        rocketView.setTranslationY(0f);
        rocketView.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();

        float screenHeight = getResources().getDisplayMetrics().heightPixels;
        ObjectAnimator animator = ObjectAnimator.ofFloat(rocketView, "translationY", 0f, -screenHeight);
        animator.setDuration(600);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rocketView.setVisibility(View.GONE);
                runSimulation();
            }
        });
        animator.start();
    }

    private void showBookingDialog() {
        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            LocalDate s = LocalDate.of(y, m+1, d);
            DatePickerDialog dpd2 = new DatePickerDialog(this, (view2, y2, m2, d2) -> {
                LocalDate e = LocalDate.of(y2, m2+1, d2);
                if(e.isBefore(s)) {
                    Toast.makeText(this, "Invalid Date Range", Toast.LENGTH_SHORT).show();
                    return;
                }
                AppData.plannedLeaves.add(new AppData.PlannedLeave(s, e, "Planned"));
                refreshBookedList();
            }, s.getYear(), s.getMonthValue()-1, s.getDayOfMonth());
            dpd2.setTitle("Select End Date");
            dpd2.show();
        }, LocalDate.now().getYear(), LocalDate.now().getMonthValue()-1, LocalDate.now().getDayOfMonth());
        dpd.setTitle("Select Start Date");
        dpd.show();
    }

    private void refreshBookedList() {
        layoutBookedList.removeAllViews();
        if (AppData.plannedLeaves.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No leaves added yet.");
            tv.setTextColor(Color.parseColor("#88FFFFFF"));
            layoutBookedList.addView(tv);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        for (int i = 0; i < AppData.plannedLeaves.size(); i++) {
            AppData.PlannedLeave l = AppData.plannedLeaves.get(i);
            TextView tv = new TextView(this);
            tv.setText("📅 " + l.start.format(fmt) + " - " + l.end.format(fmt));
            tv.setTextColor(Color.parseColor("#FFAB91"));
            tv.setBackgroundColor(Color.parseColor("#33FFFFFF"));
            tv.setPadding(30, 20, 30, 20);
            tv.setTextSize(14f);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, 15);
            tv.setLayoutParams(lp);

            int finalI = i;
            tv.setOnClickListener(v -> {
                AppData.plannedLeaves.remove(finalI);
                refreshBookedList();
            });
            layoutBookedList.addView(tv);
        }
    }

    private void runSimulation() {
        if (simEnd.isBefore(simStart)) {
            Toast.makeText(this, "Check your dates!", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDate today = LocalDate.now();
        // Counter Buckets
        Map<String, Integer> futureAttended = new HashMap<>();
        Map<String, Integer> bookedMissed = new HashMap<>();
        Map<String, Integer> simMissed = new HashMap<>();
        Map<String, Integer> holidaySaved = new HashMap<>();

        // Loop from Tomorrow until End of Simulation Range (or Semester End if you prefer)
        // User requested: "Assumes I will do all classes... except these"
        // Since this is "Time Machine" (Simulation), we project until the END of the specific range selected.
        LocalDate curr = today.plusDays(1);

        while (!curr.isAfter(simEnd)) {
            // Check if timetable has classes for this day
            if (AppData.TIMETABLE.containsKey(curr.getDayOfWeek())) {
                List<String> classes = AppData.TIMETABLE.get(curr.getDayOfWeek());

                // PRIORITY 1: IS IT A HOLIDAY?
                if (AppData.HOLIDAYS.contains(curr)) {
                    // It's a holiday. Even if you booked a leave, you are saved.
                    // If you booked a leave, we count it as "Saved by Holiday"
                    if (AppData.isDateBooked(curr)) {
                        for (String code : classes) {
                            holidaySaved.put(code, holidaySaved.getOrDefault(code, 0) + 1);
                        }
                    }
                    // If not booked, it's just a normal holiday, no attendance impact.
                }
                // PRIORITY 2: IS IT BOOKED OR SIMULATED ABSENT?
                else {
                    boolean isBooked = AppData.isDateBooked(curr);
                    boolean isSimRange = !curr.isBefore(simStart) && !curr.isAfter(simEnd);

                    if (isBooked) {
                        for (String code : classes)
                            bookedMissed.put(code, bookedMissed.getOrDefault(code, 0) + 1);
                    } else if (isSimRange) {
                        // If checking "What if I miss this week?", count as Sim Missed
                        for (String code : classes)
                            simMissed.put(code, simMissed.getOrDefault(code, 0) + 1);
                    } else {
                        // PRIORITY 3: ASSUME ATTENDED
                        for (String code : classes)
                            futureAttended.put(code, futureAttended.getOrDefault(code, 0) + 1);
                    }
                }
            }
            curr = curr.plusDays(1);
        }

        // GENERATE CARDS
        for (AppData.Subject s : subjects) {
            int gVal = futureAttended.getOrDefault(s.code, 0);
            int bVal = bookedMissed.getOrDefault(s.code, 0);
            int sVal = simMissed.getOrDefault(s.code, 0);
            int hVal = holidaySaved.getOrDefault(s.code, 0);

            int alreadyMissed = s.total - s.attended;

            int newAttended = s.attended + gVal;
            // Total = Attended + (Booked Missed) + (Sim Missed)
            // Holidays do NOT add to total classes
            int newTotal = s.total + gVal + bVal + sVal;

            float newPct = (newTotal == 0) ? 0 : ((float) newAttended / newTotal) * 100;

            addResultCard(s, newPct, alreadyMissed, bVal + sVal, hVal);
        }
    }

    private void addResultCard(AppData.Subject s, float projPct, int alreadyMissed, int leaveMissing, int holidaySaved) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#26000000"));
        card.setPadding(40, 40, 40, 40);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 25);
        card.setLayoutParams(lp);

        TextView tvHeader = new TextView(this);
        tvHeader.setText(s.name);
        tvHeader.setTextSize(18f);
        tvHeader.setTextColor(Color.WHITE);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvStatus = new TextView(this);
        String emoji = projPct >= targetPct ? "✅ Safe" : "❌ Critical";
        int color = projPct >= targetPct ? Color.parseColor("#00E676") : Color.parseColor("#FF5252");

        tvStatus.setText(String.format("%.1f%%  %s", projPct, emoji));
        tvStatus.setTextColor(color);
        tvStatus.setTextSize(22f);
        tvStatus.setPadding(0, 5, 0, 20);

        TextView tvDetails = new TextView(this);
        StringBuilder sb = new StringBuilder();

        sb.append("• Already Missed: ").append(alreadyMissed).append("\n");
        sb.append("• Leave Missing: ").append(leaveMissing).append("\n");
        if (holidaySaved > 0) {
            sb.append("• Saved by Holiday: ").append(holidaySaved).append(" 🎉\n");
        }

        tvDetails.setText(sb.toString().trim());
        tvDetails.setTextColor(Color.parseColor("#BBBBBB"));
        tvDetails.setTextSize(13f);
        tvDetails.setLineSpacing(0, 1.3f);

        card.addView(tvHeader);
        card.addView(tvStatus);
        card.addView(tvDetails);
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