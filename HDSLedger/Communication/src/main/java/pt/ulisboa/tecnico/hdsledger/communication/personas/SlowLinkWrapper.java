package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class SlowLinkWrapper extends LinkWrapper {
    int slowDownTime = 1000; // ms
    
    public SlowLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }

    public SlowLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
        boolean activateLogs, int baseSleepTime) {
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
    }

    public SlowLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
        boolean activateLogs, int baseSleepTime, int slowDownTime) {
        super(self, port, nodes, messageClass, activateLogs, baseSleepTime);
        this.slowDownTime = slowDownTime;
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        try {
            Thread.sleep(this.slowDownTime);
            this.getLink().unreliableSend(hostname, port, data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
