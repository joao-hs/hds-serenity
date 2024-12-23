package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.ContextMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.GetContextMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.RoundChangeMessage;

/**
 * Wrapper class for all consensus messages.
 * 
 * Relation: ConsensusMessage has a serialized 
 * PrePrepareMessage, PrepareMessage, CommitMessage or RoundChange
 */
public class ConsensusMessage extends Message {

    // Consensus instance
    private int consensusInstance;
    // Round
    private int round;
    // Who sent the previous message
    private String replyTo;
    // Id of the previous message
    private int replyToMessageId;
    // Message (PREPREPARE, PREPARE, COMMIT, ROUND_CHANGE)
    private String message;

    public ConsensusMessage(String senderId, Type type) {
        super(senderId, type);
    }

    public PrePrepareMessage deserializePrePrepareMessage() {
        return new Gson().fromJson(this.message, PrePrepareMessage.class);
    }

    public PrepareMessage deserializePrepareMessage() {
        return new Gson().fromJson(this.message, PrepareMessage.class);
    }

    public CommitMessage deserializeCommitMessage() {
        return new Gson().fromJson(this.message, CommitMessage.class);
    }

    public RoundChangeMessage deserializeRoundChangeMessage() {
        return new Gson().fromJson(this.message, RoundChangeMessage.class);
    }

    public GetContextMessage deserializeGetContextMessage() {
        return new Gson().fromJson(this.message, GetContextMessage.class);
    }

    public ContextMessage deserializeContextMessage() {
        return new Gson().fromJson(this.message, ContextMessage.class);
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }
}
