package com.example.adimarket;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotifikasiActivity extends AppCompatActivity {

    private RecyclerView rvNotif;
    private TextView tvEmpty, tvBadge;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifikasi);

        dbHelper  = new DatabaseHelper(this);
        rvNotif   = findViewById(R.id.rvNotifikasi);
        tvEmpty   = findViewById(R.id.tvEmptyNotif);
        tvBadge   = findViewById(R.id.tvUnreadBadge);

        findViewById(R.id.btnTandaiBaca).setOnClickListener(v -> {
            dbHelper.markAllRead();
            loadNotifications();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        List<DatabaseHelper.NotifData> list = dbHelper.getAllNotifications();
        int unread = dbHelper.getUnreadCount();

        if (tvBadge != null) {
            if (unread > 0) {
                tvBadge.setText(unread + " belum dibaca");
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }

        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvNotif.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        rvNotif.setVisibility(View.VISIBLE);
        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        rvNotif.setAdapter(new NotifAdapter(list));
    }

    // ─── ADAPTER ─────────────────────────────────────────────────────────────

    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        final List<DatabaseHelper.NotifData> items;

        NotifAdapter(List<DatabaseHelper.NotifData> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notifikasi, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            DatabaseHelper.NotifData n = items.get(pos);
            h.tvTitle.setText(n.title);
            h.tvMsg.setText(n.message);

            // Waktu relatif
            String time = getRelativeTime(n.createdAt);
            h.tvTime.setText(time);

            // Warna background: belum baca = biru muda, sudah baca = putih/abu
            if (!n.isRead) {
                h.container.setBackgroundColor(Color.parseColor("#E3F2FD"));
                h.tvDot.setVisibility(View.VISIBLE);
            } else {
                h.container.setBackgroundColor(Color.parseColor("#FAFAFA"));
                h.tvDot.setVisibility(View.INVISIBLE);
            }

            // Ikon sesuai tipe
            String icon = getIcon(n.type);
            h.tvIcon.setText(icon);

            // Tandai baca saat diklik
            h.itemView.setOnClickListener(v -> {
                if (!n.isRead) {
                    new DatabaseHelper(v.getContext()).markRead(n.id);
                    n.isRead = true;
                    h.container.setBackgroundColor(Color.parseColor("#FAFAFA"));
                    h.tvDot.setVisibility(View.INVISIBLE);
                }
            });
        }

        @Override public int getItemCount() { return items.size(); }

        private String getIcon(String type) {
            if (type == null) return "🔔";
            switch (type) {
                case "iklan_disetujui": return "🎉";
                case "iklan_ditolak":  return "❌";
                case "iklan_baru":     return "📋";
                case "welcome":        return "👋";
                default:               return "💡";
            }
        }

        private String getRelativeTime(long millis) {
            long diff = System.currentTimeMillis() - millis;
            if (diff < 60_000)              return "Baru saja";
            if (diff < 3_600_000)           return (diff / 60_000) + " menit lalu";
            if (diff < 86_400_000)          return (diff / 3_600_000) + " jam lalu";
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout container;
            TextView tvIcon, tvTitle, tvMsg, tvTime, tvDot;
            VH(View v) {
                super(v);
                container = v.findViewById(R.id.containerNotif);
                tvIcon    = v.findViewById(R.id.tvNotifIcon);
                tvTitle   = v.findViewById(R.id.tvNotifTitle);
                tvMsg     = v.findViewById(R.id.tvNotifMsg);
                tvTime    = v.findViewById(R.id.tvNotifTime);
                tvDot     = v.findViewById(R.id.tvUnreadDot);
            }
        }
    }
}
