package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.builder.BlockchainResponseBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.IClientService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class ClientService implements UDPService, IClientService {
    
    private static final CustomLogger LOGGER = new CustomLogger(ClientServiceWrapper.class.getName());

    private final Map<String, ProcessConfig> clientsConfigs;

    private final ProcessConfig config;

    private LedgerServiceWrapper ledger;

    private final LinkWrapper link;

    private final ClientServiceWrapper clientService;

    public ClientService(ClientServiceWrapper clientService ,LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        this.clientService = clientService;
        this.link = link;
        this.config = config;
        this.clientsConfigs = Arrays.stream(clientsConfigs).collect(
            Collectors.toMap(ProcessConfig::getId, c -> c));
    }

    public ProcessConfig getConfig() {
        return config;
    }

    public void setLedgerService(LedgerServiceWrapper ledger){
        this.ledger = ledger;
    }

    public Map<String, ProcessConfig> getConfigs(){
        return clientsConfigs;
    }

    public boolean isFresh(String issuer, TransferRequest request) {
        String currentTimestamp = Timestamp.getCurrentTimestamp();
        
        return Timestamp.sameWindow(currentTimestamp, request.getTimestamp());
    }

    public boolean hasValidSignature(ClientRequest request) {
        return request.verifySignature(clientsConfigs.get(request.getCreator()).getPubKeyPath());
    }

    public boolean existsClient(String issuer) {
        return clientsConfigs.containsKey(issuer);
    }

    public boolean isSenderValid(String issuer, TransferRequest request) {
        return request.getSender().equals(issuer);
    }

    public void uponTransfer(String issuer, TransferRequest request) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received TRANSFER request from {1}",
            config.getId(), issuer));

        if (!this.clientService.existsClient(issuer)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received request from non-existing client named {1}",
                config.getId(), issuer));
            return;
        }

        if (!this.clientService.hasValidSignature(request)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received invalid signature request from {1}",
                config.getId(), issuer));
            return;
        }

        if (!this.clientService.isSenderValid(issuer, request)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received invalid sender-issuer pair request from {1}",
                config.getId(), issuer));
            return;
        }

        if (!this.clientService.isFresh(issuer, request)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received non-fresh request from {1}",
                config.getId(), issuer));
            return;
        }

        TransferResponse response = ledger.transfer(request);
        link.sendPort(issuer, new BlockchainResponseBuilder(config.getId(), Message.Type.TRANSFER_RESPONSE)
            .setSerializedResponse(response.toJson())
            .build()
        );
    }

    public void uponBalance(String issuer, BalanceRequest request) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received BALANCE request from {1}",
            config.getId(), issuer));

        if (!this.clientService.existsClient(issuer)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received request from non-existing client named {1}",
                config.getId(), issuer));
            return;
        }

        if (!this.clientService.hasValidSignature(request)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received invalid signature request from {1}",
                config.getId(), issuer));
            return;
        }
        
        BalanceResponse response = ledger.balance(request);
        link.sendPort(issuer, new BlockchainResponseBuilder(config.getId(), Message.Type.BALANCE_RESPONSE)
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
                                    this.clientService.uponTransfer(request.getSenderId(), ((BlockchainRequest) request).deserializeTransferRequest());
                                case BALANCE ->
                                    this.clientService.uponBalance(request.getSenderId(), ((BlockchainRequest) request).deserializeBalanceRequest());
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
