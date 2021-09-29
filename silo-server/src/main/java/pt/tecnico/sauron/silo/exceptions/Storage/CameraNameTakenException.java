package pt.tecnico.sauron.silo.exceptions.Storage;

public class CameraNameTakenException extends StorageException {
    public CameraNameTakenException(String cameraName) {
        super("Camera name \"" + cameraName + "\" is already being used by a different camera.");
    }
}
