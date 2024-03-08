package pt.ulisboa.tecnico.hdsledger.communication.builder;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.personas.RegularLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.personas.SlowLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig.Persona;

public class LinkWrapperBuilder {
    private final LinkWrapper instance;

    public LinkWrapperBuilder(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        switch (self.getPersona()) {
            // case REGULAR is the default
            case SLOW:
                this.instance = new SlowLinkWrapper(self, port, nodes, messageClass);
                break;
            default:
                this.instance = new RegularLinkWrapper(self, port, nodes, messageClass);
                break;
        };
    }

    public LinkWrapper build() {
        return instance;
    }
}
