package com.jobos.android.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jobos.android.R;
import com.jobos.android.data.model.Notification;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.title.setText(notification.getTitle());
        holder.body.setText(notification.getBody());
        
        if (notification.getCreatedAt() != null) {
            String timeStr = dateFormat.format(new Date(notification.getCreatedAt()));
            holder.time.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void addNotification(Notification notification) {
        notifications.add(0, notification); // Add to top
        notifyItemInserted(0);
    }

    public void clearNotifications() {
        notifications.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView body;
        TextView time;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.notificationTitle);
            body = view.findViewById(R.id.notificationBody);
            time = view.findViewById(R.id.notificationTime);
        }
    }
}
