package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {}

    public enum Persona {
        REGULAR,
        SLOW,
        BYZANTINE_DROP,
        WRONG_VALUE,
        WRONG_COMMIT
    }

    private String hostname;

    private String id;

    private int port;

    private int clientPort; // port to listen for client requests

    private String pubKeyPath;

    private String privKeyPath;

    private Persona persona;

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

    public Persona getPersona() {
        return persona;
    }

}
