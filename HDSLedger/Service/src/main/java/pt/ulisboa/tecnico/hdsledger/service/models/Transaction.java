package pt.ulisboa.tecnico.hdsledger.service.models;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;

public class Transaction {
    @Expose
    private String serializedRequest;

    public Transaction(ClientRequest request) {
        this.serializedRequest = request.toJson();
    }

    public TransferRequest getTransferRequest() {
        return new Gson().fromJson(serializedRequest, TransferRequest.class);
    }

    @Override
    public String toString() {
        return serializedRequest.toString();
    }

}
