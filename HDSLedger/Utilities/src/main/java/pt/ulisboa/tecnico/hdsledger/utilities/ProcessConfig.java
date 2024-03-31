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
        @SerializedName("DEAF")
        DEAF,
        @SerializedName("MUTE")
        MUTE,
        @SerializedName("ARBITRARY")
        ARBITRARY,
        @SerializedName("INVARGS")
        INVARGS
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

    /*
     * Example: 
     * 1. I have 100 funds. I want to send 10 + 1 fee. My balanceFeeMargin is 1/100=0.11; my balanceAmountMargin is 11/100 = 0.11
     * 2. I have 52 funds. I want to send 30 + 21 fee. My balanceFeeMargin is 21/52=0.40; my balanceAmountMargin is 51/52 = 0.98
     */
    // minimum percentage of margin between balance and to-be deducted fee in a transaction
    private double maxBalanceFeeMargin = 0.5; // 0..0.5 -> should be conservative, otherwise validator might risk paying the fee
    
    // minimum percentage of margin between balance and total to-be deducted amount in a transaction
    private double maxBalanceAmountMargin = 1; // 0..1 -> can be more lenient, since there is no hard penalty


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

    public double getMaxBalanceFeeMargin() {
        return maxBalanceFeeMargin;
    }

    public double getMaxBalanceAmountMargin() {
        return maxBalanceAmountMargin;
    }

    public Persona getPersona() {
        return persona;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

}
