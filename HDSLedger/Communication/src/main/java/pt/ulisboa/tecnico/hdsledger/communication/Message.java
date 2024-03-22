package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.interfaces.Signable;

public class Message extends SignedMessage implements Signable {

    // Sender identifier
    private String senderId;
    // Message identifier
    private int messageId;
    // Message type
    private Type type;

    public enum Type {
        TRANSFER, 
        TRANSFER_RESPONSE,
        BALANCE,
        BALANCE_RESPONSE,
        PRE_PREPARE,
        PREPARE,
        COMMIT,
        ACK,
        IGNORE,
        ROUND_CHANGE;
    }

    public Message(String senderId, Type type) {
        this.senderId = senderId;
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toSignable() {
        Gson gson = new GsonBuilder().setExclusionStrategies(
            new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getName().equals("signature");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            }
        ).create();
        return gson.toJson(this);
    }
}
