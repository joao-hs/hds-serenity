package pt.ulisboa.tecnico.hdsledger.communication.client;

import com.google.gson.Gson;

public class BalanceResponse extends ClientResponse {

    private String target;
    private int balance;

    public BalanceResponse(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public int hashCode() {
        return getStatus().hashCode() + target.hashCode() + balance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BalanceResponse) {
            BalanceResponse other = (BalanceResponse) obj;
            return getStatus().equals(other.getStatus()) && target.equals(other.target) && balance == other.balance;
        }
        return false;
    }
}
