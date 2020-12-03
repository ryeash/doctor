package doctor.cluster.jobs;

import doctor.cluster.Cluster;

public class ConnectJob implements Runnable {

    private final Cluster cluster;
    private final String host;
    private final int port;

    public ConnectJob(Cluster cluster, String host, int port) {
        this.cluster = cluster;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        cluster.
    }
}
