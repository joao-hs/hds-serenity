package pt.ulisboa.tecnico.hdsledger.communication.builder;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainResponse;
import pt.ulisboa.tecnico.hdsledger.communication.Message;

public class BlockchainResponseBuilder {
    private final BlockchainResponse instance;

    public BlockchainResponseBuilder(String sender, Message.Type type) {
        instance = new BlockchainResponse(sender, type);
    }

    public BlockchainResponseBuilder setSerializedResponse(String serializedResponse) {
        instance.setSerializedResponse(serializedResponse);
        return this;
    }

    public BlockchainResponse build() {
        return instance;
    }
}
