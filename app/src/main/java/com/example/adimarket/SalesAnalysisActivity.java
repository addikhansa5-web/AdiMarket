package com.example.adimarket;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SalesAnalysisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_analysis);

        Button btnBack = findViewById(R.id.btnBackSales);
        btnBack.setOnClickListener(v -> finish());
    }
}
