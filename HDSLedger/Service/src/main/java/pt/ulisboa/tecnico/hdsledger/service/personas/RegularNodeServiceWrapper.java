package pt.ulisboa.tecnico.hdsledger.service.personas;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ValueValidator;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class RegularNodeServiceWrapper extends NodeServiceWrapper {

    public RegularNodeServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] nodesConfig, ValueValidator validator) {
        super(link, config, nodesConfig, validator);
    }

    // All methods have the default implementation from NodeServiceWrapper: pass the call to the nodeService object.
}