package pt.ulisboa.tecnico.hdsledger.communication;


public class BlockchainResponse extends Message{
        
    public BlockchainResponse(String senderId) {
        super(senderId, Type.ACK);
    }
}
