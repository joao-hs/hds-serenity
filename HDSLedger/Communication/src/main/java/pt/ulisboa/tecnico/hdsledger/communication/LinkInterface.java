package pt.ulisboa.tecnico.hdsledger.communication;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

interface LinkInterface {

    public void ackAll(List<Integer> messageIds);

    /**
     * Broadcasts a message to all nodes in the network
     *
     * @param data The message to be broadcasted
     */
    public void broadcastPort(Message data);

    /**
     * Broadcasts a message to all clients in the network
     *
     * @param data The message to be broadcasted
     */
    public void broadcastClientPort(Message data);

    /**
     * Sends a message to a specific node in the network
     * 
     * @param nodeId The node identifier
     * 
     * @param data The message to be sent
     */
    public void sendPort(String nodeId, Message data);

    /**
     * Sends a message to a specific client in the network
     * 
     * @param nodeId The node identifier
     * 
     * @param data The message to be sent
     */
    public void sendClientPort(String nodeId, Message data);

    /**
     * Sends a message to a specific node with guarantee of delivery
     *
     * @param nodeId The node identifier
     *
     * @param data The message to be sent
     */
    public void send(String nodeId, int destPort, Message data);

    /**
     * Sends a message to a specific node without guarantee of delivery
     * Mainly used to send ACKs, if they are lost, the original message will be
     * resent
     *
     * @param address The address of the destination node
     *
     * @param port The port of the destination node
     *
     * @param data The message to be sent
     */
    public void unreliableSend(InetAddress hostname, int port, Message data);

    /**
     * Receives a message from any node in the network (blocking)
     */
    public Message receive() throws IOException, ClassNotFoundException;

}