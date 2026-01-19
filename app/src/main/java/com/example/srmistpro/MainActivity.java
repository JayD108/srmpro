package com.example.srmistpro;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<AppData.Subject> subjects;
    private BarChart barChart;
    private PieChart pieChart;
    private TableLayout tableLayout;

    // Default Target for Status Calculation
    private final int currentTargetPct = 75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        tableLayout = findViewById(R.id.tableLayout);

        // 2. Load Data passed from Input/Login Activity
        String json = getIntent().getStringExtra("data");
        if (json != null) {
            subjects = new Gson().fromJson(json, new TypeToken<List<AppData.Subject>>(){}.getType());
        } else {
            subjects = new ArrayList<>();
        }

        // 3. Render Dashboard
        setupDashboard();

        // 4. Bottom Navigation Listeners

        // Dashboard Button (Already Here)
        findViewById(R.id.btnNavDash).setOnClickListener(v ->
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
        );

        // Time Machine Button
        findViewById(R.id.btnNavTime).setOnClickListener(v -> {
            Intent i = new Intent(this, TimeMachineActivity.class);
            i.putExtra("data", new Gson().toJson(subjects));
            startActivity(i);
        });

        // Forecast Button
        findViewById(R.id.btnNavSem).setOnClickListener(v -> {
            Intent i = new Intent(this, SemForecastActivity.class);
            i.putExtra("data", new Gson().toJson(subjects));
            startActivity(i);
        });
        // --- INFO BUTTON ---
        findViewById(R.id.btnAppInfo).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Bunk & Chutti")
                    .setMessage("Created by: Mrityunjay\n\nDaba ke karo bunk")
                    .setPositiveButton("Awesome", null)
                    .show();
        });
    }

    private void setupDashboard() {
        tableLayout.removeAllViews();
        barChart.clear();
        pieChart.clear();

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<PieEntry> pieEntries = new ArrayList<>();

        int grandTotalAttended = 0;
        int grandTotalClasses = 0;

        // --- TABLE HEADER ---
        // Columns: Subject | Att | Abs | Tot | % | Stat
        addTableRow("Subject", "Att", "Abs", "Tot", "%", "Stat", true);

        for (int i = 0; i < subjects.size(); i++) {
            AppData.Subject s = subjects.get(i);
            int missed = s.total - s.attended;

            grandTotalAttended += s.attended;
            grandTotalClasses += s.total;

            // Chart Data
            entries.add(new BarEntry(i, s.getPercentage()));

            // Abbreviate name for Chart Labels (e.g., "Artificial..." -> "Artif...")
            String shortName = s.name.length() > 8 ? s.name.substring(0, 8) + ".." : s.name;
            labels.add(shortName);

            // --- SMART STATUS CALCULATION ---
            // Formula to determine + (Bunks Left) or - (Classes Needed)
            String statusStr;
            float targetRatio = currentTargetPct / 100.0f;

            // Logic: How many can I skip and stay >= 75%?
            int maxTotal = (int) (s.attended / targetRatio);
            int canSkip = maxTotal - s.total;

            // Logic: How many must I attend to reach 75%?
            int mustAttend = (int) Math.ceil((targetRatio * s.total - s.attended) / (1.0 - targetRatio));

            if (s.getPercentage() >= currentTargetPct) {
                statusStr = "+" + canSkip; // Green Positive
            } else {
                statusStr = "-" + (mustAttend > 0 ? mustAttend : 0); // Red Negative
            }

            // --- ADD ROW ---
            String subjectDisplay = s.name + "\n(" + s.code + ")";
            addTableRow(subjectDisplay,
                    String.valueOf(s.attended),
                    String.valueOf(missed),
                    String.valueOf(s.total),
                    String.format("%.0f%%", s.getPercentage()),
                    statusStr,
                    false);
        }

        // Setup Charts
        setupCharts(grandTotalAttended, grandTotalClasses, entries, labels, pieEntries);
    }

    private void addTableRow(String c1, String c2, String c3, String c4, String c5, String c6, boolean isHeader) {
        TableRow row = new TableRow(this);
        // Padding for the row
        row.setPadding(0, 25, 0, 25);

        // Transparent background for Glass Effect
        row.setBackgroundColor(Color.TRANSPARENT);

        // Column 1: Subject Name (Weight 2)
        row.addView(createTextView(c1, isHeader, 2f, true));
        // Columns 2-4: Stats (Weight 0.7)
        row.addView(createTextView(c2, isHeader, 0.7f, false));
        row.addView(createTextView(c3, isHeader, 0.7f, false));
        row.addView(createTextView(c4, isHeader, 0.7f, false));

        // Column 5: Percentage
        TextView tvPct = createTextView(c5, isHeader, 1.0f, false);
        if(!isHeader) {
            float val = Float.parseFloat(c5.replace("%", ""));
            // Red if danger, Green if safe
            tvPct.setTextColor(val < currentTargetPct ? Color.parseColor("#FF5252") : Color.parseColor("#69F0AE"));
            tvPct.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        row.addView(tvPct);

        // Column 6: Status (+5 / -3)
        TextView tvStat = createTextView(c6, isHeader, 0.9f, false);
        if(!isHeader) {
            if (c6.startsWith("+")) {
                tvStat.setTextColor(Color.parseColor("#69F0AE")); // Neon Green
            } else {
                tvStat.setTextColor(Color.parseColor("#FF5252")); // Neon Red
            }
            tvStat.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        row.addView(tvStat);

        tableLayout.addView(row);

        // Add a thin separator line if not header
        if(isHeader) {
            // Header already has a line in XML
        }
    }

    private TextView createTextView(String text, boolean isHeader, float weight, boolean alignLeft) {
        TextView tv = new TextView(this);
        tv.setText(text);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);

        tv.setPadding(5, 0, 5, 0);
        tv.setGravity(alignLeft ? android.view.Gravity.START : android.view.Gravity.CENTER);

        if (isHeader) {
            tv.setTextColor(Color.WHITE); // White Header
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextSize(11f);
        } else {
            tv.setTextColor(Color.parseColor("#E0E0E0")); // Light Grey Body
            tv.setTextSize(11f);
        }
        return tv;
    }

    private void setupCharts(int grandTotalAttended, int grandTotalClasses, ArrayList<BarEntry> entries, ArrayList<String> labels, ArrayList<PieEntry> pieEntries) {
        // --- 1. BAR CHART CONFIG ---
        BarDataSet barSet = new BarDataSet(entries, "Attendance %");
        barSet.setColor(Color.parseColor("#00E5FF")); // Cyan Bars
        barSet.setValueTextColor(Color.WHITE); // White Numbers
        barSet.setValueTextSize(10f);

        BarData barData = new BarData(barSet);
        barData.setBarWidth(0.5f);

        barChart.setData(barData);

        // Axis Styling for Dark Theme
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.getDescription().setEnabled(false);

        // Hide Right Axis & Grid Lines for cleanliness
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#33FFFFFF"));

        // X-Axis Labels
        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        barChart.setExtraBottomOffset(20f);
        barChart.animateY(1000);
        barChart.invalidate();

        // --- 2. PIE CHART CONFIG ---
        float totalAvg = (grandTotalClasses == 0) ? 0 : ((float)grandTotalAttended / grandTotalClasses) * 100;

        pieEntries.add(new PieEntry(grandTotalAttended, "Present"));
        pieEntries.add(new PieEntry(grandTotalClasses - grandTotalAttended, "Absent"));

        PieDataSet pieSet = new PieDataSet(pieEntries, "");
        // Green & Red colors
        pieSet.setColors(Color.parseColor("#00E676"), Color.parseColor("#FF5252"));
        pieSet.setValueTextSize(12f);
        pieSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(pieSet);
        pieChart.setData(pieData);

        // Dark Theme Adjustments
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setHoleColor(Color.TRANSPARENT); // Transparent hole for glass effect
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setCenterText("Avg\n" + String.format("%.1f%%", totalAvg));
        pieChart.setCenterTextSize(16f);
        pieChart.setHoleRadius(45f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.invalidate();
    }
}