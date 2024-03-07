package pt.ulisboa.tecnico.hdsledger.communication;

public class BlockchainRequest extends Message {
    //TODO: Add authentication to the message
    //TODO: Add freshness to the message

    private String message;

    public BlockchainRequest(String senderId, Type type, String message) {
        super(senderId, type);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}
