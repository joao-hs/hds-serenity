package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class WrongValueLinkWrapper extends LinkWrapper {

    String wrong_message = "Wrong message";
    
    public WrongValueLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }

    public WrongValueLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
        boolean activateLogs, int baseSleepTime) {
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
    }

    public WrongValueLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass, 
            boolean activateLogs, int baseSleepTime, String wrong_message) {
        
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
        this.wrong_message = wrong_message;
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        ConsensusMessage message = (ConsensusMessage) data;
        message.setMessage(this.wrong_message);
        this.getLink().unreliableSend(hostname, port, message);
    }

}