package pt.ulisboa.tecnico.hdsledger.communication;

public abstract class ClientResponse {
    public enum Status {
        // General
        OK,
        // TransferResponse
        INSUFFICIENT_FUNDS,
        BAD_DESTINATION,
        BAD_SIGNATURE,
        BAD_FRESHNESS,
        NOT_FRESH,
        // BalanceResponse
        ACCOUNT_NOT_FOUND,
    }

    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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