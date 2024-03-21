package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class BalanceRequest {

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
