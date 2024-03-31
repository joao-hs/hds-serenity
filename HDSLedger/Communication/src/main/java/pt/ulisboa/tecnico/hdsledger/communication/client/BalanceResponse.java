package pt.ulisboa.tecnico.hdsledger.communication.client;

import java.util.Objects;

public class BalanceResponse extends ClientResponse {

    private String target;
    private double balance;

    public BalanceResponse(Status status, String clientRequestHash, String target) {
        this.setGeneralStatus(GeneralStatus.NOT_SUBMITTED); // to consensus
        this.setStatus(status);
        this.setClientRequestHash(clientRequestHash);
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGeneralStatus(), getStatus(), getClientRequestHash(), target, balance);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BalanceResponse) {
            BalanceResponse other = (BalanceResponse) obj;
            return getGeneralStatus().equals(other.getGeneralStatus())
                && getStatus().equals(other.getStatus()) 
                && getClientRequestHash().equals(other.getClientRequestHash()) 
                && target.equals(other.target) 
                && balance == other.balance;
        }
        return false;
    }
}
