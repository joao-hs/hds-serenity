package pt.ulisboa.tecnico.hdsledger.service.builders;

import pt.ulisboa.tecnico.hdsledger.service.personas.RegularClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class ClientServiceWrapperBuilder {
    private final ClientServiceWrapper instance;

    public ClientServiceWrapperBuilder(LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        switch (config.getPersona()) {
            // case REGULAR is the default
            case SLOW:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
            case DROP:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
            case WRONG_VALUE:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
            case WRONG_COMMIT:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
            case ROUND_CHANGE:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
            default:
                this.instance = new RegularClientServiceWrapper(link, config, clientsConfigs);
                break;
        };
    }

    public ClientServiceWrapper build() {
        return instance;
    }
}