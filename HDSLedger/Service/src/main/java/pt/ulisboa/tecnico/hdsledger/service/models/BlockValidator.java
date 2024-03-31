package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class BlockValidator implements ValueValidator {
    private final static CustomLogger LOGGER = new CustomLogger(BlockValidator.class.getName());

    private Map<String, String> externalPublicKeys;
    private Map<String, String> internalPublicKeys;

    public BlockValidator(ProcessConfig[] clientsConfig, ProcessConfig[] serversConfig) {
        this.externalPublicKeys = Arrays.stream(clientsConfig).collect(
            ConcurrentHashMap::new,
            (map, client) -> map.put(client.getId(), client.getPubKeyPath()),
            ConcurrentHashMap::putAll
        );
        this.internalPublicKeys = Arrays.stream(serversConfig).collect(
            ConcurrentHashMap::new,
            (map, server) -> map.put(server.getId(), server.getPubKeyPath()),
            ConcurrentHashMap::putAll
        );
    }

    
    @Override
    public boolean validate(String serializedValue) {
        // Consensus only needs to verify that the value is externally provided
        // We do that by checking if all transactions inside the block are signed by the creator (a client)

        LOGGER.log(Level.INFO, "Validating block: " + serializedValue);
        Block block = new Gson().fromJson(serializedValue, Block.class);
        if (block == null || block.getCreator() == null) {
            LOGGER.log(Level.INFO, "Block is null or creator is null");
            return false;
        }

        if (!block.verifySignature(internalPublicKeys.get(block.getCreator()))) {
            LOGGER.log(Level.INFO, "Block signature is invalid");
            return false;
        }

        LOGGER.log(Level.INFO, "Checking if transactions signatures are from external entities");
        Set<Transaction> transactions = block.getTransactions();
        return transactions.stream().allMatch(
            transaction -> transaction.getTransferRequest().verifySignature(
                this.externalPublicKeys.get(transaction.getTransferRequest().getCreator())
            )
        );

    }
}
