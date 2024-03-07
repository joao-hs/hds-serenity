package pt.ulisboa.tecnico.hdsledger.client;

import java.text.MessageFormat;
import java.util.Scanner;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ErrorMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private static String clientConfigPath = "src/main/resources/";
    private static String blockchainNodeConfigPath = "../Service/src/main/resources/";
    
    public static void main(String[] args) {
        if (args.length != 3) {
            LOGGER.log(Level.INFO, 
                " [~] Invalid program arguments. Expected format: " + 
                "<call> <clientID> <client-config-file> <blockchain-node-config-file");
            System.exit(1);
        }

        final String clientId = args[0];
        clientConfigPath += args[1];
        blockchainNodeConfigPath += args[2];

        ProcessConfig clientProcessConfig = new ProcessConfigBuilder().idFromFile(clientConfigPath, clientId)
                .orElseThrow(() -> new HDSSException(ErrorMessage.ClientNotFound));
        ProcessConfig[] blockchainNodeConfigs = new ProcessConfigBuilder().fromFile(blockchainNodeConfigPath);

        Blockchain blockchain = new Blockchain(clientProcessConfig, blockchainNodeConfigs);
        blockchain.start();

        Scanner reader = new Scanner(System.in);
        clientMenu(clientId);
        while (true) {
            String command = reader.nextLine().trim();
            String[] command_info = command.split(" +");
            if (command_info.length == 0) {
                continue;
            }

            if (command_info[0].equals("quit") || command_info[0].equals("q")) {
                break;
            }
            else if (command_info[0].equals("menu") || command_info[0].equals("m")){
                clientMenu(clientId);
                continue;
            }
            else if (command_info.length == 2 && (command_info[0].equals("append") || command_info[0].equals("a"))){
                blockchain.append(command_info[1]);
                continue;
            }
            else {
                System.out.println(" [~] Invalid Command. Try Again");
                continue;
            }
        }

        reader.close();
    }

    private static void clientMenu(String clientId) {
        LOGGER.log(Level.INFO, "---------------------------------------------");
        LOGGER.log(Level.INFO, "|        ~ HDS Serenity Client Menu ~        ");
        LOGGER.log(Level.INFO, MessageFormat.format("|        ~ Client ID: {0} ~        ", clientId));
        LOGGER.log(Level.INFO, "---------------------------------------------");
        LOGGER.log(Level.INFO, "");
        LOGGER.log(Level.INFO, " Input one of the following commands: ");
        LOGGER.log(Level.INFO, "    < append | a, string > to append a string");
        LOGGER.log(Level.INFO, "    < menu | m > to print menu again");
        LOGGER.log(Level.INFO, "    < quit | q > to exit the programm");
        LOGGER.log(Level.INFO, "");
        LOGGER.log(Level.INFO, "---------------------------------------------");
        LOGGER.log(Level.INFO, "");
    }
}
