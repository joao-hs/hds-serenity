package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Comparator;
import java.util.Collection;
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
import pt.ulisboa.tecnico.hdsledger.communication.RoundChange;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.INodeService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.service.models.ProgressIndicator;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeService implements UDPService, INodeService {

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    // Current node is leader
    private final ProcessConfig config;

    // Link to communicate with nodes
    private final LinkWrapper link;

    private final LedgerService ledger = LedgerService.getInstance();

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
    private final Map<Integer, ProgressIndicator> progressIndicators = new ConcurrentHashMap<>();
    // Changing round
    private final Map<Integer, AtomicBoolean> stopTimeouts = new ConcurrentHashMap<>();

    public NodeService(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig) {
        this.link = link;
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
 
    public ConsensusMessage createConsensusMessage(Block block, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(block);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .build();

        return consensusMessage;
    }

    public void startTimer(int consensusInstance) {
        ProgressIndicator progressIndicator = progressIndicators.get(consensusInstance);
        AtomicBoolean stopTimeout = stopTimeouts.get(consensusInstance);
        while (true) {
            try {
                Thread.sleep(progressIndicator.getTimeout());
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Woke up, current state:\n{1}",
                    config.getId(), this.instanceInfo.get(consensusInstance).toString()));
                if (stopTimeout.get()) {
                    continue;
                }
                if (progressIndicator.isFrozen()) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Progress is frozen, broadcasting round change",
                    config.getId()));
                    instanceInfo.get(consensusInstance).incrementRound();
                    stopTimeout.set(true);
                    progressIndicator.resetProgress();
                    this.link.broadcastPort(
                        new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                            .setRound(instanceInfo.get(consensusInstance).getCurrentRound())
                            .setConsensusInstance(consensusInstance)
                            .build());
                }
            } catch (InterruptedException e) {
                if (stopTimeout.get()) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Stopped timer for consensus instance {1}", config.getId(), consensusInstance));
                    return;
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(Block block) {

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(block));


        // If startConsensus was called for localConsensusInstance
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    config.getId(), localConsensusInstance));
            return;
        }
        
        this.stopTimeouts.putIfAbsent(localConsensusInstance, new AtomicBoolean(false));
        this.progressIndicators.putIfAbsent(localConsensusInstance, new ProgressIndicator());
        this.progressIndicators.get(localConsensusInstance).registerProgress();

        // Thread to trigger round change through timeouts
        Thread timer = new Thread(() -> startTimer(localConsensusInstance));
        timers.put(localConsensusInstance, timer);
        timer.start();
        
        // Leader broadcasts PRE-PREPARE message
        if (isLeader(config.getId(), localConsensusInstance)) {
            InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);
            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));
            this.link.broadcastPort(this.createConsensusMessage(block, localConsensusInstance, instance.getCurrentRound()));
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }
    }

    private Optional<Pair<Integer, Block>> highestPrepared(List<RoundChange> quorumRoundChange) {
        Optional<RoundChange> roundChange = quorumRoundChange.stream()
            .filter(rc -> rc.getLastPreparedRound() != null)
            .max(Comparator.comparing(rc -> rc.getLastPreparedRound()));
        if (roundChange.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pair.of(roundChange.get().getLastPreparedRound(), roundChange.get().getLastPreparedBlock()));
    }

    private boolean justifyPrePrepare(int instanceId, int round) {
        return (round == 1) ||
                roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                        .allMatch(roundChange ->
                                roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedBlock() == null) ||
                highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of(-1, new Block()))
                        .getRight().equals(prepareMessages.hasValidPrepareQuorum(config.getId(), instanceId, round).orElse(null));
    }

    private boolean justifyRoundChange(int instanceId, InstanceInfo instanceInfo, List<RoundChange> quorumRoundChange) {
        int round = instanceInfo.getCurrentRound();
        return roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                .allMatch(roundChange ->
                        roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedBlock() == null) ||
                highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of(-1, new Block()))
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

        Block block = prePrepareMessage.getValue();

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
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, block));

        
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

        this.timers.putIfAbsent(consensusInstance, new Thread(() -> startTimer(consensusInstance)));
        this.stopTimeouts.get(consensusInstance).set(false);
        progressIndicators.get(consensusInstance).registerProgress();

        PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                .setConsensusInstance(consensusInstance)
                .setRound(round)
                .setMessage(prepareMessage.toJson())
                .setReplyTo(senderId)
                .setReplyToMessageId(senderMessageId)
                .build();

        this.link.broadcastPort(consensusMessage);
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

        Block block = prepareMessage.getBlock();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Doesn't add duplicate messages
        prepareMessages.addMessage(message);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, block));
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

            link.sendPort(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<Block> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {

            this.stopTimeouts.get(consensusInstance).set(false);
            progressIndicators.get(consensusInstance).registerProgress();

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

                link.sendPort(senderMessage.getSenderId(), m);
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

        Optional<Block> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);

        if (commitValue.isPresent() && instance.getCommittedRound() < round) {

            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            Block block = commitValue.get();
            
            this.stopTimeouts.get(consensusInstance).set(true);

            // all previous consensus instances must decide before the current one
            while (lastDecidedConsensusInstance.get() < consensusInstance - 1) {
                try {
                    lastDecidedConsensusInstance.wait(); // once a consensus instance is decided, notifies all threads
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Append value to the ledger (must be synchronized to be thread-safe)
            ledger.insertBlock(block);
            
            lastDecidedConsensusInstance.getAndIncrement();
            lastDecidedConsensusInstance.notifyAll();
            
            this.timers.get(consensusInstance).interrupt();
            
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            config.getId(), consensusInstance, round, true));
        }
    }

    /*
     * Handle round change request -> ROUND-CHANGE(consensusInstanceId, roundToChangeTo, lastPreparedRound?, lastPreparedValue?) 
     *                                                                                      [? means it can be null]
     * upon receiving f+1 round change requests, the node will broadcast a round change request
     *    why: at least one correct node is asking to changing round
     *    ROUND-CHANGE(consensusInstanceId, min(roundToChangeTo), lastPreparedRound?, lastPreparedValue?) -- last two values are associated with the second
     * upon receiving 2f+1 round change requests, 
     *    why: a majority of correct nodes are asking to change round
     */
    public synchronized void uponRoundChangeRequest(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        Block value;

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
                link.sendPort(message.getSenderId(), m);
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
            
            Optional<Pair<Integer, Block>> highestPrepared = highestPrepared(roundChangeList);
            
            value = highestPrepared.orElse(Pair.of((Integer) null, instance.getBlock())).getRight();
            instance.setPreparedRound(highestPrepared.orElse(Pair.of((Integer) null, instance.getBlock())).getLeft());
            instance.setPreparedValue(value);

            return;
        }

        Optional<RoundChange> roundChangeSetValue = roundChangeMessages.hasValidRoundChangeSet(config.getId(),
                consensusInstance, round);
        if (roundChangeSetValue.isPresent()) {

            instance.setCurrentRound(roundChangeSetValue.get().getRound()); // r_i <- r_min

            this.stopTimeouts.get(consensusInstance).set(false); // set timer to running

            // create <ROUND-CHANGE, lambda_i, r_i, pr_i, pv_i>
            RoundChange rc = new RoundChange(consensusInstance, instance.getCurrentRound(), instance.getPreparedRound(), instance.getPreparedBlock());

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(instance.getCurrentRound())
                    .setMessage(rc.toJson())
                    .build();
            
            link.broadcastPort(m);
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
                        Message message = link.receive();

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reachConsensus(Block block) {
        startConsensus(block);
    }

}
