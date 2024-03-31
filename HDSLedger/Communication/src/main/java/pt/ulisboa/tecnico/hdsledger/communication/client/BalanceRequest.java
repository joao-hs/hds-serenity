package pt.ulisboa.tecnico.hdsledger.communication.client;

import com.google.gson.annotations.Expose;

public class BalanceRequest extends ClientRequest {
    @Expose
    private final String target;

    public BalanceRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
