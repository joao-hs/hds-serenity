package pt.ulisboa.tecnico.hdsledger.service.builders;

import pt.ulisboa.tecnico.hdsledger.service.personas.RegularLedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.BlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerServiceWrapperBuilder {
    private final LedgerServiceWrapper instance;

    public LedgerServiceWrapperBuilder( ProcessConfig nodeConfig,ProcessConfig[] clientConfigs
    ,ClientServiceWrapper clientService,NodeServiceWrapper nodeService,BlockBuilderService blockBuilderService) throws Exception {
        switch (nodeConfig.getPersona()) {
            // case REGULAR is the default
            case SLOW:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case DROP:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case DEAF:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case MUTE:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case WRONG_VALUE:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case WRONG_COMMIT:
                this.instance = new RegularLedgerServiceWrapper( nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            case ROUND_CHANGE:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
            default:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
                break;
        };
    }

    public LedgerServiceWrapper build() {
        return instance;
    }
}