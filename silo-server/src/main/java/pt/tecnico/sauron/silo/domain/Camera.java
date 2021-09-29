package pt.tecnico.sauron.silo.domain;

import java.util.Objects;

public class Camera {

    private String name;
    private double coord1;
    private double coord2;


    public Camera(String name, double coord1, double coord2) {
        this.name = name;
        this.coord1 = coord1;
        this.coord2 = coord2;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Camera camera = (Camera) o;
        return Double.compare(camera.coord1, coord1) == 0 &&
                Double.compare(camera.coord2, coord2) == 0 &&
                Objects.equals(name, camera.name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCoord1() {
        return coord1;
    }

    public void setCoord1(double coord1) {
        this.coord1 = coord1;
    }

    public double getCoord2() {
        return coord2;
    }

    public void setCoord2(double coord2) {
        this.coord2 = coord2;
    }
}