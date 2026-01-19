package com.example.srmistpro;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class InputActivity extends AppCompatActivity {

    private List<AppData.Subject> subjects;
    private List<EditText[]> inputs = new ArrayList<>();

    // UI Containers
    private LinearLayout layoutSelection, layoutManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        layoutSelection = findViewById(R.id.layoutSelection);
        layoutManual = findViewById(R.id.layoutManual);

        // --- BUTTON 1: SYNC PORTAL ---
        findViewById(R.id.btnSyncPortal).setOnClickListener(v -> {
            startActivity(new Intent(this, PortalLoginActivity.class));
        });

        // --- BUTTON 2: MANUAL FILL ---
        findViewById(R.id.btnManualFill).setOnClickListener(v -> {
            layoutSelection.setVisibility(View.GONE);
            layoutManual.setVisibility(View.VISIBLE);
            setupManualInputs(); // Create fields dynamically
        });

        // Manual Generate Button
        findViewById(R.id.btnGenerate).setOnClickListener(v -> generateManualDashboard());
    }

    private void setupManualInputs() {
        LinearLayout container = findViewById(R.id.containerSubjects);
        subjects = AppData.getInitialSubjects();

        for (AppData.Subject s : subjects) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, 30);

            TextView label = new TextView(this);
            label.setText(s.code + " - " + s.name);
            label.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout inputsRow = new LinearLayout(this);
            inputsRow.setOrientation(LinearLayout.HORIZONTAL);

            EditText etAtt = new EditText(this);
            etAtt.setHint("Attended");
            etAtt.setInputType(InputType.TYPE_CLASS_NUMBER);
            etAtt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            EditText etTot = new EditText(this);
            etTot.setHint("Total Classes");
            etTot.setInputType(InputType.TYPE_CLASS_NUMBER);
            etTot.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            inputsRow.addView(etAtt);
            inputsRow.addView(etTot);

            row.addView(label);
            row.addView(inputsRow);
            container.addView(row);

            inputs.add(new EditText[]{etAtt, etTot});
        }
    }

    private void generateManualDashboard() {
        for (int i = 0; i < subjects.size(); i++) {
            String attStr = inputs.get(i)[0].getText().toString();
            String totStr = inputs.get(i)[1].getText().toString();

            subjects.get(i).attended = attStr.isEmpty() ? 0 : Integer.parseInt(attStr);
            subjects.get(i).total = totStr.isEmpty() ? 0 : Integer.parseInt(totStr);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("data", new Gson().toJson(subjects));
        startActivity(intent);
    }
}