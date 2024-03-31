package pt.ulisboa.tecnico.hdsledger.service.builders;

import pt.ulisboa.tecnico.hdsledger.service.personas.RegularLedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.BlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerServiceWrapperBuilder {
    private final LedgerServiceWrapper instance;

    public LedgerServiceWrapperBuilder(ProcessConfig nodeConfig, ProcessConfig[] clientConfigs, ProcessConfig[] nodeConfigs,
        ClientServiceWrapper clientService, NodeServiceWrapper nodeService, BlockBuilderService blockBuilderService) throws Exception {
        
        switch (nodeConfig.getPersona()) {
            // case REGULAR is the default
            default:
                this.instance = new RegularLedgerServiceWrapper(nodeConfig, clientConfigs, nodeConfigs, clientService, nodeService,blockBuilderService);
                break;
        };
    }

    public LedgerServiceWrapper build() {
        return instance;
    }
}