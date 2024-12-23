package pt.ulisboa.tecnico.hdsledger.communication;

import java.io.Serializable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.interfaces.Signable;

public abstract class SignedMessage implements Serializable, Signable {

    // Message's signature
    private String signature;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String toSignable() {
        Gson gson = new GsonBuilder().setExclusionStrategies(
            new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getName().equals("signature");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            }
        ).create();
        return gson.toJson(this);
    }
}