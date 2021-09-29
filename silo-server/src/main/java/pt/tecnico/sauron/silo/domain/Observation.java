package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.time.LocalDateTime;

public class Observation implements Comparable<Observation>{

    private LocalDateTime time;
    private Camera camera;
    private ObjectType type;
    private String id;

    public Observation(LocalDateTime time, Camera camera, ObjectType type, String id) {
        this.time = time;
        this.camera = camera;
        this.type = type;
        this.id = id;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int compareTo (Observation obs) {
        return this.getTime().compareTo(obs.getTime());
    }
}
