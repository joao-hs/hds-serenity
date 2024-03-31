package pt.ulisboa.tecnico.hdsledger.utilities;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class ProcessConfig {
    public ProcessConfig() {}

    public enum Persona {
        @SerializedName("REGULAR")
        REGULAR,
        @SerializedName("SLOW")
        SLOW,
        @SerializedName("DROP")
        DROP,
        @SerializedName("WRONG_VALUE")
        WRONG_VALUE,
        @SerializedName("WRONG_COMMIT")
        WRONG_COMMIT,
        @SerializedName("ROUND_CHANGE")
        ROUND_CHANGE
    }

    private String hostname;

    private String id;

    private int port;

    private int clientPort; // port to listen for client requests

    private String pubKeyPath;

    private String privKeyPath;

    private double minFee = 0; // minimum fee per transaction

    private double maxFee = Double.MAX_VALUE; // maximum fee per transaction

    private double feeThreshold = 0; // fee threshold to build a block

    private float minFeeAmountRatio = 0; // minimum fee amount ratio, (e.g., if 0.1, fee needs to be 10% or more of the amount)

    private Persona persona;

    private Map<String, String> additionalInfo;

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

    public double getMinFee() {
        return minFee;
    }

    public double getMaxFee() {
        return maxFee;
    }

    public double getFeeThreshold() {
        return feeThreshold;
    }

    public float getMinFeeAmountRatio() {
        return minFeeAmountRatio;
    }

    public Persona getPersona() {
        return persona;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

}
