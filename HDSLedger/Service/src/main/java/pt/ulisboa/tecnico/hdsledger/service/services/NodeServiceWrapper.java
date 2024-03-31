package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.INodeService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public abstract class NodeServiceWrapper implements UDPService, INodeService {

    protected NodeService nodeService;

    protected Map<String, String> additionalInfo;

    public NodeServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        this.nodeService = new NodeService(this, link, config, nodesConfig, validator);
        this.additionalInfo = config.getAdditionalInfo();
    }

    public ProcessConfig getConfig() {
        return this.nodeService.getConfig();
    }

    public void setLedgerService(LedgerServiceWrapper ledger){
        this.nodeService.setLedgerService(ledger);
    }

    public Map<String, ProcessConfig> getConfigs(){
        return this.nodeService.getConfigs();
    }

    public int getConsensusInstance() {
        return this.nodeService.getConsensusInstance();
    }

    public boolean isLeader(String id, int consensusInstance){
        return this.nodeService.isLeader(id, consensusInstance);
    }

    public ProcessConfig getLeader(int consensusInstance){
        return this.nodeService.getLeader(consensusInstance);
    }

    public boolean isConsensusValueValid(String serializedValue){
        return this.nodeService.isConsensusValueValid(serializedValue);
    }
 
    public ConsensusMessage createConsensusMessage(String serializedValue, int instance, int round) {
        return this.nodeService.createConsensusMessage(serializedValue, instance, round);
    }

    public void startTimer(int consensusInstance) {
        this.nodeService.startTimer(consensusInstance);
    }

    public void startConsensus(String serializedValue) {
        this.nodeService.startConsensus(serializedValue);
    }

    public Optional<Pair<Integer, String>> highestPrepared(List<RoundChangeMessage> quorumRoundChange){
        return this.nodeService.highestPrepared(quorumRoundChange);
    }

    public boolean justifyPrePrepare(int instanceId, int round){
        return this.nodeService.justifyPrePrepare(instanceId, round);
    }

    public boolean justifyRoundChange(int instanceId, InstanceInfo instanceInfo, List<RoundChangeMessage> quorumRoundChange){
        return this.nodeService.justifyRoundChange(instanceId, instanceInfo, quorumRoundChange);
    }

    public void uponPrePrepare(ConsensusMessage message) {
        this.nodeService.uponPrePrepare(message);
    }

    public synchronized void uponPrepare(ConsensusMessage message) {
        this.nodeService.uponPrepare(message);
    }

    public synchronized void uponCommit(ConsensusMessage message) {
        this.nodeService.uponCommit(message);
    }

    public synchronized void uponRoundChangeRequest(ConsensusMessage message) {
        this.nodeService.uponRoundChangeRequest(message);
    }

    public synchronized void uponGetContext(ConsensusMessage message) {
        this.nodeService.uponGetContext(message);
    }

    public void uponReceivedContext(ConsensusMessage message){
        this.nodeService.uponReceivedContext(message);
    }

    @Override
    public void listen() {
        this.nodeService.listen();
    }

    @Override
    public void reachConsensus(String value) {
        this.nodeService.reachConsensus(value);
    }
}
