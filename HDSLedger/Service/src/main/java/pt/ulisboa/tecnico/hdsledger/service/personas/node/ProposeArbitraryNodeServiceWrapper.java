package pt.ulisboa.tecnico.hdsledger.service.personas.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class ProposeArbitraryNodeServiceWrapper extends NodeServiceWrapper {

    /*
     * Tries:
     * 1. Proposes a completely fabricated value
     * 2. Tampers one of the transactions with a different value
     * 3. Adds a new transaction
     */
    private int tries = 0;

    public ProposeArbitraryNodeServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        super(link, config, nodesConfig, validator);
    }

    private String fabricateValue() {
        TransferRequest request1 = new TransferRequest("C1", "C2", 5, 5, Timestamp.getCurrentTimestamp());
        TransferRequest request2 = new TransferRequest("C2", "C3", 5, 5, Timestamp.getCurrentTimestamp());
        TransferRequest request3 = new TransferRequest("C3", "C4", 5, 5, Timestamp.getCurrentTimestamp());
        List<TransferRequest> requests = new ArrayList<>();
        requests.add(request1);
        requests.add(request2);
        requests.add(request3);
        requests.stream().forEach(request -> request.sign(this.getConfig().getId(), this.getConfig().getPrivKeyPath()));
        Collection<Transaction> transactions = requests.stream().map(request -> new Transaction(request)).collect(Collectors.toList());
        Block block = new Block(transactions);
        block.sign(this.getConfig().getId(), this.getConfig().getPrivKeyPath());
        return block.toJson();
    }

    private TransferRequest tamperRequest(TransferRequest request) {
        JsonObject json = new Gson().toJsonTree(request).getAsJsonObject();
        json.add("fee", new Gson().toJsonTree(json.get("fee").getAsDouble() * 2));
        return new Gson().fromJson(json, TransferRequest.class);
    }

    @Override
    public void startConsensus(String serializedValue) {
        tries += 1;
        switch (this.tries) {
            case 1 -> {
                this.nodeService.startConsensus(fabricateValue());
            }
            case 2 -> {
                Set<Transaction> transactions = new Gson().fromJson(serializedValue, Block.class).getTransactions();
                Block block = new Block(
                    transactions.stream().findAny().map(transaction -> {
                        TransferRequest request = transaction.getTransferRequest();
                        return transactions.stream().map(t -> {
                            if (t.equals(transaction)) {
                                return new Transaction(tamperRequest(request));
                            }
                            return t;
                        }).collect(Collectors.toSet());
                    }).get()
                );
                block.sign(this.getConfig().getId(), this.getConfig().getPrivKeyPath());
                this.nodeService.startConsensus(block.toJson());
            }
            case 3 -> {
                TransferRequest newRequest = new TransferRequest("C1", "C2", 5, 5, Timestamp.getCurrentTimestamp());
                newRequest.sign(this.getConfig().getId(), this.getConfig().getPrivKeyPath());
                Transaction newTransaction = new Transaction(newRequest);
                Set<Transaction> transactions = new Gson().fromJson(serializedValue, Block.class).getTransactions();
                transactions.add(newTransaction);
                Block block = new Block(transactions);
                block.sign(this.getConfig().getId(), this.getConfig().getPrivKeyPath());
                this.nodeService.startConsensus(block.toJson());
            }
            default -> {
                this.nodeService.startConsensus(serializedValue);
            }
        }
    }

    @Override
    public boolean isConsensusValueValid(String serializedValue) {
        return true;
    }
    
}