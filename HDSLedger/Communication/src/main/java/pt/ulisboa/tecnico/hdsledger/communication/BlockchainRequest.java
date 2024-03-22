package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;

public class BlockchainRequest extends Message {

    private String serializedRequest;

    public BlockchainRequest(String senderId, Type type) {
        super(senderId, type);
    }

    public BalanceRequest deserializeBalanceRequest() {
        return new Gson().fromJson(this.serializedRequest, BalanceRequest.class);
    }

    public TransferRequest deserializeTransferRequest() {
        return new Gson().fromJson(this.serializedRequest, TransferRequest.class);
    }

    public String getSerializedRequest() {
        return serializedRequest;
    }

    public void setSerializedRequest(String message) {
        this.serializedRequest = message;
    }
}
