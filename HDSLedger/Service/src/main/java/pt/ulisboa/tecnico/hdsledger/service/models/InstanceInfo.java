package pt.ulisboa.tecnico.hdsledger.service.models;


import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private ConsensusValue preparedValue;
    private CommitMessage commitMessage;
    private ConsensusValue value;
    private int committedRound = -1;

    public InstanceInfo(ConsensusValue value) {
        this.value = value;
    }

    public InstanceInfo(int currentRound, ConsensusValue value) {
        this.currentRound = currentRound;
        this.value = value;
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

    public ConsensusValue getPreparedValue() {
        return preparedValue;
    }

    public void setPreparedValue(ConsensusValue preparedValue) {
        this.preparedValue = preparedValue;
    }

    public ConsensusValue getValue() {
        return value;
    }

    public void setValue(ConsensusValue value) {
        this.value = value;
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
