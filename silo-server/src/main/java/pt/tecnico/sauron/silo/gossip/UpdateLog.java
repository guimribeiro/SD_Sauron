package pt.tecnico.sauron.silo.gossip;

import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.util.*;

public class UpdateLog {
    private final Vector<TreeMap<Integer, Update>> _log;
    private final Integer _numReplicas;

    public UpdateLog(Integer numReplicas) {
        _numReplicas = numReplicas;
        _log = new Vector<>(numReplicas);
        for (int i = 0; i < numReplicas; i++) {
            _log.add(i, new TreeMap<>());
        }
    }

    @Override
    public synchronized String toString() {
        return "UpdateLog{" +
                "_log=" + _log +
                ", _numReplicas=" + _numReplicas +
                '}';
    }

    public synchronized Vector<TreeMap<Integer, Update>> getLog() {
        return _log;
    }

    public synchronized void add (Integer myId, Integer version, ArrayList<Obs> obsList, ArrayList<Cam> camList) {
        Update update = new Update(obsList, camList);
        _log.get(myId - 1).put(version, update);
    }

    public synchronized Vector<SortedMap<Integer, Update>> getUpdatesFrom(VecTimestamp fromKey) {
        Vector<SortedMap<Integer, Update>> log = new Vector<>(_numReplicas);
        for (int i = 0; i < _numReplicas; i++) {
            log.add(i, _log.get(i).tailMap(fromKey.getReplicaVersion(i + 1), false));

        }
        return log;
    }

    public synchronized void removeUpdatesUntil (VecTimestamp toKey) {
        for (int i = 0; i < _numReplicas; i ++) {
            _log.get(i).headMap(toKey.getReplicaVersion(i + 1), true).clear();
        }
    }

    public synchronized ArrayList<Update> merge(VecTimestamp myTimestamp, List<UpdtList> log) {
        ArrayList<Update> updates = new ArrayList<>();
        int replica = 0;
        for (UpdtList updtList : log) {
            TreeMap<Integer, Update> map = _log.get(replica);
            Integer myVersion = myTimestamp.getReplicaVersion(replica + 1);
            for (Updt updt : updtList.getUpdatesList()) {
                Integer version = updt.getVersion();
                if (version > myVersion) {
                    Update update = new Update(updt.getObservationsList(), updt.getCamerasList());
                    map.put(version, update);
                    updates.add(update);
                }
            }
            replica++;
        }
        return updates;
    }
}
