package pt.ulisboa.tecnico.hdsledger.communication.builder;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.personas.DeafLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.personas.IgnoreMessagesLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.personas.MuteLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.personas.RegularLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.personas.SlowLinkWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LinkWrapperBuilder {
    private final LinkWrapper instance;

    public LinkWrapperBuilder(ProcessConfig self, int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        switch (self.getPersona()) {
            // case REGULAR is the default
            case SLOW:
                this.instance = new SlowLinkWrapper(self, port, nodes, messageClass);
                break;
            case DROP:
                this.instance = new IgnoreMessagesLinkWrapper(self, port, nodes, messageClass);
                break;
            case DEAF:
                this.instance = new DeafLinkWrapper(self, port, nodes, messageClass);
                break;
            case MUTE:
                this.instance = new MuteLinkWrapper(self, port, nodes, messageClass);
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
