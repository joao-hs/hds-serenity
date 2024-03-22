package pt.ulisboa.tecnico.hdsledger.communication.client;

public class BalanceRequest extends ClientRequest {

    private String target;

    public BalanceRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
