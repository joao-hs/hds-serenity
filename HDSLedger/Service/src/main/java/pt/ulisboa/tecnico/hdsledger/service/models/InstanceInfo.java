package pt.ulisboa.tecnico.hdsledger.service.models;


import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private String preparedSerializedValue;
    private CommitMessage commitMessage;
    private String serializedValue;
    private int committedRound = -1;

    public InstanceInfo(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public InstanceInfo(int currentRound, String serializedValue) {
        this.currentRound = currentRound;
        this.serializedValue = serializedValue;
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

    public int getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public String getPreparedSerializedValue() {
        return preparedSerializedValue;
    }

    public void setPreparedValue(String preparedValue) {
        this.preparedSerializedValue = preparedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

    public void setSerializedValue(String value) {
        this.serializedValue = value;
    }

    public int getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(int committedRound) {
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
