package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.interfaces.Signable;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;


/**
 * Class that represents a message that can be shared between different entities
 * 
 * To share a message, you must prove who created it by signing it
 * Must be used when propagating messages that are not necessarily created by the sender
 */
public abstract class SharableMessage implements Signable {
    private String creator;
    private String signature;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

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

    /**
     * 
     * @param publicKey - path to the public key of the creator
     * 
     * @return true if the signature is valid, false otherwise
     *  
     */
    public boolean verifySignature(String publicKey) {
        if (this.creator == null || this.signature == null) {
            return false;
        }
        return RSAEncryption.verifySignature(this.toSignable(), this.signature, publicKey);
    }
}
