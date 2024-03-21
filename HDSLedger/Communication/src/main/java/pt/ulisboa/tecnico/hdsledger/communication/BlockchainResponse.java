package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class BlockchainResponse extends Message {

    private String serializedResponse;

    public BlockchainResponse(String senderId, Type type) {
        super(senderId, type);
    }

    public BalanceResponse deserializeBalanceResponse() {
        return new Gson().fromJson(this.serializedResponse, BalanceResponse.class);
    }

    public TransferResponse deserializeTransferResponse() {
        return new Gson().fromJson(this.serializedResponse, TransferResponse.class);
    }

    public String getSerializedResponse() {
        return serializedResponse;
    }

    public void setSerializedResponse(String serializedResponse) {
        this.serializedResponse = serializedResponse;
    }
}
