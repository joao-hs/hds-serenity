package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class WrongValueLinkWrapper extends LinkWrapper {

    private String wrongMessage = "Wrong message";
    
    public WrongValueLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
        this.wrongMessage = this.additionalInfo.getOrDefault("wrongMessage", this.wrongMessage);
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        ConsensusMessage message = (ConsensusMessage) data;
        message.setMessage(this.wrongMessage);
        this.getLink().unreliableSend(hostname, port, message, true);
    }

}