package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class TransferResponse extends ClientResponse {

    public TransferResponse(Status status) {
        this.setStatus(status);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
