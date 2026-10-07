package com.astra.sirius;

import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import com.astra.sirius.browser.SiriusBrowserBridge;

public final class MainActivity extends Activity {
    private TextView status;
    private SiriusRouter router;
    private SiriusTaskDispatcher dispatcher;
    private SiriusBrowserBridge browserBridge;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startServiceCompat(SiriusNodeService.class);
        startServiceCompat(SiriusSupervisorService.class);

        router = new SiriusRouter("192.168.100.27", 8766);
        router.addNode("Samsung", "192.168.100.5", 8766);
        router.addNode("Huawei", "192.168.100.3", 8766);
        dispatcher = new SiriusTaskDispatcher(router, new SiriusClient());

        try { browserBridge = new SiriusBrowserBridge(8767); browserBridge.start(); } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(32,32,32,32);
        root.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this); title.setText("✦ ASTRA • SIRIUS ✦"); title.setTextColor(Color.WHITE); title.setTextSize(30); title.setGravity(Gravity.CENTER);
        TextView sub = new TextView(this); sub.setText("Browser Agent • LoreMotion • Distributed Core"); sub.setTextColor(Color.LTGRAY); sub.setGravity(Gravity.CENTER); sub.setPadding(0,16,0,24);
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(16); status.setGravity(Gravity.CENTER); status.setPadding(0,0,0,20);

        Button check = new Button(this); check.setText("CHECK NODES"); check.setOnClickListener(v -> checkNodes());
        Button test = new Button(this); test.setText("TEST DISTRIBUTION"); test.setOnClickListener(v -> testTask());

        root.addView(title); root.addView(sub); root.addView(status); root.addView(check); root.addView(test);
        setContentView(root); checkNodes();
    }

    private void startServiceCompat(Class<?> cls) {
        Intent i = new Intent(this, cls);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
    private void checkNodes() {
        status.setText("Checking nodes…");
        new Thread(() -> { String r = router.getNodesStatus(); runOnUiThread(() -> status.setText(r)); }).start();
    }
    private void testTask() {
        status.setText("Distributing task…");
        new Thread(() -> { String r = dispatcher.dispatch(router.createAddTask(15,27)); runOnUiThread(() -> status.setText(r + "\nNode: " + dispatcher.getLastNodeName())); }).start();
    }
    @Override protected void onDestroy() { if (browserBridge != null) browserBridge.stop(); super.onDestroy(); }
}
