package pt.ulisboa.tecnico.hdsledger.communication.client;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ClientMessageInterface;

public abstract class ClientResponse implements ClientMessageInterface {
    public enum Status {
        // General
        OK,
        // TransferResponse
        INSUFFICIENT_FUNDS,
        NO_AMOUNT,
        NO_FEE,
        BAD_DESTINATION,
        BAD_SOURCE,
        BAD_SIGNATURE,
        BAD_FRESHNESS,
        NOT_FRESH,
        // BalanceResponse
        ACCOUNT_NOT_FOUND,
    }

    private Status status;

    private String clientRequestHash;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getClientRequestHash() {
        return clientRequestHash;
    }

    public void setClientRequestHash(String clientRequestHash) {
        this.clientRequestHash = clientRequestHash;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    // methods to be implemented by subclasses
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    };

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    };
}
