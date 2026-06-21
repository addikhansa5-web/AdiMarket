package com.example.adimarket;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BeliKendaraanActivity extends AppCompatActivity {

    // UI
    private EditText etCariKendaraan;
    private ImageButton btnFilter;
    private LinearLayout llFilterPanel;
    private EditText etFilterMerek, etHargaMin, etHargaMax, etTahunMin, etTahunMax;
    private Spinner spinnerTipe;
    private Button btnTerapkanFilter, btnResetFilter;
    private RecyclerView rvKendaraanDinamis;
    private DynamicVehicleAdapter adapter;
    private TextView tvEmptyState, tvFilterInfo;

    // Tabs
    private TextView tabSemua, tabMobil, tabMotor, tabLainnya, tabFavorit;

    // Data
    private List<DatabaseHelper.VehicleData> allVehicles;
    private List<DatabaseHelper.VehicleData> displayedVehicles;
    private DatabaseHelper dbHelper;

    // State
    private String currentCategory = "";
    private boolean isFilterPanelOpen = false;
    private boolean isFavoritTab = false;

    // Active filters
    private String filterMerek = "";
    private long filterHargaMin = 0;
    private long filterHargaMax = Long.MAX_VALUE;
    private int filterTahunMin = 1990;
    private int filterTahunMax = 2025;
    private String filterKota = "";
    private String filterWarna = "";     // BARU: filter warna
    private String filterKondisi = "";   // BARU: filter kondisi (Baru/Bekas)
    private Spinner spinnerFilterKota;
    private Spinner spinnerWarna;        // BARU
    private Spinner spinnerKondisi;      // BARU

    private static final String USER_ID = "user_adimarket";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beli_kendaraan);
        WallpaperHelper.apply(this, findViewById(R.id.headerImage));

        dbHelper = new DatabaseHelper(this);

        // Bind Views
        etCariKendaraan    = findViewById(R.id.etCariKendaraan);
        btnFilter          = findViewById(R.id.btnFilter);
        llFilterPanel      = findViewById(R.id.llFilterPanel);
        etFilterMerek      = findViewById(R.id.etFilterMerek);
        etHargaMin         = findViewById(R.id.etHargaMin);
        etHargaMax         = findViewById(R.id.etHargaMax);
        etTahunMin         = findViewById(R.id.etTahunMin);
        etTahunMax         = findViewById(R.id.etTahunMax);
        spinnerTipe        = findViewById(R.id.spinnerTipe);
        btnTerapkanFilter  = findViewById(R.id.btnTerapkanFilter);
        btnResetFilter     = findViewById(R.id.btnResetFilter);
        rvKendaraanDinamis = findViewById(R.id.rvKendaraanDinamis);
        tvEmptyState       = findViewById(R.id.tvEmptyState);
        tvFilterInfo       = findViewById(R.id.tvFilterInfo);
        tabSemua           = findViewById(R.id.tabSemua);
        tabMobil           = findViewById(R.id.tabMobil);
        tabMotor           = findViewById(R.id.tabMotor);
        tabLainnya         = findViewById(R.id.tabLainnya);
        tabFavorit         = findViewById(R.id.tabFavorit);

        // Setup Spinner Tipe
        String[] tipeOptions = {"Semua", "Mobil", "Motor", "Lain-lain"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipeOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipe.setAdapter(spinnerAdapter);

        // Load data
        allVehicles = dbHelper.getAllVehicles();
        displayedVehicles = new ArrayList<>();

        // Setup RecyclerView
        adapter = new DynamicVehicleAdapter(displayedVehicles, dbHelper, USER_ID);
        rvKendaraanDinamis.setLayoutManager(new LinearLayoutManager(this));
        rvKendaraanDinamis.setAdapter(adapter);

        // Default: tampilkan semua
        setActiveTab(tabSemua);

        // Setup Spinner Filter Kota
        spinnerFilterKota = findViewById(R.id.spinnerFilterKota);
        List<String> kotaOptions = new ArrayList<>();
        kotaOptions.add("📍 Semua Kota");
        kotaOptions.addAll(dbHelper.getDistinctCities());
        ArrayAdapter<String> kotaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kotaOptions);
        kotaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterKota.setAdapter(kotaAdapter);
        spinnerFilterKota.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                filterKota = pos == 0 ? "" : kotaOptions.get(pos);
                applyFiltersAndSearch();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Setup Spinner Warna
        spinnerWarna = findViewById(R.id.spinnerWarna);
        if (spinnerWarna != null) {
            String[] warnaOptions = {"🎨 Semua Warna", "Putih", "Hitam", "Silver/Abu", "Merah",
                    "Biru", "Hijau", "Kuning", "Orange", "Coklat", "Ungu", "Gold"};
            ArrayAdapter<String> warnaAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, warnaOptions);
            warnaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerWarna.setAdapter(warnaAdapter);
        }

        // Setup Spinner Kondisi
        spinnerKondisi = findViewById(R.id.spinnerKondisi);
        if (spinnerKondisi != null) {
            String[] kondisiOptions = {"🏷️ Semua Kondisi", "Baru", "Bekas (Sangat Baik)",
                    "Bekas (Baik)", "Bekas (Cukup)"};
            ArrayAdapter<String> kondisiAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, kondisiOptions);
            kondisiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerKondisi.setAdapter(kondisiAdapter);
        }

        applyFiltersAndSearch();

        // Search real-time
        etCariKendaraan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Toggle Filter Panel
        btnFilter.setOnClickListener(v -> {
            isFilterPanelOpen = !isFilterPanelOpen;
            llFilterPanel.setVisibility(isFilterPanelOpen ? View.VISIBLE : View.GONE);
        });

        // Terapkan Filter
        btnTerapkanFilter.setOnClickListener(v -> {
            filterMerek = etFilterMerek.getText().toString().trim();
            try { filterHargaMin = Long.parseLong(etHargaMin.getText().toString().trim()) * 1_000_000L; }
            catch (Exception e) { filterHargaMin = 0; }
            try { filterHargaMax = Long.parseLong(etHargaMax.getText().toString().trim()) * 1_000_000L; }
            catch (Exception e) { filterHargaMax = Long.MAX_VALUE; }
            try { filterTahunMin = Integer.parseInt(etTahunMin.getText().toString().trim()); }
            catch (Exception e) { filterTahunMin = 1990; }
            try { filterTahunMax = Integer.parseInt(etTahunMax.getText().toString().trim()); }
            catch (Exception e) { filterTahunMax = 2025; }

            // Tipe dari spinner
            int selectedTipe = spinnerTipe.getSelectedItemPosition();
            currentCategory = selectedTipe == 0 ? "" : tipeOptions[selectedTipe];

            // Warna dari spinnerWarna
            if (spinnerWarna != null && spinnerWarna.getSelectedItemPosition() > 0) {
                filterWarna = spinnerWarna.getSelectedItem().toString();
            } else {
                filterWarna = "";
            }

            // Kondisi dari spinnerKondisi
            if (spinnerKondisi != null && spinnerKondisi.getSelectedItemPosition() > 0) {
                filterKondisi = spinnerKondisi.getSelectedItem().toString()
                        .replace("🏷️ Semua Kondisi", "");
            } else {
                filterKondisi = "";
            }

            llFilterPanel.setVisibility(View.GONE);
            isFilterPanelOpen = false;
            isFavoritTab = false;
            updateTabUI();
            applyFiltersAndSearch();
        });

        // Reset Filter
        btnResetFilter.setOnClickListener(v -> {
            etFilterMerek.setText(""); etHargaMin.setText(""); etHargaMax.setText("");
            etTahunMin.setText(""); etTahunMax.setText(""); spinnerTipe.setSelection(0);
            if (spinnerFilterKota != null) spinnerFilterKota.setSelection(0);
            if (spinnerWarna != null) spinnerWarna.setSelection(0);
            if (spinnerKondisi != null) spinnerKondisi.setSelection(0);
            filterMerek = ""; filterHargaMin = 0; filterHargaMax = Long.MAX_VALUE;
            filterTahunMin = 1990; filterTahunMax = 2025; currentCategory = "";
            filterKota = ""; filterWarna = ""; filterKondisi = "";
            tvFilterInfo.setVisibility(View.GONE);
            setActiveTab(tabSemua);
            applyFiltersAndSearch();
        });

        // TABS
        tabSemua.setOnClickListener(v -> { currentCategory = ""; isFavoritTab = false; setActiveTab(tabSemua); applyFiltersAndSearch(); });
        tabMobil.setOnClickListener(v -> { currentCategory = "Mobil"; isFavoritTab = false; setActiveTab(tabMobil); applyFiltersAndSearch(); });
        tabMotor.setOnClickListener(v -> { currentCategory = "Motor"; isFavoritTab = false; setActiveTab(tabMotor); applyFiltersAndSearch(); });
        tabLainnya.setOnClickListener(v -> { currentCategory = "Lain-lain"; isFavoritTab = false; setActiveTab(tabLainnya); applyFiltersAndSearch(); });
        tabFavorit.setOnClickListener(v -> { isFavoritTab = true; setActiveTab(tabFavorit); showFavorites(); });
    }

    private void applyFiltersAndSearch() {
        if (isFavoritTab) { showFavorites(); return; }

        // Tampilkan Shimmer Loading State
        final com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.shimmerVehicleList);
        if (shimmer != null) {
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
            rvKendaraanDinamis.setVisibility(View.INVISIBLE);
        }

        String keyword = etCariKendaraan.getText().toString().trim();
        List<DatabaseHelper.VehicleData> filtered;

        // AI Smart Search: jika keyword >= 5 karakter, gunakan AI intent parser
        if (keyword.length() >= 5) {
            VehicleAIEngine.SearchIntent intent = VehicleAIEngine.parseSearchIntent(keyword);

            // Gabungkan dengan filter panel aktif
            if (!filterKota.isEmpty() && intent.vehicleType.isEmpty()) {
                intent.vehicleType = "";
            }
            if (!currentCategory.isEmpty()) intent.vehicleType = currentCategory;
            if (intent.budgetMax == 0 && filterHargaMax < Long.MAX_VALUE) intent.budgetMax = filterHargaMax;
            if (!filterMerek.isEmpty()) intent.brand = filterMerek;

            List<DatabaseHelper.VehicleData> smartResult = VehicleAIEngine.applySmartSearch(intent, allVehicles);
            if (!smartResult.isEmpty()) {
                filtered = smartResult;
            } else {
                // Fallback ke pencarian biasa jika AI tidak menemukan hasil
                filtered = dbHelper.searchAdvanced(currentCategory, filterMerek, filterHargaMin,
                        filterHargaMax == Long.MAX_VALUE ? 999_000_000_000L : filterHargaMax,
                        filterTahunMin, filterTahunMax);
                filtered = filtered.stream().filter(v ->
                    v.name.toLowerCase().contains(keyword.toLowerCase()) ||
                    v.brand.toLowerCase().contains(keyword.toLowerCase())
                ).collect(java.util.stream.Collectors.toList());
            }
        } else {
            // Pencarian biasa untuk keyword pendek
            filtered = dbHelper.searchAdvanced(currentCategory, filterMerek, filterHargaMin,
                    filterHargaMax == Long.MAX_VALUE ? 999_000_000_000L : filterHargaMax,
                    filterTahunMin, filterTahunMax);
        }

        displayedVehicles.clear();
        for (DatabaseHelper.VehicleData v : filtered) {
            boolean kotaOk = filterKota.isEmpty()
                    || (v.location != null && v.location.equalsIgnoreCase(filterKota));
            boolean warnaOk = filterWarna.isEmpty()
                    || (v.color != null && v.color.toLowerCase().contains(filterWarna.toLowerCase()));
            boolean kondisiOk = filterKondisi.isEmpty()
                    || filterKondisi.equalsIgnoreCase(v.type)  // fallback
                    || (v.name != null && v.name.toLowerCase().contains(filterKondisi.toLowerCase()));
            boolean keyOk = keyword.length() >= 5 || keyword.isEmpty()
                    || v.name.toLowerCase().contains(keyword.toLowerCase())
                    || v.brand.toLowerCase().contains(keyword.toLowerCase())
                    || v.type.toLowerCase().contains(keyword.toLowerCase());
            if (kotaOk && warnaOk && kondisiOk && keyOk) displayedVehicles.add(v);
        }

        // Simulasikan delay render agar efek Shimmer terlihat premium & nyata
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (shimmer != null) {
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
            }
            rvKendaraanDinamis.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();

            // Update info & empty state
            boolean hasFilter = !filterMerek.isEmpty() || filterHargaMin > 0
                    || filterHargaMax < Long.MAX_VALUE || !filterKota.isEmpty() || !keyword.isEmpty();
            if (hasFilter) {
                String infoText = "🤖 " + displayedVehicles.size() + " hasil";
                if (!keyword.isEmpty()) infoText += " untuk \"" + keyword + "\"";
                if (!filterKota.isEmpty()) infoText += " di " + filterKota;
                tvFilterInfo.setText(infoText);
                tvFilterInfo.setVisibility(View.VISIBLE);
            } else {
                tvFilterInfo.setVisibility(View.GONE);
            }

            tvEmptyState.setVisibility(displayedVehicles.isEmpty() ? View.VISIBLE : View.GONE);
        }, 650); // 650ms delay
    }

    private void showFavorites() {
        List<DatabaseHelper.VehicleData> favList = dbHelper.getFavoriteVehicles(USER_ID);
        displayedVehicles.clear();
        displayedVehicles.addAll(favList);
        adapter.notifyDataSetChanged();
        tvFilterInfo.setText("❤️ " + favList.size() + " kendaraan favorit");
        tvFilterInfo.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(favList.isEmpty() ? View.VISIBLE : View.GONE);
        if (favList.isEmpty()) tvEmptyState.setText("😔 Belum ada kendaraan favorit.\nKetuk ❤ di iklan manapun untuk menyimpan!");
    }

    private void setActiveTab(TextView activeTab) {
        // Reset all tabs
        for (TextView tab : new TextView[]{tabSemua, tabMobil, tabMotor, tabLainnya}) {
            tab.setBackgroundResource(R.drawable.btn_outline_rounded);
            tab.setTextColor(getColor(R.color.primary_green));
        }
        tabFavorit.setBackgroundResource(R.drawable.btn_outline_pink);
        tabFavorit.setTextColor(getColor(android.R.color.holo_red_light));

        // Set active
        if (activeTab == tabFavorit) {
            activeTab.setBackgroundResource(R.drawable.btn_outline_pink);
            activeTab.setTextColor(0xFFE91E63);
        } else {
            activeTab.setBackgroundResource(R.drawable.btn_green_rounded);
            activeTab.setTextColor(0xFFFFFFFF);
        }
    }

    private void updateTabUI() {
        TextView activeTab = tabSemua;
        if (currentCategory.equals("Mobil")) activeTab = tabMobil;
        else if (currentCategory.equals("Motor")) activeTab = tabMotor;
        else if (currentCategory.equals("Lain-lain")) activeTab = tabLainnya;
        setActiveTab(activeTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        allVehicles = dbHelper.getAllVehicles();
        applyFiltersAndSearch();
    }
}