package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.BlockchainResponse;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;


import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.builder.BlockchainRequestBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.builder.LinkWrapperBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.ClientResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.MerkleTree;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Timestamp;

public class Blockchain {
    private ProcessConfig clientConfig;
    private Integer N_nodes;
    private Integer F_nodes;
    private Map<String, List<ClientResponse>> responses = new HashMap<>();
    private Map<String, TransferRequest> pendingTransfers = new HashMap<>();
    private LinkWrapper link;

    public Blockchain(ProcessConfig clientConfig, ProcessConfig[] nodesConfig) {
        this.clientConfig = clientConfig;
        this.N_nodes = nodesConfig.length;
        this.F_nodes = Math.floorDiv(N_nodes - 1, 3);
        this.link = new LinkWrapperBuilder(clientConfig, clientConfig.getPort(), nodesConfig, BlockchainResponse.class).build();
    }

    private ClientResponse responseMajority(String requestHash) {
        if (!responses.containsKey(requestHash)) {
            return null;
        }
        Map<Integer, Pair<ClientResponse, Integer>> responseHistogram = new HashMap<>();
        for (ClientResponse response : responses.get(requestHash)) {
            Pair<ClientResponse, Integer> pair = responseHistogram.getOrDefault(response.hashCode(), Pair.of(response, 0));
            pair = Pair.of(pair.getLeft(), pair.getRight() + 1);
            responseHistogram.put(response.hashCode(), pair);
        }

        System.out.println(MessageFormat.format("{0} - Response Histogram: {1}", clientConfig.getId(), String.join(", ", responseHistogram.entrySet().toString())));

        return responseHistogram.entrySet().stream()
                // need to have at least F + 1 votes, so that at least one correct process is voting for it
                .filter(entry -> entry.getValue().getRight() == F_nodes + 1) // to avoid duplicates, we only consider the first response with F + 1 votes
                .map(Map.Entry::getValue)
                .map(Pair::getLeft)
                .findFirst() // there can only be one response with more than 2F votes
                .orElse(null);
    }

    private boolean verifyProofOfConsensus(Collection<CommitMessage> commitMessages) {
        // ! Assuming that this proof of consensus is sent by at least one correct node
        // 1. Only one commit message per node
        Map<String, CommitMessage> commitMessagesMap = commitMessages.stream()
            .collect(Collectors.toMap(CommitMessage::getCreator, c -> c));
        
        // 2. At least F + 1 commit messages with the same value
        // Why? 
        return commitMessagesMap.values().stream()
            .collect(Collectors.groupingBy(CommitMessage::getSerializedValue, Collectors.counting()))
            .values().stream().anyMatch(votes -> votes >= 2 * F_nodes + 1);
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
                                uponBalanceResponse((BlockchainResponse) message);
                            }

                            case TRANSFER_RESPONSE -> {
                                uponTransferResponse((BlockchainResponse) message);
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

    public void balance(String target) {
        System.out.println(MessageFormat.format("{0} - Requesting Balance of {1}", clientConfig.getId(), target));

        BalanceRequest request = new BalanceRequest(target);
        request.sign(clientConfig.getId(), clientConfig.getPrivKeyPath());

        String requestHash = request.digest();

        responses.put(requestHash, new LinkedList<>());

        link.broadcastClientPort(new BlockchainRequestBuilder(clientConfig.getId(), Message.Type.BALANCE)
            .setSerializedRequest(
                request.toJson()
            ).build()
        );
    }

    public void transfer(String receiver, double amount, double fee) {
        System.out.println(
                MessageFormat.format("{0} - Requesting Transfer {1} to {2}", clientConfig.getId(), amount, receiver));
        String freshness = Timestamp.getCurrentTimestamp();
        String requestHash = "";
        TransferRequest request = new TransferRequest(
            clientConfig.getId(), 
            receiver,
            amount,
            fee,
            freshness // timestamp
        );
        request.sign(clientConfig.getId(), clientConfig.getPrivKeyPath());
        
        requestHash = request.digest();
        
        pendingTransfers.put(requestHash, request);
        responses.put(requestHash, new LinkedList<>());

        link.broadcastClientPort(new BlockchainRequestBuilder(clientConfig.getId(), Message.Type.TRANSFER)
            .setSerializedRequest(
                request.toJson()
            ).build()
        );
    }

    public synchronized void uponBalanceResponse(BlockchainResponse response) {
        System.out.println(MessageFormat.format("{0} - Received Balance Response from {1}",
            clientConfig.getId(), response.getSenderId()));
        BalanceResponse balanceResponse = ((BlockchainResponse) response).deserializeBalanceResponse();
        responses.get(balanceResponse.getClientRequestHash()).add(balanceResponse);
        BalanceResponse majorityResponse = (BalanceResponse) responseMajority(balanceResponse.getClientRequestHash());
        if (majorityResponse != null) {
            System.out.println(MessageFormat.format("{0} - Majority Balance Response from {1}",
                    clientConfig.getId(), response.getSenderId()));
            System.out.println(MessageFormat.format("Balance of {1} is {2}",
                    clientConfig.getId(), majorityResponse.getTarget(), majorityResponse.getBalance()));
        }
    }


    public synchronized void uponTransferResponse(BlockchainResponse response) {
        System.out.println(MessageFormat.format("{0} - Received Transfer Response from {1}",
            clientConfig.getId(), response.getSenderId()));
        TransferResponse transferResponse = ((BlockchainResponse) response).deserializeTransferResponse();
        responses.get(transferResponse.getClientRequestHash()).add(transferResponse);
        TransferResponse majorityResponse = (TransferResponse) responseMajority(transferResponse.getClientRequestHash());
        if (majorityResponse != null) {
            uponCorrectTransferResponse(majorityResponse);
        }
    }

    private void uponCorrectTransferResponse(TransferResponse majorityResponse) {
        // Validate response
        // 1. Is the transaction in the block?
        TransferRequest request = pendingTransfers.get(majorityResponse.getClientRequestHash());
        if (request == null) {
            System.out.println(MessageFormat.format("{0} - Transfer with hash {1} failed to find the request",
                clientConfig.getId(), majorityResponse.getClientRequestHash()));
            pendingTransfers.remove(majorityResponse.getClientRequestHash()); // TODO: explore
            return;
        }
        Pair<String, ArrayList<String>> proofOfInclusion = majorityResponse.getProofOfInclusion();
        if (majorityResponse.getGeneralStatus() == ClientResponse.GeneralStatus.SUBMITTED && !MerkleTree.verifyProof(
            request.toJson(), // ! assuming the full leaves are just transfer requests in json
            proofOfInclusion.getLeft(), // merkle root
            proofOfInclusion.getRight() // merkle sibling path to root
        )) {
            System.out.println(MessageFormat.format("{0} - Transfer of {1} to {2} failed verification of inclusion in block",
                clientConfig.getId(), request.getAmount(), request.getReceiver()));
            pendingTransfers.remove(majorityResponse.getClientRequestHash()); // TODO: explore
            return;
        }
        // 2. Was the block agreed on consensus?
        if (majorityResponse.getGeneralStatus() == ClientResponse.GeneralStatus.SUBMITTED && !verifyProofOfConsensus(majorityResponse.getProofOfConsensus())) {
            
            System.out.println(MessageFormat.format("{0} - Transfer of {1} to {2} failed verification of consensus",
                clientConfig.getId(), request.getAmount(), request.getReceiver()));
            return;
        }
        // 3. Is the block in the blockchain?
        /*
         * We trust that correct nodes will not lie about the block being in the blockchain
         * Since we have at least 1 correct node responding with this information,
         * we can conclude that the block is in the blockchain
         */
        
        System.out.println(MessageFormat.format("#### Transfer of {1} to {2} was {3} - {4}",
            clientConfig.getId(), request.getAmount(), request.getReceiver(), majorityResponse.getGeneralStatus().name(), majorityResponse.getStatus().name()));
        
        pendingTransfers.remove(majorityResponse.getClientRequestHash());
    }

}
