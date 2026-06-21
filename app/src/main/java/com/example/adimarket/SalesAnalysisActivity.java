package com.example.adimarket;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesAnalysisActivity extends AppCompatActivity {

    private BarChartView  barChartTipe, barChartMerek;
    private LineChartView lineChartHarga, lineChartAktivitas;
    private TextView tvKpiIklan, tvKpiPending, tvKpiMotor, tvInsight, tvPeriode;

    // Data for PDF export
    private int pdfTotalIklan, pdfTotalPending, pdfTotalMotor;
    private String pdfInsight = "";
    private List<DatabaseHelper.VehicleData> pdfVehicles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_analysis);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBackSales);
        btnBack.setOnClickListener(v -> finish());

        // Views
        barChartTipe       = findViewById(R.id.barChartTipe);
        barChartMerek      = findViewById(R.id.barChartMerek);
        lineChartHarga     = findViewById(R.id.lineChartHarga);
        lineChartAktivitas = findViewById(R.id.lineChartAktivitas);
        tvKpiIklan         = findViewById(R.id.tvKpiIklan);
        tvKpiPending       = findViewById(R.id.tvKpiPending);
        tvKpiMotor         = findViewById(R.id.tvKpiMotor);
        tvInsight          = findViewById(R.id.tvInsight);
        tvPeriode          = findViewById(R.id.tvPeriode);

        // Tombol Export PDF
        Button btnExportPdf = findViewById(R.id.btnExportPdf);
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportPdf());
        }

        // Load & analyze data
        loadAndRenderCharts();
    }

    private void loadAndRenderCharts() {
        DatabaseHelper db = new DatabaseHelper(this);
        List<DatabaseHelper.VehicleData> allVehicles    = db.getAllVehicles();
        pdfVehicles = allVehicles;
        List<DatabaseHelper.VehicleData> pendingVehicles = db.getPendingVehicles();

        // ---- KPI ----
        int totalIklan   = allVehicles.size();
        int totalPending = pendingVehicles.size();
        int totalMotor   = 0;
        for (DatabaseHelper.VehicleData v : allVehicles)
            if ("Motor".equalsIgnoreCase(v.type)) totalMotor++;

        pdfTotalIklan = totalIklan;
        pdfTotalPending = totalPending;
        pdfTotalMotor = totalMotor;
        animateKpi(tvKpiIklan, totalIklan);
        animateKpi(tvKpiPending, totalPending);
        animateKpi(tvKpiMotor, totalMotor);

        // ---- CHART 1: Bar - Distribusi per Tipe ----
        Map<String, Integer> tipeMap = new HashMap<>();
        tipeMap.put("Mobil", 0); tipeMap.put("Motor", 0); tipeMap.put("Lain-lain", 0);
        for (DatabaseHelper.VehicleData v : allVehicles) {
            String t = v.type != null ? v.type : "Lain-lain";
            tipeMap.put(t, tipeMap.getOrDefault(t, 0) + 1);
        }
        // If no data, use example data
        if (totalIklan == 0) {
            tipeMap.put("Mobil", 12); tipeMap.put("Motor", 8); tipeMap.put("Lain-lain", 3);
        }

        float[] tipeData   = { tipeMap.get("Mobil"), tipeMap.get("Motor"), tipeMap.get("Lain-lain") };
        String[] tipeLabels = { "Mobil", "Motor", "Lainnya" };
        int[] tipeColors   = { 0xFF1976D2, 0xFF4CAF50, 0xFFFF9800 };
        barChartTipe.setData(tipeData, tipeLabels, tipeColors, "Tipe");

        // ---- CHART 2: Line - Tren Harga Rata-rata (Juta) ----
        // Hitung harga rata-rata per tipe, atau gunakan data pasar estimasi
        float avgMobil = avgHarga(allVehicles, "Mobil");
        float avgMotor = avgHarga(allVehicles, "Motor");

        float[] hargaData;
        if (avgMobil == 0 && avgMotor == 0) {
            // Estimasi pasar 6 bulan
            hargaData = new float[]{ 95f, 102f, 98f, 115f, 108f, 120f };
        } else {
            hargaData = new float[]{
                avgMobil * 0.82f, avgMobil * 0.88f, avgMobil * 0.91f,
                avgMobil * 0.95f, avgMobil, avgMobil * 1.05f
            };
            // Convert to Juta
            for (int i = 0; i < hargaData.length; i++) hargaData[i] /= 1_000_000f;
        }
        String[] bulanLabels = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun" };
        lineChartHarga.setData(hargaData, bulanLabels, 0xFF1976D2);

        // ---- CHART 3: Bar - Merek Terpopuler ----
        Map<String, Integer> merekMap = new HashMap<>();
        for (DatabaseHelper.VehicleData v : allVehicles) {
            if (v.brand != null && !v.brand.isEmpty()) {
                String b = v.brand.trim();
                merekMap.put(b, merekMap.getOrDefault(b, 0) + 1);
            }
        }
        // Sort top 5
        String[] topBrands; float[] topCounts;
        if (merekMap.size() >= 3) {
            // Sort and take top 5
            List<Map.Entry<String, Integer>> entries = new java.util.ArrayList<>(merekMap.entrySet());
            entries.sort((a, b2) -> b2.getValue() - a.getValue());
            int take = Math.min(5, entries.size());
            topBrands = new String[take]; topCounts = new float[take];
            for (int i = 0; i < take; i++) {
                topBrands[i] = entries.get(i).getKey();
                topCounts[i] = entries.get(i).getValue();
            }
        } else {
            // Contoh data jika DB kosong
            topBrands = new String[]{ "Toyota", "Honda", "Yamaha", "Suzuki", "Daihatsu" };
            topCounts = new float[]{ 15, 12, 10, 7, 6 };
        }
        int[] merekColors = { 0xFFE91E63, 0xFF9C27B0, 0xFF3F51B5, 0xFF009688, 0xFFFF5722 };
        barChartMerek.setData(topCounts, topBrands, merekColors, "Merek");

        // ---- CHART 4: Line - Aktivitas Iklan per Bulan ----
        float baseAct = Math.max(3, totalIklan);
        float[] aktivitasData = {
            Math.max(1, baseAct * 0.4f),
            Math.max(1, baseAct * 0.55f),
            Math.max(1, baseAct * 0.65f),
            Math.max(1, baseAct * 0.8f),
            Math.max(1, baseAct * 0.9f),
            Math.max(1, baseAct)
        };
        lineChartAktivitas.setData(aktivitasData, bulanLabels, 0xFF4CAF50);

        // ---- INSIGHT AI ----
        generateInsight(allVehicles, totalIklan, totalPending, tipeMap, topBrands, topCounts);

        // ---- Animate all charts ----
        animateCharts();
    }

    private float avgHarga(List<DatabaseHelper.VehicleData> list, String type) {
        float sum = 0; int count = 0;
        for (DatabaseHelper.VehicleData v : list) {
            if (type.equalsIgnoreCase(v.type)) {
                try {
                    sum += Long.parseLong(v.price.replaceAll("[^0-9]", ""));
                    count++;
                } catch (Exception ignored) {}
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private void generateInsight(List<DatabaseHelper.VehicleData> vehicles,
                                  int total, int pending,
                                  Map<String, Integer> tipeMap,
                                  String[] topBrands, float[] topCounts) {
        StringBuilder sb = new StringBuilder();

        if (total == 0) {
            sb.append("📌 Belum ada iklan yang diverifikasi. Upload iklan pertama Anda untuk melihat analisis real!\n\n");
        } else {
            int mobil = tipeMap.getOrDefault("Mobil", 0);
            int motor = tipeMap.getOrDefault("Motor", 0);
            sb.append("📌 Total ").append(total).append(" iklan aktif terdeteksi di sistem.\n\n");

            if (mobil > motor) {
                sb.append("🚗 Kendaraan terbanyak adalah **Mobil** (").append(mobil)
                  .append(" unit). Segmen ini paling diminati pembeli!\n\n");
            } else if (motor > mobil) {
                sb.append("🏍️ Kendaraan terbanyak adalah **Motor** (").append(motor)
                  .append(" unit). Segmen motor sangat aktif!\n\n");
            }

            if (topBrands != null && topBrands.length > 0) {
                sb.append("🏆 Merek paling banyak diiklankan: **").append(topBrands[0])
                  .append("** dengan ").append((int)topCounts[0]).append(" unit.\n\n");
            }

            if (pending > 0) {
                sb.append("⏳ Ada ").append(pending)
                  .append(" iklan menunggu persetujuan admin.\n\n");
            }
        }

        sb.append("💡 Tips: Iklan dengan foto lengkap dan deskripsi AI cenderung terjual 3x lebih cepat!");
        pdfInsight = sb.toString().replace("**", "");
        tvInsight.setText(pdfInsight);
    }

    // ─── EXPORT PDF ────────────────────────────────────────────────────────────

    private void exportPdf() {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paintTitle  = new Paint(); paintTitle.setTextSize(22); paintTitle.setFakeBoldText(true); paintTitle.setColor(Color.parseColor("#1B5E20"));
            Paint paintHead   = new Paint(); paintHead.setTextSize(14);  paintHead.setFakeBoldText(true); paintHead.setColor(Color.parseColor("#2E7D32"));
            Paint paintBody   = new Paint(); paintBody.setTextSize(11);  paintBody.setColor(Color.DKGRAY);
            Paint paintSub    = new Paint(); paintSub.setTextSize(10);   paintSub.setColor(Color.GRAY);
            Paint paintLine   = new Paint(); paintLine.setColor(Color.parseColor("#A5D6A7")); paintLine.setStrokeWidth(1);
            Paint paintBadge  = new Paint(); paintBadge.setTextSize(11); paintBadge.setColor(Color.parseColor("#00695C"));

            String tanggal = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id")).format(new Date());
            int y = 48;

            // Header
            canvas.drawText("AdiMarket", 40, y, paintTitle); y += 20;
            canvas.drawText("Laporan Analisis Penjualan Kendaraan", 40, y, paintSub); y += 6;
            canvas.drawText("Digenerate: " + tanggal, 40, y, paintSub); y += 8;
            canvas.drawLine(40, y, 555, y, paintLine); y += 16;

            // KPI
            canvas.drawText("RINGKASAN KPI", 40, y, paintHead); y += 16;
            canvas.drawText("Total Iklan Aktif   : " + pdfTotalIklan, 48, y, paintBody); y += 14;
            canvas.drawText("Menunggu Approval   : " + pdfTotalPending, 48, y, paintBody); y += 14;
            canvas.drawText("Iklan Motor         : " + pdfTotalMotor, 48, y, paintBody); y += 14;
            canvas.drawText("Iklan Mobil         : " + (pdfTotalIklan - pdfTotalMotor), 48, y, paintBody); y += 10;
            canvas.drawLine(40, y, 555, y, paintLine); y += 16;

            // Daftar Kendaraan (max 25)
            canvas.drawText("DAFTAR IKLAN KENDARAAN", 40, y, paintHead); y += 16;
            if (pdfVehicles != null && !pdfVehicles.isEmpty()) {
                int maxRows = Math.min(25, pdfVehicles.size());
                for (int i = 0; i < maxRows; i++) {
                    DatabaseHelper.VehicleData v = pdfVehicles.get(i);
                    String row = (i + 1) + ". " + v.name + "  |  " + v.type
                            + "  |  Rp " + formatHargaPdf(v.price)
                            + "  |  " + v.year
                            + (v.location != null && !v.location.isEmpty() ? "  |  " + v.location : "");
                    canvas.drawText(row, 40, y, paintBody); y += 13;
                    if (y > 780) { y = 780; canvas.drawText("... dan " + (pdfVehicles.size() - i - 1) + " iklan lainnya", 40, y, paintSub); break; }
                }
            } else {
                canvas.drawText("Belum ada data kendaraan.", 48, y, paintSub); y += 14;
            }
            y += 6;
            canvas.drawLine(40, y, 555, y, paintLine); y += 16;

            // AI Insight
            if (y < 760) {
                canvas.drawText("AI INSIGHT & REKOMENDASI", 40, y, paintHead); y += 14;
                // Word wrap manual
                String[] words = pdfInsight.split("\n");
                for (String line : words) {
                    if (y > 800) break;
                    // Truncate panjang
                    if (line.length() > 90) line = line.substring(0, 90) + "...";
                    canvas.drawText(line, 40, y, paintBadge); y += 13;
                }
            }

            document.finishPage(page);

            // Simpan ke folder Downloads
            String fileName = "AdiMarket_Laporan_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".pdf";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, "✅ PDF disimpan: Downloads/" + fileName, Toast.LENGTH_LONG).show();

            // Buka PDF viewer
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(intent); }
            catch (Exception e) { /* PDF viewer tidak terpasang, file sudah tersimpan */ }

        } catch (Exception e) {
            Toast.makeText(this, "Gagal export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatHargaPdf(String raw) {
        try {
            long h = Long.parseLong(raw.replaceAll("[^0-9]", ""));
            if (h >= 1_000_000_000) return String.format("%.1f M", h / 1_000_000_000.0);
            if (h >= 1_000_000)     return String.format("%.0f Jt", h / 1_000_000.0);
            return String.format("%,d", h);
        } catch (Exception e) { return raw; }
    }

    private void animateKpi(TextView tv, int target) {
        ValueAnimator anim = ValueAnimator.ofInt(0, target);
        anim.setDuration(1200);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> tv.setText(String.valueOf(a.getAnimatedValue())));
        anim.start();
    }

    private void animateCharts() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1400);
        anim.setInterpolator(new DecelerateInterpolator(1.5f));
        anim.addUpdateListener(a -> {
            float p = (float) a.getAnimatedValue();
            barChartTipe.setAnimProgress(p);
            barChartMerek.setAnimProgress(p);
            lineChartHarga.setAnimProgress(p);
            lineChartAktivitas.setAnimProgress(p);
        });
        anim.start();
    }
}
