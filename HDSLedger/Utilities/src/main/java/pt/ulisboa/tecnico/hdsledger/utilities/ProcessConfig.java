package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {}

    public enum Persona {
        REGULAR,
        SLOW,
        DROP,
        WRONG_VALUE,
        WRONG_COMMIT,
        ROUND_CHANGE
    }

    private String hostname;

    private String id;

    private int port;

    private int clientPort; // port to listen for client requests

    private String pubKeyPath;

    private String privKeyPath;

    private int minFee;

    private int maxFee;

    private int feeThreshold;

    private float minAmountFeeRatio;

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

    public int getMinFee() {
        return minFee;
    }

    public int getMaxFee() {
        return maxFee;
    }

    public int getFeeThreshold() {
        return feeThreshold;
    }

    public float getMinAmountFeeRatio() {
        return minAmountFeeRatio;
    }

    public Persona getPersona() {
        return persona;
    }

}
