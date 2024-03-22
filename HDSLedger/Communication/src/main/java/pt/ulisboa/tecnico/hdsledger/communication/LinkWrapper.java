package pt.ulisboa.tecnico.hdsledger.communication;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public abstract class LinkWrapper implements LinkInterface {

    private Link link;

    public LinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        this.link = new Link(this, self, port, nodes, messageClass);
    }

    public LinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
            boolean activateLogs, int baseSleepTime) {
        
        this.link = new Link(this, self, port, nodes, messageClass, activateLogs, baseSleepTime);
    }

    public Link getLink() {
        return this.link;
    }
    
    public void ackAll(List<Integer> messageIds) {
        this.link.ackAll(messageIds);
    }

    public void broadcastPort(Message data) {
        this.link.broadcastPort(data);
    }

    public void broadcastClientPort(Message data) {
        this.link.broadcastClientPort(data);
    }

    public void sendPort(String nodeId, Message data) {
        this.link.sendPort(nodeId, data);
    }

    public void sendClientPort(String clientId, Message data) {
        this.link.sendClientPort(clientId, data);
    }

    public void send(String nodeId, int destPort, Message data) {
        this.link.send(nodeId, destPort, data);
    }

    public void unreliableSend(InetAddress hostname, int port, Message data) {
        this.link.unreliableSend(hostname, port, data);
    }

    public Message receive() throws IOException, ClassNotFoundException {
        return this.link.receive();
    }

}
