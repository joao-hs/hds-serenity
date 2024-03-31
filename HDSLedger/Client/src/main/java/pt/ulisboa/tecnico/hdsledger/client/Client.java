package pt.ulisboa.tecnico.hdsledger.client;

import java.text.MessageFormat;
import java.util.Scanner;

import pt.ulisboa.tecnico.hdsledger.utilities.ErrorMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

public class Client {

    private static String clientConfigPath = "src/main/resources/";
    private static String blockchainNodeConfigPath = "../Service/src/main/resources/";
    
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println(
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
            else if (command_info.length == 4 && (command_info[0].equals("transfer") || command_info[0].equals("t"))) {
                String receiver = command_info[1];
                double amount = Double.valueOf(command_info[2]);
                double fee = Double.valueOf(command_info[3]);
                blockchain.transfer(receiver, amount, fee);
            }
            else if (command_info.length == 2 && (command_info[0].equals("balance") || command_info[0].equals("b"))) {
                String target = command_info[1];
                blockchain.balance(target);
            }
            else {
                System.out.println(" [~] Invalid Command. Try Again");
                continue;
            }
        }

        reader.close();
    }

    private static void clientMenu(String clientId) {
        System.out.println("---------------------------------------------");
        System.out.println("|        ~ HDS Serenity Client Menu ~        ");
        System.out.println(MessageFormat.format("|        ~ Client ID: {0} ~        ", clientId));
        System.out.println("---------------------------------------------");
        System.out.println("");
        System.out.println(" Input one of the following commands: ");
        System.out.println("    transfer <destination:clientID:string> <amount:int> <fee:int> - transfer funds from your account to another");
        System.out.println("    balance <target:clientID:string> - check balance of target account");
        System.out.println("    menu - to print menu again");
        System.out.println("    quit - to exit the program");
        System.out.println("(Commands are case-sensitive and can be abbreviated to the first letter)");
        System.out.println("");
        System.out.println("---------------------------------------------");
        System.out.println("");
    }
}
