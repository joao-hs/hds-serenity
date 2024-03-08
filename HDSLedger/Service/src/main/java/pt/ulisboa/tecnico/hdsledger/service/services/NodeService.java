package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.Pair;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChange;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.service.models.ProgressIndicator;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeService implements UDPService {

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    // Current node is leader
    private final ProcessConfig config;

    // Link to communicate with nodes
    private final LinkWrapper nodeLink;

    // Link to communicate with clients
    private final LinkWrapper clientLink;

    // Consensus instance -> Round -> List of prepare messages
    private final MessageBucket prepareMessages;
    // Consensus instance -> Round -> List of commit messages
    private final MessageBucket commitMessages;
    // Consensus instance -> Round -> List of round change messages
    private final MessageBucket roundChangeMessages;
    // Store if already received pre-prepare for a given <consensus, round>
    private final Map<Integer, Map<Integer, Boolean>> receivedPrePrepare = new ConcurrentHashMap<>();
    // Consensus instance information per consensus instance
    private final Map<Integer, InstanceInfo> instanceInfo = new ConcurrentHashMap<>();
    // Current consensus instance
    private final AtomicInteger consensusInstance = new AtomicInteger(0);
    // Last decided consensus instance
    private final AtomicInteger lastDecidedConsensusInstance = new AtomicInteger(0);
    // Timers
    private final Map<Integer, Thread> timers = new ConcurrentHashMap<>();

    // Progress indicator
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    // Changing round
    private final AtomicBoolean stopTimeout = new AtomicBoolean(true);

    // Ledger (for now, just a list of strings)
    private ArrayList<String> ledger = new ArrayList<String>();

    public NodeService(LinkWrapper nodeLink, LinkWrapper clientLink, ProcessConfig config, ProcessConfig[] nodesConfig) {

        this.nodeLink = nodeLink;
        this.clientLink = clientLink;
        this.config = config;
        this.nodesConfig = nodesConfig;

        this.prepareMessages = new MessageBucket(nodesConfig.length);
        this.commitMessages = new MessageBucket(nodesConfig.length);
        this.roundChangeMessages = new MessageBucket(nodesConfig.length);
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public ArrayList<String> getLedger() {
        return this.ledger;
    }

    private boolean isLeader(String id, int consensusInstance) {
        return getLeader(consensusInstance).getId().equals(id);
    }

    private ProcessConfig getLeader(int consensusInstance) {
        Integer round = this.instanceInfo.get(consensusInstance).getCurrentRound();
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Leader of consensus instance {1} is {2}", config.getId(), consensusInstance, (round % nodesConfig.length)));
        return Arrays.stream(nodesConfig)
            .filter(node -> String.valueOf(round % nodesConfig.length).equals(node.getId()))
            .findFirst().get(); // optional cannnot be empty
    }
 
    public ConsensusMessage createConsensusMessage(String value, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .build();

        return consensusMessage;
    }

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(String value) {

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(value));


        // If startConsensus was called for localConsensusInstance
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    config.getId(), localConsensusInstance));
            return;
        }

        // Only start a consensus instance if the last one was decided
        // We need to be sure that the previous value has been decided
        while (lastDecidedConsensusInstance.get() < localConsensusInstance - 1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        this.stopTimeout.set(false);
        progressIndicator.registerProgress();

        // Thread to trigger round change through timeouts
        Thread timer = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(progressIndicator.getTimeout());
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Woke up, current state:\n{1}\nCurrent Ledger: {2}",
                        config.getId(), this.instanceInfo.get(localConsensusInstance).toString(), String.join("", this.getLedger())));
                    if (stopTimeout.get()) {
                        continue;
                    }
                    if (progressIndicator.isFrozen()) {
                        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Progress is frozen, broadcasting round change",
                        config.getId()));
                        instanceInfo.get(localConsensusInstance).incrementRound();
                        this.stopTimeout.set(true);
                        progressIndicator.resetProgress();
                        this.nodeLink.broadcastPort(
                            new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                                .setRound(instanceInfo.get(localConsensusInstance).getCurrentRound())
                                .setConsensusInstance(localConsensusInstance)
                                .build());
                    }
                } catch (InterruptedException e) {
                    if (stopTimeout.get()) {
                        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Stopped timer for consensus instance {1}", config.getId(), localConsensusInstance));
                        return;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        });
        timers.put(localConsensusInstance, timer);
        timer.start();
        
        // Leader broadcasts PRE-PREPARE message
        if (isLeader(config.getId(), localConsensusInstance)) {
            InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);
            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));
            this.nodeLink.broadcastPort(this.createConsensusMessage(value, localConsensusInstance, instance.getCurrentRound()));
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }
    }

    private Optional<Pair<Integer, String>> highestPrepared(List<RoundChange> quorumRoundChange) {
        Optional<RoundChange> roundChange = quorumRoundChange.stream()
            .filter(rc -> rc.getLastPreparedRound() != null)
            .max(Comparator.comparing(rc -> rc.getLastPreparedRound()));
        if (roundChange.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pair.of(roundChange.get().getLastPreparedRound(), roundChange.get().getLastPreparedValue()));
    }

    private boolean justifyPrePrepare(int instanceId, int round) {
        return (round == 1) ||
                roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                        .allMatch(roundChange ->
                                roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedValue() == null) ||
                highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of(-1, ""))
                        .getRight().equals(prepareMessages.hasValidPrepareQuorum(config.getId(), instanceId, round).orElse(null));
    }

    private boolean justifyRoundChange(int instanceId, InstanceInfo instanceInfo, List<RoundChange> quorumRoundChange) {
        int round = instanceInfo.getCurrentRound();
        return roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                .allMatch(roundChange ->
                        roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedValue() == null) ||
                highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of(-1, ""))
                        .getRight().equals(prepareMessages.hasValidPrepareQuorum(config.getId(), instanceId, round).orElse(null));

    }

    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();
        int senderMessageId = message.getMessageId();

        PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();

        String value = prePrepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Verify if pre-prepare was sent by leader
        if (!isLeader(senderId, round) || !justifyPrePrepare(consensusInstance, round)) {
            if (!isLeader(senderId, round)) LOGGER.log(Level.INFO, MessageFormat.format("{0} - Not leader, ignoring PRE-PREPARE message", config.getId()));
            if (!justifyPrePrepare(consensusInstance, round)) LOGGER.log(Level.INFO, MessageFormat.format("{0} - Not justified, ignoring PRE-PREPARE message", config.getId()));
            return;
        }

        // Set instance value
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, value));

        
        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        receivedPrePrepare.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        if (receivedPrePrepare.get(consensusInstance).put(round, true) != null) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));
        }

        this.stopTimeout.set(false);
        progressIndicator.registerProgress();

        PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                .setConsensusInstance(consensusInstance)
                .setRound(round)
                .setMessage(prepareMessage.toJson())
                .setReplyTo(senderId)
                .setReplyToMessageId(senderMessageId)
                .build();

        this.nodeLink.broadcastPort(consensusMessage);
    }

    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public synchronized void uponPrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        PrepareMessage prepareMessage = message.deserializePrepareMessage();

        String value = prepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Doesn't add duplicate messages
        prepareMessages.addMessage(message);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, value));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setMessage(instance.getCommitMessage().toJson())
                    .build();

            nodeLink.sendPort(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<String> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {

            this.stopTimeout.set(false);
            progressIndicator.registerProgress();

            instance.setPreparedValue(preparedValue.get());
            instance.setPreparedRound(round);

            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = prepareMessages.getMessages(consensusInstance, round)
                    .values();

            CommitMessage c = new CommitMessage(preparedValue.get());
            instance.setCommitMessage(c);

            sendersMessage.forEach(senderMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(senderMessage.getSenderId())
                        .setReplyToMessageId(senderMessage.getMessageId())
                        .setMessage(c.toJson())
                        .build();

                nodeLink.sendPort(senderMessage.getSenderId(), m);
            });
        }
    }



    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public synchronized void uponCommit(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        commitMessages.addMessage(message);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), consensusInstance, round);
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }

        Optional<String> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);

        if (commitValue.isPresent() && instance.getCommittedRound() < round) {

            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String value = commitValue.get();

            // Append value to the ledger (must be synchronized to be thread-safe)
            synchronized(ledger) {

                // Increment size of ledger to accommodate current instance
                ledger.ensureCapacity(consensusInstance);
                while (ledger.size() < consensusInstance - 1) {
                    ledger.add("");
                }
                
                ledger.add(consensusInstance - 1, value);
                
                LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Current Ledger: {1}",
                            config.getId(), String.join("", ledger)));
            }

            lastDecidedConsensusInstance.getAndIncrement();

            this.stopTimeout.set(true);
            this.timers.get(consensusInstance).interrupt();
            
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            config.getId(), consensusInstance, round, true));
        }
    }


    public synchronized void uponAppend(BlockchainRequest message) {
        startConsensus(message.getMessage()); // if the node is not the leader, it won't start a consensus, it will just store the request
    }

    /*
     * Handle round change request -> ROUND-CHANGE(consensusInstanceId, roundToChangeTo, lastPreparedRound?, lastPreparedValue?) 
     *                                                                                      [? means it can be null]
     * upon receiving f+1 round change requests, the node will broadcast a round change request
     *    why: at least one correct node is asking to changing round
     *    ROUND-CHANGE(consensusInstanceId, min(roundToChangeTo), lastPreparedRound?, lastPreparedValue?) -- last two values are associated with the second
     * upon receiving 2f+1 round change requests, 
     *    why: a majority of correct nodes are asking to change round
     *    StartConsensus(consensusInstanceId, )
     */
    public synchronized void uponRoundChangeRequest(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String value;

        roundChangeMessages.addMessage(message);

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received ROUND_CHANGE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received ROUND_CHANGE message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), consensusInstance, round);
            return;
        }

        // If this process already decided on a value for this instance, so this process has received a Quorum of CommitMessages
        if (instance.getCommittedRound() != -1) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already decided on Consensus Instance {1}, Round {2}, sending commit messages to sender",
                            config.getId(), consensusInstance, round));
            commitMessages.getMessages(consensusInstance, instance.getCommittedRound()).values().forEach(commitMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(message.getSenderId())
                        .setReplyToMessageId(commitMessage.getMessageId())
                        .setMessage(instance.getCommitMessage().toJson())
                        .build();
                nodeLink.sendPort(message.getSenderId(), m);
            });
            
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCurrentRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received ROUND_CHANGE message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }

        List<RoundChange> roundChangeList = roundChangeMessages.getRoundChangeMessages(consensusInstance, round);

        if(roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round) 
            && isLeader(this.config.getId(), round) && justifyRoundChange(consensusInstance, instance, roundChangeList)){
            
            Optional<Pair<Integer, String>> highestPrepared = highestPrepared(roundChangeList);
            
            value = highestPrepared.orElse(Pair.of((Integer) null, instance.getInputValue())).getRight();

            startConsensus(value);
            return;
        }

        Optional<RoundChange> roundChangeSetValue = roundChangeMessages.hasValidRoundChangeSet(config.getId(),
                consensusInstance, round);
        if (roundChangeSetValue.isPresent()) {

            instance.setCurrentRound(roundChangeSetValue.get().getRound()); // r_i <- r_min

            this.stopTimeout.set(false); // set timer to running

            // create <ROUND-CHANGE, lambda_i, r_i, pr_i, pv_i>
            RoundChange rc = new RoundChange(consensusInstance, instance.getCurrentRound(), instance.getPreparedRound(), instance.getPreparedValue());

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(instance.getCurrentRound())
                    .setMessage(rc.toJson())
                    .build();
            
            nodeLink.broadcastPort(m);
            return;
        }
    }

    @Override
    public void listen() {
        try {
            // Thread to listen nodes
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = nodeLink.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case PRE_PREPARE ->
                                    uponPrePrepare((ConsensusMessage) message);


                                case PREPARE ->
                                    uponPrepare((ConsensusMessage) message);


                                case COMMIT ->
                                    uponCommit((ConsensusMessage) message);

                                case ROUND_CHANGE ->
                                    uponRoundChangeRequest((ConsensusMessage) message);


                                case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            config.getId(), message.getSenderId()));

                                case IGNORE ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();

            // Thread to listen clients
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = clientLink.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {
                                case APPEND ->
                                    uponAppend((BlockchainRequest) message);

                                case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            config.getId(), message.getSenderId()));

                                case IGNORE ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));

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
