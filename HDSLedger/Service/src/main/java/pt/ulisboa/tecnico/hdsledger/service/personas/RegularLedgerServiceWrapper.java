package pt.ulisboa.tecnico.hdsledger.service.personas;

import pt.ulisboa.tecnico.hdsledger.service.services.BlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class RegularLedgerServiceWrapper extends LedgerServiceWrapper {

    public RegularLedgerServiceWrapper( ProcessConfig nodeConfig,ProcessConfig[] clientConfigs
    ,ClientServiceWrapper clientService,NodeServiceWrapper nodeService,BlockBuilderService blockBuilderService) throws Exception {
        super( nodeConfig, clientConfigs,clientService,nodeService,blockBuilderService);
    }

    // All methods have the default implementation from LedgerServiceWrapper: pass the call to the ledgerService object.
}