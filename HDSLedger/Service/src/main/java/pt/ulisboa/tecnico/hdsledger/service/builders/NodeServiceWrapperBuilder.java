package pt.ulisboa.tecnico.hdsledger.service.builders;

import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.service.personas.RegularNodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeServiceWrapperBuilder {
    private final NodeServiceWrapper instance;

    public NodeServiceWrapperBuilder(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        switch (config.getPersona()) {
            // case REGULAR is the default
            default:
                this.instance = new RegularNodeServiceWrapper(link, config, nodesConfig, validator);
                break;
        };
    }

    public NodeServiceWrapper build() {
        return instance;
    }
}