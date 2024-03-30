package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Map;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.INodeService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeServiceWrapper implements UDPService, INodeService {

    private NodeService nodeService;

    protected Map<String, String> additionalInfo;

    public NodeServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        this.nodeService = new NodeService(link, config, nodesConfig, validator);
        this.additionalInfo = config.getAdditionalInfo();
    }

    public ProcessConfig getConfig() {
        return this.nodeService.getConfig();
    }

    public Map<String, ProcessConfig> getConfigs(){
        return this.nodeService.getConfigs();
    }

    public int getConsensusInstance() {
        return this.nodeService.getConsensusInstance();
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

    @Override
    public void listen() {
        this.nodeService.listen();
    }

    @Override
    public void reachConsensus(String value) {
        this.nodeService.reachConsensus(value);
    }
}
