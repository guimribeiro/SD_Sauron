package pt.tecnico.sauron.silo.exceptions.Storage;

public class CameraNameMustNotBeEmptyException extends StorageException {
    public CameraNameMustNotBeEmptyException() {
        super("Camera name must not be empty.");
    }
}
