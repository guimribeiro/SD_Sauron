package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.Transform;
import pt.tecnico.sauron.silo.exceptions.Storage.*;
import pt.tecnico.sauron.silo.gossip.GossipManager;
import pt.tecnico.sauron.silo.gossip.Update;
import pt.tecnico.sauron.silo.gossip.VecTimestamp;
import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Storage {
    private final GossipManager _gossipManager;

    //Associated with a type of object there is a map
    //Each key in that map is the id of an object of that type
    //Associated with that id there is a list of observations of that object ordered by date
    private final HashMap<Integer, TreeMap<String, Vector<Observation>>> _obsByType;
    private final HashMap<String, Camera> _cameras;

    public Storage(String zooHost, String zooPort, Integer myReplicaId, Integer numReplicas, long gossipInterval) {
        _obsByType = new HashMap<>();
        _obsByType.put(ObjectType.CAR_VALUE, new TreeMap<>());
        _obsByType.put(ObjectType.PERSON_VALUE, new TreeMap<>());
        _cameras = new HashMap<>();
        _gossipManager = new GossipManager(zooHost, zooPort, myReplicaId, numReplicas, gossipInterval);
    }

    //Getters and Setters

    public synchronized HashMap<String, Camera> getCameras() {
        return _cameras;
    }

    public synchronized HashMap<Integer, TreeMap<String, Vector<Observation>>> getObsByType() {
        return _obsByType;
    }

    public VecTimestamp getTimestamp() {
        return _gossipManager.getTimestamp();
    }

    public void addObservation(ObjectType type, Observation observation) {
        synchronized (_obsByType) {
            TreeMap<String, Vector<Observation>> observationTree = getObsByType().get(type.getNumber());
            Vector<Observation> observationList = observationTree.get(observation.getId());
            if (observationList == null) {
                observationList = new Vector<>();
            }
            observationList.add(observation);
            observationTree.put(observation.getId(), observationList);
        }
    }

    public void addCamera(Camera camera) throws CameraNameTakenException {
        synchronized (_cameras) {
            HashMap<String, Camera> cameraList = getCameras();
            System.out.println(camera.getName());
            Camera camStored = cameraList.get(camera.getName());
            if (camStored != null) {
                if (!camStored.equals(camera)) {
                    throw new CameraNameTakenException(camera.getName());
                }
            }
            cameraList.put(camera.getName(), camera);
        }
    }

    //Main methods

    public void init(List<Obs> obsList, List<Cam> camList) throws CameraNameTakenException {
        if (camList != null) {
            for (Cam cam : camList) {
                double coord1 = cam.getCoord1();
                double coord2 = cam.getCoord2();
                String camName = cam.getName();
                Camera camera = new Camera(camName, coord1, coord2);
                addCamera(camera);
            }
        }
        if (obsList != null) {
            for (Obs obs : obsList) {
                double coord1 = obs.getCoord1();
                double coord2 = obs.getCoord2();
                String camName = obs.getCameraName();
                Camera camera = new Camera(camName, coord1, coord2);
                addCamera(camera);
                ObjectType objectType = obs.getObjectType();
                String id = obs.getObjectId();
                LocalDateTime dateTime = Instant.ofEpochSecond(obs.getTimestamp()
                        .getSeconds())
                        .atZone(ZoneId.of("Europe/Lisbon"))
                        .toLocalDateTime();
                Observation observation = new Observation(dateTime, getCameras().get(camName), objectType, id);
                System.out.println("Observation added: " + objectType.toString() + " " + id + " " + dateTime + " " + camName);
                addObservation(objectType, observation);
            }
        }
    }

    public void clear() {
        synchronized (_obsByType) {
            _obsByType.clear();
            _obsByType.put(ObjectType.CAR_VALUE, new TreeMap<>());
            _obsByType.put(ObjectType.PERSON_VALUE, new TreeMap<>());
        }
        synchronized (_cameras) {
            _cameras.clear();
        }
    }

    public void camJoin(String name, Double coord1, Double coord2) throws CameraNameTakenException,
            CameraNameMustNotBeEmptyException,
            InvalidCoordinatesException {
        System.out.println(name);
        if (name.length() == 0) {
            throw new CameraNameMustNotBeEmptyException();
        }
        else if (!(coord1 < 90 && coord1 > -90 && coord2 < 90 && coord2 >-90 )) {
            throw new InvalidCoordinatesException(name);
        }
        else {
            addCamera(new Camera(name, coord1, coord2));
            ArrayList<Cam> camListLog = new ArrayList<>();
            camListLog.add(Cam.newBuilder()
                    .setName(name)
                    .setCoord1(coord1)
                    .setCoord2(coord2)
                    .build());
            _gossipManager.addToCamLog(camListLog);
        }
    }

    public Double[] camInfo(String name) throws CameraDoesNotExistException {
        Double[] coords = new Double[2];
        Camera cam = getCameras().get(name);
        if (cam == null) {
            throw new CameraDoesNotExistException(name);
        }
        coords[0] = cam.getCoord1();
        coords[1] = cam.getCoord2();
        return coords;
    }

    public void report(String name, List<Obs> obsList) throws CameraDoesNotExistException, InvalidIdException {
        LocalDateTime timestamp = LocalDateTime.now();
        StringBuilder exceptionMessage = new StringBuilder();
        ArrayList<Obs> obsListLog = new ArrayList<>();
        for (Obs obs : obsList) {
            if (_cameras.get(name) == null) {
                System.out.println("invalid camera: " + name);
                throw new CameraDoesNotExistException(name);
            }

            ObjectType objectType = obs.getObjectType();
            String id = obs.getObjectId();

            if (objectType == ObjectType.PERSON && !id.matches("[0-9]+")) {
                System.out.println("invalid person id: " + id);
                exceptionMessage.append("person id: ").append(id).append("; ");
            }
            else if (objectType == ObjectType.PERSON && id.matches("[0-9]+") &&
                    !((Double.parseDouble(id) > 0) && (Double.parseDouble(id) < (Math.pow(2, 63)-1)))) {
                System.out.println("invalid person id: " + id);
                exceptionMessage.append("person id: ").append(id).append("; ");
            }
            else if (objectType == ObjectType.CAR &&
                    !(id.matches("[0-9][0-9][A-Z][A-Z][0-9][0-9]") ||
                    id.matches("[0-9][0-9][0-9][0-9][A-Z][A-Z]") ||
                    id.matches("[A-Z][A-Z][0-9][0-9][0-9][0-9]") ||
                    id.matches("[A-Z][A-Z][0-9][0-9][A-Z][A-Z]") ||
                    id.matches("[A-Z][A-Z][A-Z][A-Z][0-9][0-9]") ||
                    id.matches("[0-9][0-9][A-Z][A-Z][A-Z][A-Z]"))) {
                System.out.println("invalid car id: " + id);
                exceptionMessage.append("car id: ").append(id).append("; ");
            }
            else {
                Observation observation = new Observation(timestamp, _cameras.get(name), objectType, id);
                System.out.println("observation added: " + objectType.toString() + " " + id + " " + timestamp + " " + name);
                addObservation(objectType, observation);

                obsListLog.add(Transform.ObservationIntoObs(observation));
            }
        }
        _gossipManager.addToObsLog(obsListLog);
        if (exceptionMessage.length() > 0) {
            throw new InvalidIdException(exceptionMessage.substring(0, exceptionMessage.length()-2));
        }
    }

    public Observation track(ObjectType type, String id) {
        synchronized (_obsByType) {
            Vector<Observation> obsList = getObsByType().get(type.getNumber()).get(id);
            if (obsList == null) {
                //empty observation
                return null;
            }
            return Collections.max(obsList);
        }
    }

    public Vector<Observation> trackMatch(ObjectType type, String idPart) throws StorageException {
        checkIdPart(idPart);
        String[] parts = idPart.split("\\*");
        Map<String, Vector<Observation>> results;
        synchronized (_obsByType) {
            TreeMap<String, Vector<Observation>> obsById = getObsByType().get(type.getNumber());
            if (idPart.charAt(0) == '*') {
                results = searchByEnd(obsById, parts[1]);
            } else if (idPart.charAt(idPart.length() - 1) == '*') {
                results = searchByBegin(obsById, parts[0]);
            } else {
                results = searchByBeginAndEnd(obsById, parts[0], parts[1]);
            }
            Vector<Observation> matches = new Vector<>();

            //get only the most recent observation for each matching Id
            for (Map.Entry<String, Vector<Observation>> entry : results.entrySet()) {
                Vector<Observation> observations = entry.getValue();
                matches.add(Collections.max(observations));
            }
            return matches;
        }
    }

    public Vector<Observation> trace(ObjectType type, String id) {
        synchronized (_obsByType) {
            Vector<Observation> obsList = getObsByType().get(type.getNumber()).get(id);
            if (obsList!=null)
                Collections.sort(obsList);
            return obsList;
        }
    }

    public void receiveGossip (UpdtLog updtLog) throws CameraNameTakenException {
        ArrayList<Update> updates = _gossipManager.update(updtLog);
        for (Update update: updates) {
            init(update.getObsList(), update.getCamList());
        }
    }

    public void closeThreads() {
        _gossipManager.closeThreads();
    }


    //Auxiliary methods

    public NavigableMap<String, Vector<Observation>> searchByBegin(TreeMap<String, Vector<Observation>> observations, String begin) {
        //returns string alphabetically after given string
        String lastKey = this.createStringNextTo(begin);

        //returns the map with only the strings started with the given string
        return observations.subMap(begin, true, lastKey, false);
    }

    public TreeMap<String, Vector<Observation>> searchByEnd(Map<String, Vector<Observation>> observations, String end) {
        TreeMap<String, Vector<Observation>> matchingObs = new TreeMap<>();
        for (Map.Entry<String, Vector<Observation>> entry : observations.entrySet()) {
            if (entry.getKey().endsWith(end)) {
                matchingObs.put(entry.getKey(), entry.getValue());
            }
        }
        return matchingObs;
    }

    public Map<String, Vector<Observation>> searchByBeginAndEnd(TreeMap<String, Vector<Observation>> observations, String begin, String end) {
        int len = begin.length() + end.length();
        Map<String, Vector<Observation>> firstRes = searchByBegin(observations, begin);
        Map<String, Vector<Observation>> IntermediateRes = searchByEnd(firstRes, end);
        Map<String, Vector<Observation>> finalRes = new HashMap<>();
        for (Map.Entry<String, Vector<Observation>> entry: IntermediateRes.entrySet()) {
            if (entry.getKey().length() >= len) {
                finalRes.put(entry.getKey(), entry.getValue());
            }
        }
        return finalRes;
    }

    public String createStringNextTo(String s) {
        int lastCharPosition = s.length() - 1;
        String withoutLastChar = s.substring(0, lastCharPosition);
        char lastChar = s.charAt(lastCharPosition);
        return withoutLastChar + (char) (lastChar + 1);
    }

    public void checkIdPart(String idPart) throws InvalidIdPartException {
        char c = '*';
        int count = 0;

        for (int i = 0; i < idPart.length(); i++) {
            if (idPart.charAt(i) == c) {
                count++;
            }
        }
        if (count != 1 || idPart.length() < 2) {
            throw new InvalidIdPartException(idPart);
        }
    }
}