package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class Transaction implements Comparable<Transaction> {
    @Expose
    private String serializedRequest;

    public Transaction(ClientRequest request) {
        this.serializedRequest = request.toJson();
    }

    public TransferRequest getTransferRequest() {
        return new Gson().fromJson(serializedRequest, TransferRequest.class);
    }

    public String getSerializedRequest(){
        return serializedRequest;
    }

    public String digest() {
        try {
            return RSAEncryption.digest(this.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return serializedRequest.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Transaction) {
            Transaction other = (Transaction) obj;
            return getSerializedRequest().equals(other.getSerializedRequest());
        }
        return false;
    }

    @Override
    public int compareTo(Transaction t2) {
        if (this.equals(t2)) {
            return 0;
        }
        TransferRequest tr1 = this.getTransferRequest();
        TransferRequest tr2 = t2.getTransferRequest();
        return tr1.getTimestamp().compareTo(tr2.getTimestamp());
    }

}
