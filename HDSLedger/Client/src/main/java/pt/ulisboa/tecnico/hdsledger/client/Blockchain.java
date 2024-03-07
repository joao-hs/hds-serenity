package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.BlockchainResponse;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class Blockchain {
    private static final CustomLogger LOGGER = new CustomLogger(Blockchain.class.getName());

    private ProcessConfig clientConfig;
    private Integer N_nodes;
    private Integer F_nodes;
    private Link link;

    public Blockchain(ProcessConfig clientConfig, ProcessConfig[] nodesConfig) {
        this.clientConfig = clientConfig;
        this.N_nodes = nodesConfig.length;
        this.F_nodes = Math.floorDiv(N_nodes - 1, 3);
        this.link = new Link(clientConfig, clientConfig.getPort(), nodesConfig, BlockchainResponse.class);
    }

    /*
     * Starts listening for responses from the blockchain
     */
    public void start() {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Starting Blockchain", clientConfig.getId()));

        new Thread(() -> {
            try {
                while (true) {
                // Wait for a message
                    Message message = link.receive();
                    
                    // New thread to handle the message
                    new Thread(() -> {
                        switch (message.getType()) {
                            case IGNORE ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received IGNORE message from {1}",
                                            clientConfig.getId(), message.getSenderId()));
                            
                            case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            clientConfig.getId(), message.getSenderId()));

                            default ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received unknown message from {1}",
                                            clientConfig.getId(), message.getSenderId()));
                    
                        }
                    }).start();

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    
    }

    public void append(String message) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Appending message: {1}", clientConfig.getId(), message));
        // Multicast to F+1 nodes to ensure that at least 1 correct node receives the request
        link.multicastClientPort(new BlockchainRequest(this.clientConfig.getId(), Message.Type.APPEND, message), F_nodes + 1);
    }
}
