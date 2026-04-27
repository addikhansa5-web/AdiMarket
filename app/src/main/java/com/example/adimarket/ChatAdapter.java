package com.example.adimarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatbotActivity.ChatMessage> messageList;

    public ChatAdapter(List<ChatbotActivity.ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatbotActivity.ChatMessage chatMessage = messageList.get(position);
        holder.tvSender.setText(chatMessage.sender);
        holder.tvMessage.setText(chatMessage.message);
        
        // Basic styling for user vs bot
        if (chatMessage.isUser) {
            holder.itemView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            holder.tvMessage.setBackgroundResource(android.R.drawable.toast_frame);
        } else {
            holder.itemView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            holder.tvMessage.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}