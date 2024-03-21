package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class TransferResponse extends ClientResponse {

    public TransferResponse(Status status) {
        this.setStatus(status);
    }

    public String toJson() {
        return new Gson().toJson(this);
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
