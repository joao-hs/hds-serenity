package pt.ulisboa.tecnico.hdsledger.communication.personas;

import java.io.IOException;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class DeafLinkWrapper extends LinkWrapper {
    
    public DeafLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }
    
    public Message receive() throws IOException, ClassNotFoundException {
        while (true) {
            try {
                Thread.sleep(10000); // Trapped in an infinite loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}