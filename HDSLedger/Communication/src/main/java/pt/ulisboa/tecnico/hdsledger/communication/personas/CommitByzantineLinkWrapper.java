package pt.ulisboa.tecnico.hdsledger.communication.personas;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class CommitByzantineLinkWrapper extends LinkWrapper {
    
    public CommitByzantineLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }

    public void sendPort(String nodeId, Message data) {
        data.setType(Message.Type.COMMIT);
        this.getLink().sendPort(nodeId, data);
    }

}
