package pt.tecnico.sauron.silo.exceptions.Storage;

public class InvalidCoordinatesException extends StorageException {
    public InvalidCoordinatesException(String cameraName) {
        super("Coordinates of camera name\"" + cameraName + "\" are not decimals between -90 and 90.");
    }
}
