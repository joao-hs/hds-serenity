package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.BlockchainResponse;
import pt.ulisboa.tecnico.hdsledger.communication.ClientResponse;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.builder.BlockchainRequestBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.personas.RegularLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class Blockchain {
    private ProcessConfig clientConfig;
    private Integer N_nodes;
    private Integer F_nodes;
    private Map<String, TreeSet<Integer>> freshness = new TreeMap<>();
    private Map<Integer, List<ClientResponse>> responses = new HashMap<>();
    private LinkWrapper link;

    public Blockchain(ProcessConfig clientConfig, ProcessConfig[] nodesConfig) {
        this.clientConfig = clientConfig;
        this.N_nodes = nodesConfig.length;
        this.F_nodes = Math.floorDiv(N_nodes - 1, 3);
        this.link = new RegularLinkWrapper(clientConfig, clientConfig.getPort(), nodesConfig, BlockchainResponse.class);
    }

    private Pair<String, Integer> getFreshness() {
        Integer nonce = new Random().nextInt();
        String currentTimestamp = Timestamp.getCurrentTimestamp();
        // remove old entries
        if (!freshness.keySet().stream().anyMatch(timestamp -> !Timestamp.sameWindow(timestamp, currentTimestamp))) {
            freshness.clear();
        }
        freshness.putIfAbsent(currentTimestamp, new TreeSet<Integer>());
        // make sure the nonce is actually a nonce
        while (!freshness.get(currentTimestamp).add(nonce)) {
            nonce = new Random().nextInt();
        }
        return Pair.of(currentTimestamp, nonce);
    }

    private ClientResponse responseMajority(int messageId) {
        if (!responses.containsKey(messageId)) {
            return null;
        }
        Map<ClientResponse, Integer> responseHistogram = new HashMap<>();
        for (ClientResponse response : responses.get(messageId)) {
            responseHistogram.put(response, responseHistogram.getOrDefault(response, 0) + 1);
        }

        System.out.println(MessageFormat.format("{0} - Response Histogram: {1}", clientConfig.getId(), String.join(", ", responseHistogram.entrySet().toString())));

        return responseHistogram.entrySet().stream()
                .filter(entry -> entry.getValue() > 2 * F_nodes).map(Map.Entry::getKey)
                .findFirst() // there can only be one response with more than 2F votes
                .orElse(null);
    }

    /*
     * Starts listening for responses from the blockchain
     */
    public void start() {
        System.out.println(MessageFormat.format("{0} - Starting Blockchain", clientConfig.getId()));

        new Thread(() -> {
            try {
                while (true) {
                    // Wait for a message
                    Message message = link.receive();

                    // New thread to handle the message
                    new Thread(() -> {
                        switch (message.getType()) {
                            case IGNORE ->
                                System.out.println(MessageFormat.format("{0} - Received IGNORE message from {1}",
                                        clientConfig.getId(), message.getSenderId()));

                            case ACK ->
                                System.out.println(MessageFormat.format("{0} - Received ACK message from {1}",
                                        clientConfig.getId(), message.getSenderId()));
                            
                            case BALANCE_RESPONSE -> {
                                System.out.println(MessageFormat.format("{0} - Received Balance Response from {1}",
                                        clientConfig.getId(), message.getSenderId()));
                                BalanceResponse response = ((BlockchainResponse) message).deserializeBalanceResponse();
                                System.out.println(MessageFormat.format("{0} - BalanceResponse<{1},{2},{3}>, hash: {4}", clientConfig.getId(), response.getStatus().name(), response.getTarget(), String.valueOf(response.getBalance()), String.valueOf(response.hashCode())));
                                responses.putIfAbsent(message.getMessageId(), new LinkedList<>());
                                responses.get(message.getMessageId()).add(response);
                                BalanceResponse majorityResponse = (BalanceResponse) responseMajority(message.getMessageId());
                                if (majorityResponse != null) {
                                    System.out.println(MessageFormat.format("{0} - Majority Balance Response from {1}",
                                            clientConfig.getId(), message.getSenderId()));
                                    System.out.println(MessageFormat.format("{0} - Balance of {1} is {2}",
                                            clientConfig.getId(), majorityResponse.getTarget(), majorityResponse.getBalance()));
                                }
                            }

                            case TRANSFER_RESPONSE -> {
                                System.out.println(MessageFormat.format("{0} - Received Transfer Response from {1}",
                                        clientConfig.getId(), message.getSenderId()));
                                TransferResponse response = ((BlockchainResponse) message).deserializeTransferResponse();
                                responses.putIfAbsent(message.getMessageId(), new LinkedList<>());
                                responses.get(message.getMessageId()).add(response);
                                TransferResponse majorityResponse = (TransferResponse) responseMajority(message.getMessageId());
                                if (majorityResponse != null) {
                                    System.out.println(MessageFormat.format("{0} - Majority Transfer Response from {1}",
                                            clientConfig.getId(), message.getSenderId()));
                                    System.out.println(MessageFormat.format("{0} - Transfer Response: {1}",
                                            clientConfig.getId(), majorityResponse.getStatus().name()));
                                }
                            }

                            default ->
                                System.out.println(MessageFormat.format("{0} - Received unknown message from {1}",
                                        clientConfig.getId(), message.getSenderId()));

                        }
                    }).start();

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    public void transfer(String receiver, int amount, int fee) {
        System.out.println(
                MessageFormat.format("{0} - Requesting Transfer {1} to {2}", clientConfig.getId(), amount, receiver));
        Pair<String, Integer> freshness = getFreshness();
        link.broadcastClientPort(new BlockchainRequestBuilder(clientConfig.getId(), Message.Type.TRANSFER)
            .setSerializedRequest(
                new TransferRequest(
                    clientConfig.getId(), 
                    receiver,
                    amount,
                    fee,
                    freshness.getLeft(), // timestamp
                    freshness.getRight() // nonce
                ).toJson()
            ).build()
        );
    }

    public void balance(String target) {
        System.out.println(MessageFormat.format("{0} - Requesting Balance of {1}", clientConfig.getId(), target));

        link.broadcastClientPort(new BlockchainRequestBuilder(clientConfig.getId(), Message.Type.BALANCE)
            .setSerializedRequest(
                new BalanceRequest(target).toJson()
            ).build()
        );
    }
}
