package pt.tecnico.sauron.silo.exceptions.Storage;

public class InvalidIdException extends StorageException {
    public InvalidIdException(String ids) { super("Invalid IDs - " + ids); }
}
