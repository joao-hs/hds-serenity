package pt.ulisboa.tecnico.hdsledger.communication.interfaces;

public interface Signable {
    /**
     * Converts the full object to a JSON string.
     * @return
     */
    public String toJson();

    /**
     * Converts the object to a string that can be signed.
     * Excludes the signature field.
     * @return
     */
    public String toSignable();
}
