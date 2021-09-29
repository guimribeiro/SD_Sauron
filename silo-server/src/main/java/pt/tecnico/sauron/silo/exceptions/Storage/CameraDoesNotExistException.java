package pt.tecnico.sauron.silo.exceptions.Storage;

public class CameraDoesNotExistException extends StorageException {
    public CameraDoesNotExistException(String cameraName) {
        super("Camera with the name \"" + cameraName + "\" does not exist.");
    }
}
