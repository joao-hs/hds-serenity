package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.io.IOException;
import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class RoundChangeLinkWrapper extends LinkWrapper {
    
    public RoundChangeLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }

    public RoundChangeLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
        boolean activateLogs, int baseSleepTime) {
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
    }

    public void sendPort(String nodeId, Message data) {
        data.setType(Message.Type.ROUND_CHANGE);
        this.getLink().sendPort(nodeId, data);
    }
}