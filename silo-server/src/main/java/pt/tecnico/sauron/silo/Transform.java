package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.gossip.Update;
import pt.tecnico.sauron.silo.gossip.VecTimestamp;
import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Transform {
    public static Obs ObservationIntoObs (Observation observation) {
        Obs.Builder obs = Obs.newBuilder();
        obs.setCameraName(observation.getCamera().getName());
        obs.setCoord1(observation.getCamera().getCoord1());
        obs.setCoord2(observation.getCamera().getCoord2());
        obs.setObjectId(observation.getId());
        obs.setObjectType(observation.getType());

        LocalDateTime time = observation.getTime();
        Instant instant = time.atZone(ZoneId.of("Europe/Lisbon")).toInstant();
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).build();
        obs.setTimestamp(timestamp);
        return obs.build();
    }

    public static List<Obs> ObservationListToObsList (List<Observation> observations) {
        List<Obs> obsList = new ArrayList<>();
        ListIterator<Observation> listIterator = observations.listIterator(observations.size());

        while (listIterator.hasPrevious()) {
            obsList.add(ObservationIntoObs(listIterator.previous()));
        }
        return obsList;
    }

    public static UpdtLog UpdateLogIntoUpdateLog (VecTimestamp vecTimestamp, Vector<SortedMap<Integer, Update>> log, Integer myId) {
        UpdtLog.Builder updtLog = UpdtLog.newBuilder();
        updtLog.setUpdateTimestamp(vecTimestamp.transform()).setReplica(myId);
        for (SortedMap<Integer, Update> map: log) {
            UpdtList.Builder updtList = UpdtList.newBuilder();
            for (Map.Entry<Integer, Update> entry : map.entrySet()) {
                Update update = entry.getValue();
                Updt.Builder updt = Updt.newBuilder();
                List<Obs> observations = update.getObsList();
                if (observations != null) {
                    updt.addAllObservations(observations);
                }
                List<Cam> cameras = update.getCamList();
                if (cameras != null) {
                    updt.addAllCameras(cameras);
                }
                updt.setVersion(entry.getKey());
                updtList.addUpdates(updt);
            }
            updtLog.addReplicasUpdates(updtList);
        }
        return updtLog.build();
    }
}
