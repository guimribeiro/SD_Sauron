package pt.tecnico.sauron.silo.exceptions.Storage;

public class InvalidIdPartException extends StorageException {
    public InvalidIdPartException(String idPart) {
        super("The part of the id \"" + idPart + "\" is not valid.");
    }
}
