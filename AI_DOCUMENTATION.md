# Dokumentasi AI & Trust Engine - AdiMarket

Dokumen ini memberikan rincian teknis mengenai arsitektur, komponen, dan strategi optimalisasi AI yang diimplementasikan dalam aplikasi AdiMarket.

## 1. Arsitektur AI (Hybrid Approach)
AdiMarket menggunakan pendekatan Hybrid AI untuk menyeimbangkan performa, biaya, dan fungsionalitas:
- **On-Device AI (Edge):** Menggunakan TensorFlow Lite dan Google ML Kit untuk tugas yang memerlukan latensi rendah (Klasifikasi & OCR).
- **Cloud-Based AI:** Menggunakan Gemini 1.5 Flash dan Groq (Llama 3.3) untuk pemrosesan bahasa alami (NLP) yang kompleks dan Chatbot.

---

## 2. Komponen Utama & Logika Teknis

### A. AI Trust Engine (Fraud Detection)
Sistem keamanan berbasis AI untuk mendeteksi iklan kendaraan yang mencurigakan sebelum ditampilkan ke pengguna.
- **File:** `FraudDetection.java`
- **Metode:** Multi-Factor Risk Analysis dengan bobot:
    - **Price Risk (35%):** Membandingkan harga iklan dengan rata-rata harga pasar dari database. Jika < 40% dari harga pasar, dianggap risiko tinggi.
    - **Linguistic Analysis (25%):** Mendeteksi pola kalimat "Urgency Bias" (misal: "B.U", "DP dulu", "Transfer sekarang").
    - **Identity Validation (20%):** Validasi format nomor telepon Indonesia dan kelengkapan visual (jumlah foto).
    - **Content Integrity (20%):** Mendeteksi inkonsistensi antara deskripsi teks dengan spesifikasi teknis.

### B. Price Prediction Engine
Memberikan estimasi harga jual kendaraan yang adil bagi penjual dan pembeli.
- **File:** `PricePredictionActivity.java`
- **Teknis:** Menggunakan model regresi TensorFlow Lite (`price_prediction_model.tflite`).
- **Input:** Tahun kendaraan, Kilometer (KM), Kondisi Fisik, dan Jenis Kendaraan.
- **Output:** Nilai estimasi dalam Rupiah setelah proses denormalisasi.

### C. Personalization & Recommendation Engine
Meningkatkan retensi pengguna dengan menyesuaikan tampilan dashboard.
- **File:** `PersonalizationEngine.java` & `RecommendationEngine.java`
- **Logika:** 
    - Melacak interaksi pengguna (View, Search, Favorite).
    - Menentukan profil preferensi (Brand favorit, tipe kendaraan favorit, rentang harga).
    - **Dashboard Dynamic UI:** Mengubah salam, filter cepat, dan tema warna berdasarkan preferensi pengguna (misal: Tema Biru untuk pecinta Mobil, Merah untuk Motor).

### D. Multi-Model Chatbot Assistant
Asisten virtual cerdas yang membantu pengguna menavigasi aplikasi dan memberikan saran teknis.
- **File:** `GeminiHelper.java` & `GroqHelper.java`
- **Fitur:**
    - **Context Awareness:** Chatbot mengetahui stok kendaraan yang tersedia saat ini di database.
    - **Demo Mode:** Jika API Key tidak tersedia, sistem beralih ke *Local Intent Engine* agar layanan tidak terputus.
    - **Caching:** Menyimpan hasil respon untuk pertanyaan yang sama guna menghemat kuota API.

### E. Vision AI (Classification & OCR)
- **Vehicle Classifier:** Mengidentifikasi jenis kendaraan secara otomatis dari foto kamera.
- **OCR Engine:** Mengekstrak teks dari dokumen kendaraan (STNK/BPKB) untuk mempercepat pengisian data iklan.

---

## 3. Optimasi Performa (AIOptimizer)
Untuk memastikan aplikasi tetap ringan, AdiMarket mengimplementasikan:
- **Memory Management:** Triggering `System.gc()` manual sebelum menjalankan tugas AI berat.
- **Inference Tracking:** Memantau waktu eksekusi model TFLite; memberikan peringatan jika durasi > 500ms.
- **Hardware Acceleration:** Mendukung NNAPI (Neural Network API) untuk perangkat yang kompatibel.

---

## 4. Pengaturan & Pengujian

### A. Pengaturan API Key (Penting!)
Untuk keamanan, API Key tidak lagi disimpan di file Java. Ikuti langkah berikut:
1. Buka file `local.properties` di root project.
2. Tambahkan baris berikut:
   ```properties
   GEMINI_API_KEY=your_key_here
   GROQ_API_KEY=your_key_here
   ```
3. Lakukan **Project Sync** dengan Gradle.

### B. Daftar Asset Model (.tflite)
Pastikan file berikut berada di folder `app/src/main/assets/`:
- `vehicle_model.tflite`
- `price_prediction_model.tflite`

### C. Langkah Pengujian
1. **Chatbot:** Masuk ke menu Chatbot, coba tanya "Rekomendasi mobil murah".
2. **Jual Kendaraan:** Unggah foto dan deskripsi yang mencurigakan (misal harga 10jt untuk mobil 2024) untuk menguji **Fraud Detection**.
3. **Prediksi Harga:** Masuk ke menu "Prediksi Harga", isi data kendaraan, dan lihat hasil estimasinya.
