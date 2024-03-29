package pt.ulisboa.tecnico.hdsledger.communication.client;

public class BalanceRequest extends ClientRequest {

    private final String target;

    public BalanceRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
