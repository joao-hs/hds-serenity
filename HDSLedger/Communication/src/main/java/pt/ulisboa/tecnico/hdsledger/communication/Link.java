package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.Message.Type;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.LinkInterface;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class Link implements LinkInterface {

    private static final CustomLogger LOGGER = new CustomLogger(Link.class.getName());
    // Time to wait for an ACK before resending the message
    private final int BASE_SLEEP_TIME;
    // UDP Socket
    private final DatagramSocket socket;
    // Map of all nodes in the network
    private final Map<String, ProcessConfig> nodes = new ConcurrentHashMap<>();
    // Reference to the node itself
    private final ProcessConfig config;
    // Class to deserialize messages to
    private final Class<? extends Message> messageClass;
    // Set of received messages from specific node (prevent duplicates)
    private final Map<String, CollapsingSet> receivedMessages = new ConcurrentHashMap<>();
    // Set of received ACKs from specific node
    private final CollapsingSet receivedAcks = new CollapsingSet();
    // Message counter
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    // Send messages to self by pushing to queue instead of through the network
    private final Queue<Message> localhostQueue = new ConcurrentLinkedQueue<>();

    // Link wrapper to have behavior of nodes expressed in Links
    // Need to pass-throught to link to make sure that the link interacts
    // with himself through the wrapper
    private LinkWrapper linkWrapper;


    public Link(LinkWrapper linkWrapper, ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        this(linkWrapper, self, port, nodes, messageClass, false, 200);
    }

    public Link(LinkWrapper linkWrapper, ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
            boolean activateLogs, int baseSleepTime) {
        this.linkWrapper = linkWrapper;
        this.config = self;
        this.messageClass = messageClass;
        this.BASE_SLEEP_TIME = baseSleepTime;

        Arrays.stream(nodes).forEach(node -> {
            String id = node.getId();
            this.nodes.put(id, node);
            receivedMessages.put(id, new CollapsingSet());
        });

        try {
            this.socket = new DatagramSocket(port, InetAddress.getByName(config.getHostname()));
        } catch (UnknownHostException | SocketException e) {
            throw new HDSSException(ErrorMessage.CannotOpenSocket);
        }
        if (!activateLogs) {
            LogManager.getLogManager().reset();
        }
    }

    public void ackAll(List<Integer> messageIds) {
        receivedAcks.addAll(messageIds);
    }

    /**
     * Broadcasts a message to all nodes in the network
     *
     * @param data The message to be broadcasted
     */
    public void broadcastPort(Message data) {
        Gson gson = new Gson();
        nodes.forEach((destId, dest) -> this.linkWrapper.sendPort(destId, gson.fromJson(data.toJson(), data.getClass())));
    }

    public void broadcastClientPort(Message data) {
        Gson gson = new Gson();
        nodes.forEach((destId, dest) -> this.linkWrapper.sendClientPort(destId, gson.fromJson(data.toJson(), data.getClass())));
    }

    public void sendPort(String nodeId, Message data) {
        ProcessConfig node = nodes.get(nodeId);
        if (node == null)
            throw new HDSSException(ErrorMessage.NoSuchNode);

        this.linkWrapper.send(nodeId, node.getPort(), data);
    }

    public void sendClientPort(String nodeId, Message data) {
        ProcessConfig node = nodes.get(nodeId);
        if (node == null)
            throw new HDSSException(ErrorMessage.NoSuchNode);

        this.linkWrapper.send(nodeId, node.getClientPort(), data);
    }

    public void send(String nodeId, int destPort, Message data) {
        this.linkWrapper.send(nodeId, destPort, data, true);
    }

    /**
     * Sends a message to a specific node with guarantee of delivery
     *
     * @param nodeId The node identifier
     *
     * @param data The message to be sent
     */
    public void send(String nodeId, int destPort, Message data, boolean sign) {

        // Spawn a new thread to send the message
        // To avoid blocking while waiting for ACK
        new Thread(() -> {
            try {
                ProcessConfig node = nodes.get(nodeId);
                if (node == null)
                    throw new HDSSException(ErrorMessage.NoSuchNode);

                data.setMessageId(messageCounter.getAndIncrement());

                // If the message is not ACK, it will be resent
                InetAddress destAddress = InetAddress.getByName(node.getHostname());
                int count = 1;
                int messageId = data.getMessageId();
                int sleepTime = BASE_SLEEP_TIME;

                // Send message to local queue instead of using network if destination in self
                if (nodeId.equals(this.config.getId())) {
                    this.localhostQueue.add(data);

                    LOGGER.log(Level.INFO,
                            MessageFormat.format("{0} - Message {1} (locally) sent to {2}:{3} successfully",
                                    config.getId(), data.getType(), destAddress, destPort));

                    return;
                }

                for (;;) {
                    LOGGER.log(Level.INFO, MessageFormat.format(
                            "{0} - Sending {1} message to {2}:{3} with message ID {4} - Attempt #{5}", config.getId(),
                            data.getType(), destAddress, destPort, messageId, count++));

                    this.linkWrapper.unreliableSend(destAddress, destPort, data, sign);

                    // Wait (using exponential back-off), then look for ACK
                    Thread.sleep(sleepTime);

                    // Receive method will set receivedAcks when sees corresponding ACK
                    if (receivedAcks.contains(messageId))
                        break;

                    sleepTime <<= 1;
                }

                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Message {1} sent to {2}:{3} successfully",
                        config.getId(), data.getType(), destAddress, destPort));
            } catch (InterruptedException | UnknownHostException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Sends a message to a specific node without guarantee of delivery
     * Mainly used to send ACKs, if they are lost, the original message will be
     * resent
     *
     * @param address The address of the destination node
     *
     * @param port The port of the destination node
     *
     * @param data The message to be sent
     */
    public void unreliableSend(InetAddress hostname, int port, Message data, boolean sign) {
        new Thread(() -> {
            try {
                if (sign) {
                    // Signing the message
                    String jsonData = data.toSignable();
                    String signature;
    
                    try {
                        signature = RSAEncryption.sign(jsonData, config.getPrivKeyPath());
                    } catch (Exception e) {
                        throw new HDSSException(ErrorMessage.SigningMessageError);
                    }
    
                    data.setSignature(signature);
                }

                byte[] buf = data.toJson().getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, hostname, port);

                socket.send(packet);

            } catch (IOException e) {
                e.printStackTrace();
                throw new HDSSException(ErrorMessage.SocketSendingError);
            }
        }).start();
    }

    /*
     * Receives a message from any node in the network (blocking)
     */
    public Message receive() throws IOException, ClassNotFoundException {

        Message message = null;
        String serialized = "";
        Boolean local = false;
        DatagramPacket response = null;
        Gson gson = new Gson();
        
        // If there are messages in the local queue, receive those first
        if (this.localhostQueue.size() > 0) {
            message = this.localhostQueue.poll();
            local = true; 
            this.receivedAcks.add(message.getMessageId());
        } else {
            byte[] buf = new byte[65535];
            response = new DatagramPacket(buf, buf.length);

            socket.receive(response);

            byte[] buffer = Arrays.copyOfRange(response.getData(), 0, response.getLength());
            serialized = new String(buffer);

            message = gson.fromJson(serialized, Message.class);
            switch (message.getType()) {
                case TRANSFER, BALANCE -> {
                    message = gson.fromJson(serialized, BlockchainRequest.class);
                }
                case TRANSFER_RESPONSE, BALANCE_RESPONSE -> {
                    message = gson.fromJson(serialized, BlockchainResponse.class);
                }
                case PRE_PREPARE, PREPARE, COMMIT, ROUND_CHANGE -> {
                    message = gson.fromJson(serialized, ConsensusMessage.class);
                }
                default -> {}
            }

            if (!RSAEncryption.verifySignature(message.toSignable(),
                message.getSignature(),
                nodes.get(message.getSenderId()).getPubKeyPath())) {

                message.setType(Message.Type.IGNORE);

                LOGGER.log(Level.INFO, MessageFormat.format("Invalid signature from node {0}:{1}",
                    InetAddress.getByName(response.getAddress().getHostName()), response.getPort()));

                return message;
            }

        }

        String senderId = message.getSenderId();
        int messageId = message.getMessageId();

        if (!nodes.containsKey(senderId))
            throw new HDSSException(ErrorMessage.NoSuchNode);

        // Handle ACKS, since it's possible to receive multiple acks from the same
        // message
        if (message.getType().equals(Message.Type.ACK)) {
            receivedAcks.add(messageId);
            return message;
        }

        // It's not an ACK -> Deserialize for the correct type
        if (!local)
            message = gson.fromJson(message.toJson(), this.messageClass);

        boolean isRepeated = !receivedMessages.get(message.getSenderId()).add(messageId);
        Type originalType = message.getType();
        // Message already received (add returns false if already exists) => Discard
        if (isRepeated) {
            message.setType(Message.Type.IGNORE);
        }

        switch (message.getType()) {
            case PRE_PREPARE -> {
                return message;
            }
            case IGNORE -> {
                if (!originalType.equals(Type.COMMIT))
                    return message;
            }
            case PREPARE -> {
                ConsensusMessage consensusMessage = (ConsensusMessage) message;
                if (consensusMessage.getReplyTo() != null && consensusMessage.getReplyTo().equals(config.getId()))
                    receivedAcks.add(consensusMessage.getReplyToMessageId());

                return message;
            }
            case COMMIT -> {
                ConsensusMessage consensusMessage = (ConsensusMessage) message;
                if (consensusMessage.getReplyTo() != null && consensusMessage.getReplyTo().equals(config.getId()))
                    receivedAcks.add(consensusMessage.getReplyToMessageId());
            }
            default -> {}
        }

        if (!local) {
            InetAddress address = InetAddress.getByName(response.getAddress().getHostAddress());
            int port = response.getPort();

            Message responseMessage = new Message(this.config.getId(), Message.Type.ACK);
            responseMessage.setMessageId(messageId);

            // ACK is sent without needing for another ACK because
            // we're assuming an eventually synchronous network
            // Even if a node receives the message multiple times,
            // it will discard duplicates
            this.linkWrapper.unreliableSend(address, port, responseMessage, true);
        }
        
        return message;
    }
}
