package pt.ulisboa.tecnico.hdsledger.communication.builder;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.Message;

public class BlockchainRequestBuilder {
    private final BlockchainRequest instance;

    public BlockchainRequestBuilder(String sender, Message.Type type) {
        instance = new BlockchainRequest(sender, type);
    }

    public BlockchainRequestBuilder setSerializedRequest(String serializedRequest) {
        instance.setSerializedRequest(serializedRequest);
        return this;
    }

    public BlockchainRequest build() {
        return instance;
    }
}
