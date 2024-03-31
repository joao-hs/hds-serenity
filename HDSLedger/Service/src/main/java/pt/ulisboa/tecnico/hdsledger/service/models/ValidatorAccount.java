package pt.ulisboa.tecnico.hdsledger.service.models;

public class ValidatorAccount extends Account {

    public ValidatorAccount(String id) {
        super(id);
    }
    
    @Override
    public boolean canReceiveFromClients() {
        return false;
    }
}
