package com.example.srmistpro;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private List<AppData.Subject> subjects;
    private BarChart barChart;
    private PieChart pieChart;
    private TableLayout tableLayout;
    private TextView txtResult, lblResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        tableLayout = findViewById(R.id.tableLayout);
        txtResult = findViewById(R.id.txtTimeMachineResult);
        lblResult = findViewById(R.id.lblTimeMachine);

        String json = getIntent().getStringExtra("data");
        subjects = new Gson().fromJson(json, new TypeToken<List<AppData.Subject>>(){}.getType());

        setupDashboard();

        findViewById(R.id.btnNavDash).setOnClickListener(v -> resetToDashboard());

        // --- 1. OPEN TIME MACHINE ACTIVITY ---
        findViewById(R.id.btnNavTime).setOnClickListener(v -> {
            Intent i = new Intent(this, TimeMachineActivity.class);
            i.putExtra("data", new Gson().toJson(subjects));
            startActivity(i);
        });

        // --- 2. OPEN FORECAST DIALOG ---
        findViewById(R.id.btnNavSem).setOnClickListener(v -> showForecastDialog());
    }

    private void resetToDashboard() {
        lblResult.setVisibility(View.GONE);
        txtResult.setVisibility(View.GONE);
    }

    private void showForecastDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📅 Semester Forecast");

        // Create Slider UI inside Dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final TextView tvLabel = new TextView(this);
        tvLabel.setText("Required Attendance: 75%");
        tvLabel.setTextSize(16f);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(75);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvLabel.setText("Required Attendance: " + i + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(tvLabel);
        layout.addView(seekBar);
        builder.setView(layout);

        builder.setPositiveButton("Calculate", (dialog, which) -> {
            runSemForecast(seekBar.getProgress());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

// Paste this inside MainActivity.java, replacing the old runSemForecast method

    private void runSemForecast(int targetPct) {
        lblResult.setVisibility(View.VISIBLE);
        lblResult.setText("📅 Semester Forecast (Target: " + targetPct + "%)");
        txtResult.setVisibility(View.VISIBLE);

        LocalDate today = LocalDate.now();

        // 1. Get Official Future Classes (Excludes Holidays, Includes your Leaves)
        // This is needed to know the TOTAL classes that will be held.
        Map<String, Integer> futureOfficial = AppData.getOfficialScheduleCounts(today.plusDays(1), AppData.SEMESTER_END);

        StringBuilder sb = new StringBuilder();

        for(AppData.Subject s : subjects) {
            int futureClassesCount = futureOfficial.getOrDefault(s.code, 0);

            // GRAND TOTAL = Current Total + Future Official
            int grandTotal = s.total + futureClassesCount;

            // CALCULATE MAX BUNKS
            // Formula: Max Absent = GrandTotal * (100 - Target)%
            int maxAbsentAllowed = (int) (grandTotal * (1.0 - (targetPct / 100.0)));

            // Current Absent
            int currentAbsent = s.total - s.attended;

            // SKIPS LEFT = Max Allowed - Already Missed
            int skipsLeft = maxAbsentAllowed - currentAbsent;

            // Build Output
            sb.append(String.format("📘 %s\n", s.code));
            sb.append(String.format("   Total Sem Classes: %d\n", grandTotal)); // <--- REQUESTED FEATURE
            sb.append(String.format("   Max Bunks Allowed: %d\n", maxAbsentAllowed));

            if (skipsLeft > 0) {
                sb.append(String.format("   ✅ Safe! Can skip %d more.\n\n", skipsLeft));
            } else {
                int mustAttend = (currentAbsent - maxAbsentAllowed);
                // Simplified logic: If negative skips, you are in trouble.
                sb.append(String.format("   ❌ CRITICAL! Over limit by %d classes.\n\n", Math.abs(skipsLeft)));
            }
        }
        txtResult.setText(sb.toString());
    }
    private void setupDashboard() {
        // ... (Same as previous response, copy-paste the chart setup here) ...
        // Re-paste the chart code from the previous Main Activity snippet I gave you
        // to keep this file complete.
        ArrayList<BarEntry> entries = new ArrayList<>();
        int totalAttended = 0, totalClasses = 0;
        addTableRow("Code", "Att.", "Max", "%", true);

        for (int i = 0; i < subjects.size(); i++) {
            AppData.Subject s = subjects.get(i);
            entries.add(new BarEntry(i, s.getPercentage()));
            totalAttended += s.attended;
            totalClasses += s.total;
            addTableRow(s.code, String.valueOf(s.attended), String.valueOf(s.total), String.format("%.1f", s.getPercentage()), false);
        }

        BarDataSet barSet = new BarDataSet(entries, "Attendance %");
        barSet.setColor(Color.parseColor("#5C6BC0"));
        barSet.setValueTextColor(Color.BLACK);
        BarData barData = new BarData(barSet);
        barChart.setData(barData);
        barChart.invalidate();

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(totalAttended, "Present"));
        pieEntries.add(new PieEntry(totalClasses - totalAttended, "Absent"));
        PieDataSet pieSet = new PieDataSet(pieEntries, "");
        pieSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"));
        PieData pieData = new PieData(pieSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void addTableRow(String c1, String c2, String c3, String c4, boolean isHeader) {
        TableRow row = new TableRow(this);
        row.setPadding(10, 20, 10, 20);
        if(isHeader) row.setBackgroundColor(Color.LTGRAY);
        row.addView(createTextView(c1, isHeader));
        row.addView(createTextView(c2, isHeader));
        row.addView(createTextView(c3, isHeader));
        row.addView(createTextView(c4 + (isHeader ? "" : "%"), isHeader));
        tableLayout.addView(row);
    }

    private TextView createTextView(String text, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 0, 8, 0);
        tv.setTextColor(Color.BLACK);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }
}