package com.astra.sirius;

public final class SiriusTaskDispatcher {
    private final SiriusRouter router;
    private final SiriusClient client;
    private String lastNodeName = "UNKNOWN";

    public SiriusTaskDispatcher(SiriusRouter router, SiriusClient client) {
        this.router = router;
        this.client = client;
    }

    public synchronized String dispatch(SiriusTask task) {
        router.checkNodes();
        SiriusNodeInfo best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (SiriusNodeInfo n : router.getNodes()) {
            if (!n.isOnline()) continue;
            long ram = n.getAvailableRamMb();
            if (ram >= 0 && ram < task.getRequiredRamMb()) continue;
            double score = (ram < 0 ? 0 : ram * 10.0) + 10000.0 / (n.getLatencyMs() + 1);
            if (best == null || score > bestScore) { best = n; bestScore = score; }
        }
        if (best == null) { lastNodeName = "NONE"; return "ERROR:NO_CAPABLE_NODES"; }
        lastNodeName = best.getName();
        return client.sendTask(best.getHost(), best.getPort(), task);
    }

    public String getLastNodeName() { return lastNodeName; }
}
