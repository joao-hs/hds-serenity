package pt.ulisboa.tecnico.hdsledger.communication.client;

public class TransferResponse extends ClientResponse {

    public TransferResponse(Status status) {
        this.setStatus(status);
    }

    @Override
    public int hashCode() {
        return getStatus().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TransferResponse) {
            TransferResponse other = (TransferResponse) obj;
            return getStatus().equals(other.getStatus());
        }
        return false;
    }
}
