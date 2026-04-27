AdiMarket mengusung desain Modern Material Design yang bersih, responsif, dan ramah pengguna. Berikut adalah rincian antarmuka utamanya:
1. Smart Interactive Dashboard
•Card-Based Navigation: Menggunakan CardView dengan elevasi halus untuk navigasi menu yang intuitif.
•Adaptive ScrollView: Tata letak yang fleksibel dan dapat di-scroll, memastikan aksesibilitas di berbagai ukuran layar smartphone.
•Iconography & Color Coding:
◦🟢 Green untuk Jual Kendaraan (Keuntungan).
◦🔵 Blue untuk Beli Kendaraan (Kepercayaan).
◦🟣 Purple untuk AI Assistant (Teknologi/Cerdas).
◦🟡 Yellow untuk Analisis (Data/Bintang).
2. Intelligent AI Chatbot Interface
•Bubble Chat UI: Antarmuka percakapan yang familiar dengan pemisahan warna antara pesan pengguna (kanan) dan AI (kiri).
•Real-time Typing Feedback: Memberikan kesan interaktif saat AI memproses jawaban.
•Dynamic Response System: UI yang secara cerdas menampilkan jawaban instan berkat optimasi AI Caching (Phase 14).
3. Advanced Vehicle Listing & Security UI
•Fraud Risk Badge: Sistem peringatan otomatis dengan badge "HIGH RISK / SUSPICIOUS" berwarna merah terang pada item kendaraan yang memiliki harga tidak wajar (Phase 10).
•Dynamic Star Ratings: Visualisasi reputasi kendaraan menggunakan RatingBar yang terintegrasi langsung dengan database lokal.
•Responsive Media Gallery: Penampilan foto kendaraan yang tajam dengan manajemen memori yang dioptimalkan menggunakan internal storage persistence.
4. Sales Analytics Dashboard
•Visual Data Representation: Menggunakan grafik batang (Bar Chart) untuk menampilkan tren penjualan antara kategori Mobil dan Motor secara visual.
•Clean Statistics: Layout minimalis yang memudahkan Admin memantau performa marketplace dalam satu klik.
5. Admin Control Center
•Secure Access: Dialog input password yang aman untuk masuk ke menu manajemen.
•Approval Workflow: UI khusus bagi admin untuk memverifikasi kelayakan iklan sebelum dipublikasikan secara umum.
🛠 Tech Stack UI
•Layout: XML (ConstraintLayout, LinearLayout, ScrollView)
•Components: Material Components, CardView, RecyclerView, RatingBar
•Image Loading: Glide (untuk rendering gambar yang cepat)
•Animations: Default Android View Animations untuk transisi antar Activity.
