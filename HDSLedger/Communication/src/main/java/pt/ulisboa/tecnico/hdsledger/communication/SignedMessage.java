package pt.ulisboa.tecnico.hdsledger.communication;

import java.io.Serializable;

public class SignedMessage implements Serializable {

    // Message's signature
    private String signature;

    public SignedMessage() {

    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}