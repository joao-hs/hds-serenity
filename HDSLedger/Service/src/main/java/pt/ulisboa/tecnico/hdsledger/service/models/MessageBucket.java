package pt.ulisboa.tecnico.hdsledger.service.models;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;

public class MessageBucket{

    private static final CustomLogger LOGGER = new CustomLogger(MessageBucket.class.getName());
    // Quorum size
    private final int quorumSize;
    // Maximum number of Byzantine nodes
    private final int f;
    // Instance -> Round -> Sender ID -> Consensus message
    private final Map<Integer, Map<Integer, Map<String, ConsensusMessage>>> bucket = new ConcurrentHashMap<>();

    public MessageBucket(int nodeCount) {
        this.f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    /*
     * Add a message to the bucket
     * 
     * @param consensusInstance
     * 
     * @param message
     */
    public void addMessage(ConsensusMessage message, String creator) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        bucket.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).putIfAbsent(round, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).get(round).put(creator, message);
    }

    public Optional<String> hasValidPrepareQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        if (!bucket.containsKey(instance)) {
            return Optional.empty();
        }
        Map<String, ConsensusMessage> roundMessages = bucket.get(instance).get(round);
        if (roundMessages == null) {
            return Optional.empty();
        }
        roundMessages.values().forEach((message) -> {
            PrepareMessage prepareMessage = message.deserializePrepareMessage();
            String serializedHashValue = prepareMessage.getSerializedHashValue();
            frequency.put(serializedHashValue, frequency.getOrDefault(serializedHashValue, 0) + 1);
        });
        frequency.entrySet().forEach((entry) -> {
            LOGGER.log(Level.INFO, MessageFormat.format("Frequency: {0}->{1}", entry.getKey(), entry.getValue()));
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Optional<String> hasValidCommitQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        Map<String, ConsensusMessage> roundMessages = bucket.get(instance).get(round);
        if (roundMessages == null) {
            return Optional.empty();
        }
        roundMessages.values().forEach((message) -> {
            CommitMessage commitMessage = message.deserializeCommitMessage();
            String serializedValue = commitMessage.getSerializedHashValue();
            frequency.put(serializedValue, frequency.getOrDefault(serializedValue, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public boolean hasValidRoundChangeQuorum(String nodeId, int instance, int round) {
        Map<String, ConsensusMessage> roundMessages = bucket.get(instance).get(round);
        if (roundMessages == null) {
            return false;
        }
        LOGGER.log(Level.INFO, MessageFormat.format("Round change current quorum size: {0}", roundMessages.size()));
        return roundMessages.size() >= 2 * f + 1;
    }

    /*
     * Is valid if:
     * f+1 round change messages are received with:
     * - the same instance
     * - higher round
     * return the round change message with the lowest round
     * else, return optional.empty
     */
    public Optional<RoundChangeMessage> hasValidRoundChangeSet(String nodeId, int instance, int round) {
        // 1. Count the number of round change messages with the same instance and higher round
        Map<String, ConsensusMessage> roundMessages = bucket.get(instance).get(round);
        Stream<ConsensusMessage> validRoundChangeMessages = roundMessages.values()
            .stream()
            .filter((message) -> message.getConsensusInstance() == instance)
            .filter((message) -> message.getRound() > round);

        if (validRoundChangeMessages.count() < f + 1) {
            return Optional.empty();
        }

        // 2. Find the round change message with the lowest round

        return validRoundChangeMessages
            .map((message) -> message.deserializeRoundChangeMessage())
            .min((message1, message2) -> message1.getRound() - message2.getRound());
    }

    public Map<String, ConsensusMessage> getMessages(int instance, int round) {
        if (!bucket.containsKey(instance) || !bucket.get(instance).containsKey(round)) {
            return Collections.emptyMap();
        }
        return bucket.get(instance).get(round);
    }

    public Collection<CommitMessage> getCommitMessages(int instance, int round) {
        Collection<ConsensusMessage> allMessages = bucket.get(instance).get(round).values();
        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return allMessages.stream()
            .filter((message) -> message.getType().equals(Message.Type.COMMIT))
            .map((message) -> message.deserializeCommitMessage())
            .toList();
    }

    public List<RoundChangeMessage> getRoundChangeMessages(int instance, int round) {
        if (!bucket.containsKey(instance) || !bucket.get(instance).containsKey(round)) {
            return Collections.emptyList();
        }
        Collection<ConsensusMessage> allMessages = bucket.get(instance).get(round).values();
        if (allMessages.isEmpty()) {
            LOGGER.log(Level.INFO, MessageFormat.format("No round change messages for instance {0} round {1}", instance, round));
            return Collections.emptyList();
        }
        return allMessages.stream()
            .filter((message) -> message.getType().equals(Message.Type.ROUND_CHANGE))
            .map((message) -> message.deserializeRoundChangeMessage())
            .map((message) -> {
                LOGGER.log(Level.INFO, MessageFormat.format("ROUND_CHANGE<{0},{1},{2},{3},{4}>", instance, round, message.getCreator(), message.getLastPreparedRound(), message.getLastPreparedSerializedHashValue()));
                return message;
            })
            .toList();
    }

    public String toString() {
        return new Gson().toJson(bucket);
    }
}