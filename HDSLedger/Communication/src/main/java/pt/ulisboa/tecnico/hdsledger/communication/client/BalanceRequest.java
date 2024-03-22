package pt.ulisboa.tecnico.hdsledger.communication.client;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;

public class BalanceRequest extends SharableMessage {

    private String target;

    public BalanceRequest(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
