package pt.ulisboa.tecnico.hdsledger.communication.client;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;

public abstract class ClientRequest extends SharableMessage {

    @Override
    public String toString() {
        return this.toJson();
    }
}
