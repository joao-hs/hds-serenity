package pt.ulisboa.tecnico.hdsledger.communication.personas;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class WrongCommitLinkWrapper extends LinkWrapper {

    public WrongCommitLinkWrapper(ProcessConfig self, int port, ProcessConfig[] nodes,
            Class<? extends Message> messageClass) {
        super(self, port, nodes, messageClass);
    }
 
}
