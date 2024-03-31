package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.Pair;

import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.ContextMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.GetContextMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.INodeService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.service.models.ProgressIndicator;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class NodeService implements UDPService, INodeService {

    private static final CustomLogger LOGGER = new CustomLogger(NodeServiceWrapper.class.getName());
    // Nodes configurations
    private final Map<String, ProcessConfig> nodesConfig;
    // Value Validator
    private final ValueValidator validator;

    // Current node is leader
    private final ProcessConfig config;

    private final NodeServiceWrapper self;

    // Link to communicate with nodes
    private final LinkWrapper link;

    private LedgerServiceWrapper ledger = null;

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
    // Valid consensus value
    private final Set<String> validConsensusValues = new HashSet<>();
    //For context messages
    private final Map<String, String> contextResponses = new ConcurrentHashMap<>();

    public NodeService(NodeServiceWrapper self, LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        this.self = self;
        this.link = link;
        this.config = config;
        this.nodesConfig = Arrays.stream(nodesConfig).collect(
            ConcurrentHashMap::new,
            (m, v) -> m.put(v.getId(), v),
            ConcurrentHashMap::putAll
        );
        this.validator = validator;

        this.prepareMessages = new MessageBucket(nodesConfig.length);
        this.commitMessages = new MessageBucket(nodesConfig.length);
        this.roundChangeMessages = new MessageBucket(nodesConfig.length);
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public void setLedgerService(LedgerServiceWrapper ledger){
        this.ledger = ledger;
    }

    public Map<String, ProcessConfig> getConfigs(){
        return this.nodesConfig;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public boolean isLeader(String id, int consensusInstance) {
        return id.equals(this.self.getLeader(consensusInstance).getId());
    }

    public ProcessConfig getLeader(int consensusInstance) {
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);
        if (instance == null) {
            return null;
        }
        Integer round = this.instanceInfo.get(consensusInstance).getCurrentRound();
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Leader of consensus instance {1} is {2}", config.getId(), consensusInstance, (round % nodesConfig.size())));
        return nodesConfig.entrySet().stream()
            .filter(entry -> entry.getValue().getId().equals(String.valueOf(round % nodesConfig.size())))
            .map(Map.Entry::getValue)
            .findFirst() // There is always a leader
            .orElse(null); // Should never happen
    }

    public boolean isConsensusValueValid(String serializedValue) {
        if (serializedValue == null) {
            return false;
        }
        String hash;
        try {
            hash = RSAEncryption.digest(serializedValue);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        if (validConsensusValues.contains(hash)) {
            return true;
        }
        boolean ret = validator.validate(serializedValue);
        if (ret) {
            validConsensusValues.add(hash);
        }
        return ret;
    }
 
    public ConsensusMessage createConsensusMessage(String serializedValue, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(serializedValue);
        prePrepareMessage.sign(config.getId(), config.getPrivKeyPath());

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
                if (stopTimeout.get()) {
                    continue;
                }
                if (progressIndicator.isFrozen()) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Progress is frozen for consensus instance {1}, broadcasting round change",
                        config.getId(), consensusInstance));
                    
                    prepareMessages.getMessages(consensusInstance, instanceInfo.get(consensusInstance).getCurrentRound()).entrySet().forEach(entry -> {
                        ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                            .setRound(entry.getValue().getRound())
                            .setConsensusInstance(entry.getValue().getConsensusInstance())
                            .setMessage(entry.getValue().getMessage())
                            .build();
                        this.link.broadcastPort(m);
                    });
                    instanceInfo.get(consensusInstance).incrementRound();
                    stopTimeout.set(true);
                    progressIndicator.resetProgress();
                    RoundChangeMessage roundChangeMessage = new RoundChangeMessage(
                        consensusInstance,
                        instanceInfo.get(consensusInstance).getCurrentRound(),
                        instanceInfo.get(consensusInstance).getPreparedRound(),
                        instanceInfo.get(consensusInstance).getPreparedSerializedHashValue()
                    );
                    roundChangeMessage.sign(config.getId(), config.getPrivKeyPath());
                    this.link.broadcastPort(
                        new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                            .setRound(instanceInfo.get(consensusInstance).getCurrentRound())
                            .setConsensusInstance(consensusInstance)
                            .setMessage(roundChangeMessage.toJson())
                            .build()
                    );
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
    public void startConsensus(String serializedValue) {
        if (!this.self.isConsensusValueValid(serializedValue)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Invalid consensus value", config.getId()));
            return;
        }

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(serializedValue));


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
        Thread timer = new Thread(() -> this.self.startTimer(localConsensusInstance));
        timers.put(localConsensusInstance, timer);
        timer.start();
        
        // Leader broadcasts PRE-PREPARE message
        if (isLeader(config.getId(), localConsensusInstance)) {
            InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);
            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));
            this.link.broadcastPort(this.self.createConsensusMessage(serializedValue, localConsensusInstance, instance.getCurrentRound()));
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }
    }

    public Optional<Pair<Integer, String>> highestPrepared(List<RoundChangeMessage> quorumRoundChange) {
        Optional<RoundChangeMessage> roundChangeMessage = quorumRoundChange.stream()
            .filter(rc -> rc.getLastPreparedRound() != null && rc.getLastPreparedSerializedHashValue() != null)
            .max(Comparator.comparing(rc -> rc.getLastPreparedRound()));
        if (roundChangeMessage.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pair.of(roundChangeMessage.get().getLastPreparedRound(), roundChangeMessage.get().getLastPreparedSerializedHashValue()));
    }

    public boolean justifyPrePrepare(int instanceId, int round) {
        return (round == 1) ||
                roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                        .allMatch(roundChange ->
                                roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedSerializedHashValue() == null) ||
                this.self.highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of((Integer) null, ""))
                        .getRight().equals(prepareMessages.hasValidPrepareQuorum(config.getId(), instanceId, round).orElse(null));
    }

    public boolean justifyRoundChange(int instanceId, InstanceInfo instanceInfo, List<RoundChangeMessage> quorumRoundChange) {
        int round = instanceInfo.getCurrentRound();
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Justifying round change for instance {1}, round {2}", config.getId(), instanceId, round));
        boolean b1 = roundChangeMessages.getRoundChangeMessages(instanceId, round).stream()
                .allMatch(roundChange ->
                        roundChange.getLastPreparedRound() == null && roundChange.getLastPreparedSerializedHashValue() == null);
        boolean b2 = this.self.highestPrepared(roundChangeMessages.getRoundChangeMessages(instanceId, round)).orElse(Pair.of((Integer) null, ""))
                        .getRight().equals(prepareMessages.hasValidPrepareQuorum(config.getId(), instanceId, round).orElse(null));
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Justified round change for instance {1}, round {2}: {3} || {4}", config.getId(), instanceId, round, b1, b2));
        return b1 || b2;
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
        String creatorId = prePrepareMessage.getCreator();

        if (!prePrepareMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        String serializedValue = prePrepareMessage.getSerializedValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), senderId, creatorId, consensusInstance, round));

        if (!this.self.isConsensusValueValid(serializedValue)) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Invalid consensus value", config.getId()));
            return;
        }

        // Verify if pre-prepare was sent by leader
        if (!isLeader(creatorId, consensusInstance) || !this.self.justifyPrePrepare(consensusInstance, round)) {
            if (!isLeader(creatorId, consensusInstance)) LOGGER.log(Level.INFO, MessageFormat.format("{0} - Not leader, ignoring PRE-PREPARE message", config.getId()));
            if (!this.self.justifyPrePrepare(consensusInstance, round)) LOGGER.log(Level.INFO, MessageFormat.format("{0} - Not justified, ignoring PRE-PREPARE message", config.getId()));
            return;
        }

        // Set instance value
        InstanceInfo oldInstanceInfo = this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, serializedValue, false));
        if (oldInstanceInfo != null) {
            oldInstanceInfo.setSerializedValue(serializedValue);
            oldInstanceInfo.setValueHash();
        }
        
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

        this.timers.putIfAbsent(consensusInstance, new Thread(() -> this.self.startTimer(consensusInstance)));
        this.stopTimeouts.get(consensusInstance).set(false);
        progressIndicators.get(consensusInstance).registerProgress();

        InstanceInfo currentInstance = this.instanceInfo.get(consensusInstance);
        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Value for Consensus Instance {1}, Round {2}: {3}. Hash: {4}",
                        config.getId(), consensusInstance, round, serializedValue, currentInstance.getValueHash()));
        PrepareMessage prepareMessage = new PrepareMessage(currentInstance.getValueHash());
        prepareMessage.sign(config.getId(), config.getPrivKeyPath());

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
        String creatorId = prepareMessage.getCreator();

        String serializedHashValue = prepareMessage.getSerializedHashValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), senderId, creatorId, consensusInstance, round));

        if (!prepareMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        // Doesn't add duplicate messages
        prepareMessages.addMessage(message, creatorId);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(round, serializedHashValue, true));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() != null && instance.getPreparedRound() >= round) {
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
        Optional<String> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && (instance.getPreparedRound() == null || instance.getPreparedRound() < round)) {

            this.stopTimeouts.get(consensusInstance).set(false);
            progressIndicators.get(consensusInstance).registerProgress();

            instance.setPreparedHashValue(preparedValue.get());
            instance.setPreparedRound(round);

            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = prepareMessages.getMessages(consensusInstance, round)
                    .values();

            CommitMessage c = new CommitMessage(preparedValue.get());
            c.sign(config.getId(), config.getPrivKeyPath());
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
        CommitMessage commitMessage = message.deserializeCommitMessage();
        String creatorId = commitMessage.getCreator();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), message.getSenderId(), creatorId, consensusInstance, round));

        if (!commitMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        commitMessages.addMessage(message, creatorId);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}, created by {2}: Consensus Instance {3}, Round {4} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), commitMessage.getCreator(), consensusInstance, round);
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() != null && instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }

        Optional<String> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);

        if (commitValue.isPresent() && (instance.getCommittedRound() == null || instance.getCommittedRound() < round)) {

            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String serializedHashValue = commitValue.get();
            
            this.stopTimeouts.get(consensusInstance).set(true);

            // all previous consensus instances must decide before the current one
            while (lastDecidedConsensusInstance.get() < consensusInstance - 1) {
                try {
                    synchronized (lastDecidedConsensusInstance) {
                        lastDecidedConsensusInstance.wait(); // once a consensus instance is decided, notifies all threads
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.ensureContext(consensusInstance, serializedHashValue);

            // Append value to the ledger (must be synchronized to be thread-safe)
            ledger.uponConsensusReached(instance.getSerializedValue(), commitMessages.getCommitMessages(consensusInstance, round));
            
            synchronized (lastDecidedConsensusInstance) {
                lastDecidedConsensusInstance.getAndIncrement();
                lastDecidedConsensusInstance.notifyAll();
            }
            
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
        String serializedHashValue;
        RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
        String creatorId = roundChangeMessage.getCreator();

        if (!roundChangeMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        roundChangeMessages.addMessage(message, creatorId);

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received ROUND_CHANGE message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), message.getSenderId(), creatorId, consensusInstance, round));

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            MessageFormat.format(
                    "{0} - CRITICAL: Received ROUND_CHANGE message from {1}, created by {2}: Consensus Instance {3}, Round {4} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), creatorId, consensusInstance, round);
            return;
        }

        // If this process already decided on a value for this instance, so this process has received a Quorum of CommitMessages
        if (instance.getCommittedRound() != null) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already decided on Consensus Instance {1}, Round {2}, sending commit messages to round change request creator",
                            config.getId(), consensusInstance, round));
            commitMessages.getMessages(consensusInstance, instance.getCommittedRound()).values().forEach(commitMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(creatorId)
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

        List<RoundChangeMessage> roundChangeList = roundChangeMessages.getRoundChangeMessages(consensusInstance, round);

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received {1} ROUND_CHANGE messages for Consensus Instance {2}, Round {3}",
                        config.getId(), roundChangeList.size(), consensusInstance, round));

        if(roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round) 
            && isLeader(this.config.getId(), consensusInstance) && this.self.justifyRoundChange(consensusInstance, instance, roundChangeList)){
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Received 2f+1 ROUND_CHANGE messages for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, round));
            
            Optional<Pair<Integer, String>> optHighestPrepared = this.self.highestPrepared(roundChangeList);
            
            if (optHighestPrepared.isPresent()) {
                serializedHashValue = optHighestPrepared.get().getRight();
                instance.setPreparedRound(optHighestPrepared.get().getLeft());
                instance.setPreparedHashValue(serializedHashValue);
            } else {
                serializedHashValue = instance.getValueHash();
            }
            
            LOGGER.log(Level.INFO,
            MessageFormat.format("{0} - Local value hash: {1}, Highest prepared value hash: {2}, Prepared round: {3}",
                config.getId(), instance.getValueHash(), serializedHashValue, instance.getPreparedRound()));
            if (serializedHashValue == null) {
                LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - No value known for Consensus Instance {1}",
                        config.getId(), consensusInstance));
                return;
            }
            
            
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} -  Try getting context for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, round));
            
            String serializedValue = instance.getMySerializedValue();
            if (serializedValue == null) {
                serializedValue = instance.getSerializedValue();
            }
            if (serializedValue == null) {
                // this node does not have the context, and it'll be too slow to retrieve it, so it let's time run out to change round
                return;
            }

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Context received for Consensus Instance {1}, Round {2}. Broadcasting PrePrepare messages",
                            config.getId(), consensusInstance, round));
            PrePrepareMessage prePrepareMessage = new PrePrepareMessage(serializedValue);
            prePrepareMessage.sign(config.getId(), config.getPrivKeyPath());
            link.broadcastPort(
                new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(prePrepareMessage.toJson())
                    .build());

            return;
        }

        Optional<RoundChangeMessage> roundChangeSetValue = roundChangeMessages.hasValidRoundChangeSet(config.getId(),
                consensusInstance, round);
        if (roundChangeSetValue.isPresent()) {

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received f+1 ROUND_CHANGE messages for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, round));
            instance.setCurrentRound(roundChangeSetValue.get().getRound()); // r_i <- r_min

            this.stopTimeouts.get(consensusInstance).set(false); // set timer to running

            // create <ROUND-CHANGE, lambda_i, r_i, pr_i, pv_i>
            RoundChangeMessage rc = new RoundChangeMessage(consensusInstance, instance.getCurrentRound(), instance.getPreparedRound(), instance.getPreparedSerializedHashValue());
            rc.sign(config.getId(), config.getPrivKeyPath());

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(instance.getCurrentRound())
                    .setMessage(rc.toJson())
                    .build();
            
            link.broadcastPort(m);
            return;
        }
    }

    private void ensureContext(int consensusInstance, String serializedHashValue) {
        boolean success = tryGetContext(consensusInstance, serializedHashValue);
        while (!success) {
            success = tryGetContext(consensusInstance, serializedHashValue);
        }
    }

    private boolean tryGetContext(int consensusInstance, String serializedHashValue) {

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance.getSerializedValue() == null || !serializedHashValue.equals(instance.getValueHash())) {
            
            if (contextResponses.containsKey(serializedHashValue) && contextResponses.get(serializedHashValue) != null) {
                try {
                    final String valueHash = RSAEncryption.digest(contextResponses.get(serializedHashValue));
                    if(valueHash.equals(serializedHashValue)) {
                        instance.setSerializedValue(contextResponses.get(serializedHashValue));
                        instance.setValueHash();
                        return true;
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            
            GetContextMessage getContextMessage = new GetContextMessage(serializedHashValue);
            getContextMessage.sign(config.getId(), config.getPrivKeyPath());
            this.link.broadcastPort(
                new ConsensusMessageBuilder(config.getId(), Message.Type.RECEIVED_CONTEXT)
                    .setRound(instanceInfo.get(consensusInstance).getCurrentRound())
                    .setConsensusInstance(consensusInstance)
                    .setMessage(getContextMessage.toJson())
                    .build()
            );

            contextResponses.put(serializedHashValue, null);
            try {
                synchronized (serializedHashValue) {
                    serializedHashValue.wait(10000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            String serializedValue = contextResponses.get(serializedHashValue);
            if (serializedValue != null) {
                try {
                    final String valueHash = RSAEncryption.digest(serializedValue);
                    if(valueHash.equals(serializedHashValue)) {
                        instance.setSerializedValue(serializedValue);
                        instance.setValueHash();
                        return true;
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        else {
            return true;
        }
    }

    public synchronized void uponGetContext(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        GetContextMessage getContextMessage = message.deserializeGetContextMessage();
        String creatorId = getContextMessage.getCreator();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received GET-CONTEXT message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), senderId, creatorId, consensusInstance, round));

        if (!getContextMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        String serializedHashValue = getContextMessage.getSerializedHashValue();
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (serializedHashValue.equals(instance.getValueHash()) && instance.getSerializedValue() != null) {
            ContextMessage contextMessage = new ContextMessage(instance.getSerializedValue());
            contextMessage.sign(config.getId(), config.getPrivKeyPath());
            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setMessage(contextMessage.toJson())
                    .build();

            link.sendPort(senderId, m);
        }
    }

    public synchronized void uponReceivedContext(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        ContextMessage getContextMessage = message.deserializeContextMessage();
        String creatorId = getContextMessage.getCreator();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received RECEIVED-CONTEXT message from {1}, created by {2}: Consensus Instance {3}, Round {4}",
                        config.getId(), senderId, creatorId, consensusInstance, round));

        if (!getContextMessage.verifySignature(nodesConfig.get(creatorId).getPubKeyPath())) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1} did not create the message or message has been tampered", config.getId(), creatorId));
            return;
        }

        String serializedValue = getContextMessage.getSerializedValue();
        if (serializedValue == null) {
            return;
        }

        try {
            final String valueHash = RSAEncryption.digest(serializedValue);
            String hash = contextResponses.keySet().stream().filter(h -> h.equals(valueHash)).findFirst().get();
            if (hash == null) {
                return;
            }
            contextResponses.put(hash, serializedValue);
            synchronized (hash) {
                hash.notifyAll();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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
                                    this.self.uponPrePrepare((ConsensusMessage) message);

                                case PREPARE ->
                                    this.self.uponPrepare((ConsensusMessage) message);

                                case COMMIT ->
                                    this.self.uponCommit((ConsensusMessage) message);

                                case ROUND_CHANGE ->
                                    this.self.uponRoundChangeRequest((ConsensusMessage) message);
                                
                                case GET_CONTEXT ->
                                    this.self.uponGetContext((ConsensusMessage) message);

                                case RECEIVED_CONTEXT ->
                                    this.self.uponReceivedContext((ConsensusMessage) message);

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
    public void reachConsensus(String value) {
        this.self.startConsensus(value);
    }

}
