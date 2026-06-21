package com.example.adimarket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * Adapter untuk fullscreen gallery dengan pinch-to-zoom.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder> {

    private final List<String> imageUris;
    private final Context context;

    public GalleryAdapter(Context context, List<String> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String uri = imageUris.get(position);
        Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.car_placeholder)
                .error(R.drawable.car_placeholder)
                .into(holder.imageView);

        // Pinch-to-zoom
        holder.setupZoom();
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        private float scaleFactor = 1f;
        private ScaleGestureDetector scaleDetector;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryItem);
        }

        void setupZoom() {
            scaleFactor = 1f;
            imageView.setScaleX(1f);
            imageView.setScaleY(1f);

            scaleDetector = new ScaleGestureDetector(itemView.getContext(),
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            scaleFactor *= detector.getScaleFactor();
                            // Batas zoom: min 1x, max 4x
                            scaleFactor = Math.max(1f, Math.min(scaleFactor, 4f));
                            imageView.setScaleX(scaleFactor);
                            imageView.setScaleY(scaleFactor);
                            return true;
                        }
                    });

            itemView.setOnTouchListener((v, event) -> {
                scaleDetector.onTouchEvent(event);
                // Double-tap: reset zoom
                if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
            });

            // Double-tap reset zoom
            imageView.setOnClickListener(null);
        }
    }
}
