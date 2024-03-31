package pt.ulisboa.tecnico.hdsledger.service.models;


import java.security.NoSuchAlgorithmException;

import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class InstanceInfo {

    private int currentRound = 1;
    private Integer preparedRound = null;
    private String preparedSerializedHashValue;
    private CommitMessage commitMessage;
    private String mySerializedValue;
    private String serializedValue;
    private String valueHash;
    private Integer committedRound = null;

    public InstanceInfo(String serializedValue) {
        this.mySerializedValue = serializedValue;
    }

    public InstanceInfo(int currentRound, String serializedValue, boolean isHash) {
        this.currentRound = currentRound;
        if (isHash) {
            this.valueHash = serializedValue;
        }
        else {
            this.serializedValue = serializedValue;
            this.setValueHash();
        }
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public void incrementRound() {
        currentRound++;
    }

    public Integer getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(Integer preparedRound) {
        this.preparedRound = preparedRound;
    }

    public String getPreparedSerializedHashValue() {
        return preparedSerializedHashValue;
    }

    public void setPreparedHashValue(String preparedValue) {
        this.preparedSerializedHashValue = preparedValue;
    }

    public String getMySerializedValue() {
        return mySerializedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

    public String getValueHash() {
        return valueHash;
    }

    public void setValueHash() {
        try {
            this.valueHash = RSAEncryption.digest(serializedValue);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setSerializedValue(String value) {
        this.serializedValue = value;
    }

    public Integer getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(Integer committedRound) {
        this.committedRound = committedRound;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(CommitMessage commitMessage) {
        this.commitMessage = commitMessage;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
