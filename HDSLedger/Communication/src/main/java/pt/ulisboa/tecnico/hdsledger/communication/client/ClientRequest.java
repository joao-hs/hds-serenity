package pt.ulisboa.tecnico.hdsledger.communication.client;

import java.security.NoSuchAlgorithmException;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ClientMessageInterface;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public abstract class ClientRequest extends SharableMessage implements ClientMessageInterface {

    @Override
    public String toString() {
        return this.toJson();
    }

    public String digest() {
        try {
            return RSAEncryption.digest(this.toJson());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
