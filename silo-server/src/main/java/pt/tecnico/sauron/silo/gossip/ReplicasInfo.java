package pt.tecnico.sauron.silo.gossip;

import java.util.HashMap;
import java.util.Map;

public class ReplicasInfo {
    private final Integer _numReplicas;
    private final HashMap<Integer, VecTimestamp> _info;

    public ReplicasInfo(Integer[] neighbours, Integer numReplicas) {
        _numReplicas = numReplicas;
        _info = new HashMap<>();
        for (Integer neighbour: neighbours) {
            _info.put(neighbour, new VecTimestamp(numReplicas));
        }
    }

    public synchronized HashMap<Integer, VecTimestamp> getInfo() {
        return _info;
    }

    public synchronized VecTimestamp getLeastUpdated() {
        VecTimestamp leastUpdated = new VecTimestamp(_numReplicas);
        if (!_info.isEmpty()) {
            leastUpdated = _info.entrySet().iterator().next().getValue();
        }
        HashMap<Integer, VecTimestamp> info = getInfo();
        for (Map.Entry<Integer, VecTimestamp> entry: info.entrySet()) {
            leastUpdated.mergeMin(entry.getValue());
        }
        return leastUpdated;
    }

    public synchronized VecTimestamp getInfoFromReplica(Integer i) {
        return _info.get(i);
    }

    public synchronized void updateReplica(Integer i, VecTimestamp vecTimestamp) {
        if (_info.get(i) != null)
            _info.put(i, vecTimestamp);
    }
}
