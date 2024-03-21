package pt.ulisboa.tecnico.hdsledger.utilities;

import java.util.Objects;

public class Transaction {
    private String sender;
    private String receiver;
    private Integer amount;
    private String timestamp;
    private Integer fee;

    public Transaction(String sender, String receiver, Integer amount, String timestamp, Integer fee) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = timestamp;
        this.fee = fee;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getFee() {
        return fee;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", amount=" + amount +
                ", timestamp='" + timestamp + '\'' +
                ", fee=" + fee +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receiver, amount, timestamp, fee);
    }
}
