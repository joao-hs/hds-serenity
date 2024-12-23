package pt.ulisboa.tecnico.hdsledger.communication;

public class Message extends SignedMessage {

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
        ROUND_CHANGE, 
        GET_CONTEXT,
        RECEIVED_CONTEXT
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

}
