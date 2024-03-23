package pt.ulisboa.tecnico.hdsledger.service;

import pt.ulisboa.tecnico.hdsledger.communication.BlockchainRequest;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.builder.LinkWrapperBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.BlockValidator;
import pt.ulisboa.tecnico.hdsledger.service.services.BlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.service.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerService;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

public class Node {

    private static final CustomLogger LOGGER = new CustomLogger(Node.class.getName());
    // Hardcoded path to files
    private static String nodesConfigPath = "src/main/resources/";
    private static String clientConfigPath = "../Client/src/main/resources/";

    public static void main(String[] args) {

        try {
            if (args.length != 3) {
                LOGGER.log(Level.INFO, " [~] Invalid program arguments. Expected format: " +
                        "<call> <nodeID> <node-config-file> <client-config-file>");
                System.exit(1);
            }

            // Command line arguments
            String id = args[0];
            nodesConfigPath += args[1];
            clientConfigPath += args[2];

            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            ProcessConfig[] clientConfigs = new ProcessConfigBuilder().fromFile(clientConfigPath);

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:({2}:{3});",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(), nodeConfig.getClientPort()));
            
            // Abstraction to send and receive messages
            LinkWrapper linkToNodes = new LinkWrapperBuilder(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class).build();
            
            LinkWrapper linkToClients = new LinkWrapperBuilder(nodeConfig, nodeConfig.getClientPort(), clientConfigs,
                BlockchainRequest.class).build();

            BlockValidator blockValidator = new BlockValidator(clientConfigs);

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, nodeConfig, nodeConfigs, blockValidator);
            ClientService clientService = new ClientService(linkToClients, nodeConfig, clientConfigs);

            // Other services
            BlockBuilderService blockBuilderService = new BlockBuilderService(nodeConfig);

            // Start ledger
            LedgerService ledger = LedgerService.getInstance();
            ledger.setConfig(nodeConfig);
            ledger.addAllAccounts(clientConfigs);
            ledger.setClientService(clientService);
            ledger.setNodeService(nodeService);
            ledger.setBlockBuilderService(blockBuilderService);
            ledger.init();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
