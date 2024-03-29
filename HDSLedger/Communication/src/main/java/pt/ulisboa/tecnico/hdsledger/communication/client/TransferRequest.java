package pt.ulisboa.tecnico.hdsledger.communication.client;

public class TransferRequest extends ClientRequest {
    private String sender;
    private String receiver;
    private int amount;
    private int fee;

    // this is not an idempotent request, so it MUST be resilient to replay attacks
    private String timestamp;

    public TransferRequest(String sender, String receiver, int amount, int fee, String timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.fee = fee;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getFee() {
        return fee;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
