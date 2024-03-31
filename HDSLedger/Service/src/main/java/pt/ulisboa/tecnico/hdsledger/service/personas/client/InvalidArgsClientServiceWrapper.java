package pt.ulisboa.tecnico.hdsledger.service.personas.client;

import java.util.Random;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class InvalidArgsClientServiceWrapper extends ClientServiceWrapper {
    String fake_issuer = "";
    int amount = -100;
    String impossible_issuer;
    Random random = new Random();  

    public InvalidArgsClientServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        super(link,config,clientsConfigs);
        while(!this.fake_issuer.equals(config.getId())){
            this.fake_issuer = clientsConfigs[random.nextInt(clientsConfigs.length)].getId();
        }
        this.amount = Integer.parseInt(this.additionalInfo.getOrDefault("amount", String.valueOf(this.amount)));
        this.impossible_issuer = this.additionalInfo.getOrDefault("impossible_issuer", "K1");
        this.forgeTransferRequests();
    }

    public void forgeTransferRequests(){
        // fake issuer test
        TransferRequest request = new TransferRequest(this.fake_issuer, this.getConfig().getId(), 20,5, Timestamp.getCurrentTimestamp());
        super.uponTransfer(fake_issuer,request);

        // thrashing money to non-existent account test 
        TransferRequest request1 = new TransferRequest(this.fake_issuer, this.impossible_issuer, 20,5, Timestamp.getCurrentTimestamp());
        super.uponTransfer(fake_issuer,request1);

        // negative money transfer test
        TransferRequest request2 = new TransferRequest(this.getConfig().getId(), this.fake_issuer, this.amount,5, Timestamp.getCurrentTimestamp());
        super.uponTransfer(fake_issuer,request2);


    }

    @Override
    public synchronized void uponTransfer(String issuer,TransferRequest request) {
        forgeTransferRequests();
    }



    // All methods have the default implementation from ClientServiceWrapper: pass the call to the clientService object.
}