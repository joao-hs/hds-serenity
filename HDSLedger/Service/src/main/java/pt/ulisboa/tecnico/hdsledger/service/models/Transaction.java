package pt.ulisboa.tecnico.hdsledger.service.models;

import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;

public class Transaction {
    private ClientRequest request;

    public Transaction(TransferRequest request) {
        this.request = request;
    }

    public TransferRequest getTransferRequest() {
        return (TransferRequest) request;
    }

    @Override
    public String toString() {
        return request.toString();
    }

}
