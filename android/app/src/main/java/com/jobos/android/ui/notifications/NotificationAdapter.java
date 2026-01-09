package com.jobos.android.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jobos.android.R;
import com.jobos.android.data.model.notification.NotificationDTO;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationDTO> notifications = new ArrayList<>();
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationDTO notification, int position);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<NotificationDTO> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final View unreadIndicator;
        private final TextView titleView;
        private final TextView messageView;
        private final TextView timeView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.notification_icon);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            titleView = itemView.findViewById(R.id.notification_title);
            messageView = itemView.findViewById(R.id.notification_message);
            timeView = itemView.findViewById(R.id.notification_time);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(notifications.get(position), position);
                }
            });
        }

        void bind(NotificationDTO notification) {
            titleView.setText(notification.getTitle());
            messageView.setText(notification.getMessage());
            
            String createdAt = notification.getCreatedAt();
            if (createdAt != null) {
                timeView.setText(createdAt);
            } else {
                timeView.setText("");
            }

            Boolean isRead = notification.getRead();
            unreadIndicator.setVisibility(isRead != null && !isRead ? View.VISIBLE : View.GONE);

            String type = notification.getType();
            int iconRes = getIconForType(type);
            iconView.setImageResource(iconRes);
        }

        private int getIconForType(String type) {
            if (type == null) return R.drawable.ic_notifications;
            switch (type) {
                case "APPLICATION_UPDATE":
                    return R.drawable.ic_description;
                case "JOB_MATCH":
                    return R.drawable.ic_work;
                case "NEW_APPLICATION":
                    return R.drawable.ic_person;
                case "MESSAGE":
                    return R.drawable.ic_email;
                default:
                    return R.drawable.ic_notifications;
            }
        }
    }
}
