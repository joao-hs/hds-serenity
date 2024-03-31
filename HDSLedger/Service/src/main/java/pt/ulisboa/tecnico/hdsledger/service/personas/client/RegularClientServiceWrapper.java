package pt.ulisboa.tecnico.hdsledger.service.personas.client;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class RegularClientServiceWrapper extends ClientServiceWrapper {

    public RegularClientServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        super(link,config,clientsConfigs);
    }

    // All methods have the default implementation from ClientServiceWrapper: pass the call to the clientService object.
}