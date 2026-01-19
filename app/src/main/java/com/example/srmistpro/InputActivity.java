package com.example.srmistpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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

    // UI References
    private View landingContainer, manualContainer, cardSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        // Bind Views
        landingContainer = findViewById(R.id.landingContainer);
        manualContainer = findViewById(R.id.manualContainer);
        cardSaved = findViewById(R.id.cardSaved);

        // --- 1. HANDLE SAVED DATA VISIBILITY ---
        // NEW CODE
        String savedJson = com.example.srmistpro.SecureStore.loadData(this, "LAST_ATTENDANCE");

        if (savedJson != null) {
            cardSaved.setVisibility(View.VISIBLE);
            cardSaved.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("data", savedJson);
                startActivity(intent);
            });
        }

        // --- 2. CARD CLICK LISTENERS ---

        // Sync Card -> Open Login
        findViewById(R.id.cardSync).setOnClickListener(v ->
                startActivity(new Intent(this, PortalLoginActivity.class)));

        // Manual Card -> Show Input Form
        findViewById(R.id.cardManual).setOnClickListener(v -> {
            landingContainer.setVisibility(View.GONE);
            manualContainer.setVisibility(View.VISIBLE);
            setupManualInputs();
        });

        // --- 3. MANUAL FORM LISTENERS ---

        // Generate Button
        findViewById(R.id.btnGenerate).setOnClickListener(v -> generateManualDashboard());

        // Cancel Button (Go back to cards)
        findViewById(R.id.btnCancelManual).setOnClickListener(v -> {
            manualContainer.setVisibility(View.GONE);
            landingContainer.setVisibility(View.VISIBLE);
        });
    }

    private void setupManualInputs() {
        LinearLayout container = findViewById(R.id.containerSubjects);
        container.removeAllViews(); // Clear previous inputs if any
        subjects = AppData.getInitialSubjects();
        inputs.clear();

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