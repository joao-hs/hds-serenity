package pt.ulisboa.tecnico.hdsledger.communication.client;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ClientMessageInterface;

public abstract class ClientRequest extends SharableMessage implements ClientMessageInterface {

    @Override
    public String toString() {
        return this.toJson();
    }
}
