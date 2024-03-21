package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.builder.BlockchainResponseBuilder;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class ClientService implements UDPService {
    
    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());

    private final ProcessConfig[] clientsConfigs;

    private final ProcessConfig config;

    private final LedgerService ledger = LedgerService.getInstance();

    private final LinkWrapper link;

    private final Map<String, Map<String, TreeSet<Integer>>> freshness = new HashMap<>();

    public ClientService(LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        this.link = link;
        this.config = config;
        this.clientsConfigs = clientsConfigs;
    }

    public ProcessConfig getConfig() {
        return config;
    }

    private boolean isFresh(String issuer, TransferRequest request) {
        freshness.putIfAbsent(issuer, new HashMap<>());
        Map<String, TreeSet<Integer>> clientFreshness = freshness.get(issuer);
        String currentTimestamp = Timestamp.getCurrentTimestamp();

        // check if the request was sent recently
        if (!Timestamp.sameWindow(currentTimestamp, request.getTimestamp())) {
            return false;
        }
        // remove old entries
        if (!clientFreshness.containsKey(request.getTimestamp())) {
            clientFreshness.clear();
        }
        clientFreshness.putIfAbsent(request.getTimestamp(), new TreeSet<>());
        return clientFreshness.get(request.getTimestamp()).add(request.getNonce());
        
    }

    private synchronized void uponTransfer(String issuer, TransferRequest request) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received TRANSFER request from {1}",
            config.getId(), issuer));

        // TODO: Check validity of request (network-logic)

        if (!isFresh(issuer, request)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received non-fresh request from {1}",
                config.getId(), issuer));
            return;
        }

        TransferResponse response = ledger.transfer(request);
        link.sendClientPort(issuer, new BlockchainResponseBuilder(config.getId(), Message.Type.TRANSFER_RESPONSE)
            .setSerializedResponse(response.toJson())
            .build()
        );
    }

    private void uponBalance(String issuer, BalanceRequest request) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received BALANCE request from {1}",
            config.getId(), issuer));
        
        BalanceResponse response = ledger.balance(request);
        link.sendClientPort(issuer, new BlockchainResponseBuilder(config.getId(), Message.Type.BALANCE_RESPONSE)
            .setSerializedResponse(response.toJson())
            .build()
        );
    }


    @Override
    public void listen() {
        try {
            new Thread(() -> {
                try {
                    while (true) {
                        Message request = link.receive();
    
                        new Thread(() -> {
                            switch (request.getType()) {
                                case TRANSFER ->
                                    uponTransfer(request.getSenderId(), ((BlockchainRequest) request).deserializeTransferRequest());
                                case BALANCE ->
                                    uponBalance(request.getSenderId(), ((BlockchainRequest) request).deserializeBalanceRequest());
                                case IGNORE ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received IGNORE message from {1}",
                                        config.getId(), request.getSenderId()));
                                default ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received unknown message from {1}",
                                        config.getId(), request.getSenderId()));
                            }
                        }).start();
    
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
