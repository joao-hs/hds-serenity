package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {}

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private int clientPort; // port to listen for client requests

    private String pubKeyPath;

    private String privKeyPath;

    public boolean isLeader() {
        return isLeader;
    }

    public int getPort() {
        return port;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPubKeyPath() {
        return pubKeyPath;
    }

    public String getPrivKeyPath() {
        return privKeyPath;
    }

}
