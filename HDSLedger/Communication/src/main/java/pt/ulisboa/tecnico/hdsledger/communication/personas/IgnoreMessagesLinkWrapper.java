package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class IgnoreMessagesLinkWrapper extends LinkWrapper {
    
    public IgnoreMessagesLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }

    public IgnoreMessagesLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
        boolean activateLogs, int baseSleepTime) {
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        try {
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}