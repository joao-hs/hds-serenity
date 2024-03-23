package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class BlockValidator implements ValueValidator {
    private Map<String, String> externalPublicKeys;

    public BlockValidator(ProcessConfig[] clientsConfig) {
        this.externalPublicKeys = Arrays.stream(clientsConfig).collect(
            ConcurrentHashMap::new,
            (map, client) -> map.put(client.getId(), client.getPubKeyPath()),
            ConcurrentHashMap::putAll
        );
    }

    
    @Override
    public boolean validate(String serializedValue) {
        // Consensus only needs to verify that the value is externally provided
        // We do that by checking if all transactions inside the block are signed by the creator (a client)

        Block block = new Gson().fromJson(serializedValue, Block.class);
        Set<Transaction> transactions = block.getTransactions();
        return transactions.stream().allMatch(
            transaction -> transaction.getTransferRequest().verifySignature(
                this.externalPublicKeys.get(transaction.getTransferRequest().getCreator())
            )
        );

    }
}
