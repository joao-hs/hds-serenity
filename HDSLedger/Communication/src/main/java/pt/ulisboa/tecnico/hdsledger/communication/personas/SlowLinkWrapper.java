package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.net.InetAddress;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class SlowLinkWrapper extends LinkWrapper {
    private int slowDownTime = 1000; // ms
    
    public SlowLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
        this.slowDownTime = Integer.parseInt(this.additionalInfo.getOrDefault("slowDownTime", String.valueOf(this.slowDownTime)));
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        try {
            Thread.sleep(this.slowDownTime);
            this.getLink().unreliableSend(hostname, port, data, true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
