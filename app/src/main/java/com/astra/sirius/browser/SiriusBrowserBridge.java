package com.astra.sirius.browser;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local Browser Bridge for SIRIUS.
 *
 * It does not bypass Turnstile or other human verification.
 * The actual Chromium/CDP Browser Agent may run in Termux and
 * communicate with this bridge over localhost.
 */
public final class SiriusBrowserBridge {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket server;

    public SiriusBrowserBridge(int port) {
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (running) return;
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        running = true;
        pool.execute(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket socket = server.accept();
                pool.execute(() -> handle(socket));
            } catch (IOException e) {
                if (running) System.err.println("[SIRIUS BROWSER] " + e.getMessage());
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(
                     s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                     s.getOutputStream(), StandardCharsets.UTF_8))) {

            String line = in.readLine();
            if (line == null) return;

            // Minimal health protocol. Task payloads are forwarded by the
            // external Browser Agent; this service intentionally does not
            // execute arbitrary JavaScript.
            String response;
            if ("PING".equalsIgnoreCase(line.trim())) {
                response = "{\"ok\":true,\"service\":\"sirius-browser-bridge\"}";
            } else {
                response = "{\"ok\":false,\"error\":\"UNSUPPORTED_COMMAND\"}";
            }

            out.write(response);
            out.write("\n");
            out.flush();
        } catch (IOException e) {
            System.err.println("[SIRIUS BROWSER] CLIENT: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        pool.shutdownNow();
    }
}
