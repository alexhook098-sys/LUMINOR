package com.astra.sirius;

import android.app.*;
import android.content.Intent;
import android.os.*;

public final class SiriusNodeService extends Service {
    private static final String CHANNEL = "sirius_node";
    private SiriusNode node;

    @Override public void onCreate() {
        super.onCreate(); createChannel();
        startForeground(2001, notification("Distributed node ONLINE • 8766"));
        node = new SiriusNode(); node.start();
    }
    private Notification notification(String text) {
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("SIRIUS Node").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_info_details).setOngoing(true).build();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "SIRIUS Node", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) { return START_STICKY; }
    @Override public void onDestroy() { if (node != null) node.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
