package pt.ulisboa.tecnico.hdsledger.communication.client;

import com.google.gson.annotations.Expose;

public class TransferRequest extends ClientRequest {
    @Expose
    private final String sender;
    @Expose
    private final String receiver;
    @Expose
    private final double amount;
    @Expose
    private final double fee;

    // this is not an idempotent request, so it MUST be resilient to replay attacks
    private String timestamp;

    public TransferRequest(String sender, String receiver, double amount, double fee, String timestamp) {
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

    public double getAmount() {
        return amount;
    }

    public double getFee() {
        return fee;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
