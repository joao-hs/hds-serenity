package pt.ulisboa.tecnico.hdsledger.communication.client;

public class TransferRequest extends ClientRequest {
    private final String sender;
    private final String receiver;
    private final int amount;
    private final int fee;

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

    public String getReceiver() {
        return receiver;
    }

    public int getAmount() {
        return amount;
    }

    public int getFee() {
        return fee;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
