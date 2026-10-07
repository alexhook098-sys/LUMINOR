package com.astra.sirius;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.net.*;

public final class SiriusSupervisorService extends Service {
    private static final String CHANNEL = "sirius_supervisor";
    private volatile boolean running;
    private Thread thread;

    @Override public void onCreate() {
        super.onCreate(); createChannel();
        startForeground(1001, notification("Nodes: checking…"));
        running = true; thread = new Thread(this::loop, "sirius-supervisor"); thread.start();
    }
    private void loop() {
        while (running) {
            int online = 0;
            online += check("Vivo", "192.168.100.27", 8766) ? 1 : 0;
            online += check("Samsung", "192.168.100.5", 8766) ? 1 : 0;
            online += check("Huawei", "192.168.100.3", 8766) ? 1 : 0;
            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) m.notify(1001, notification("Nodes ONLINE: " + online + "/3"));
            try { Thread.sleep(30000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }
    private boolean check(String name, String host, int port) {
        try (Socket s = new Socket()) { s.connect(new InetSocketAddress(host, port), 1000); return true; }
        catch (Exception e) { return false; }
    }
    private Notification notification(String text) {
        return new Notification.Builder(this, CHANNEL).setContentTitle("SIRIUS Supervisor").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_info_details).setOngoing(true).build();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "SIRIUS Supervisor", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) { return START_STICKY; }
    @Override public void onDestroy() { running = false; if (thread != null) thread.interrupt(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
