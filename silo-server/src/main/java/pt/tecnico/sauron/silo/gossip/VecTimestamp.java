package pt.tecnico.sauron.silo.gossip;

import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.util.Arrays;
import java.util.List;

public class VecTimestamp {
    public final int _numReplicas;
    private final Integer[] _update;

    public VecTimestamp(Integer numReplicas) {
        _numReplicas = numReplicas;
        _update = new Integer[numReplicas];

        for (int i = 0; i < numReplicas; i++) {
            _update[i] = 0;
        }
    }

    public VecTimestamp(UpdateTimestamp updateTimestamp) {
        _numReplicas = updateTimestamp.getTimestampCount();
        _update = new Integer[_numReplicas];
        List<Integer> versions = updateTimestamp.getTimestampList();
        int i = 0;
        for (int version: versions) {
            _update[i] = version;
            i++;
        }
    }

    public VecTimestamp(VecTimestamp vecTimestamp) {
        _numReplicas = vecTimestamp.getNReplicas();
        _update = new Integer[_numReplicas];

        for (int i = 0; i < _numReplicas; i++) {
            _update[i] = vecTimestamp.getReplicaVersion(i + 1);
        }
    }

    @Override
    public synchronized String toString() {
        return "VecTimestamp{" +
                "_update=" + Arrays.toString(_update) +
                '}';
    }

    public synchronized UpdateTimestamp transform() {
        UpdateTimestamp.Builder updateTimestamp = UpdateTimestamp.newBuilder();
        for (int i = 0; i < getNReplicas(); i++) {
            updateTimestamp.addTimestamp(_update[i]);
        }
        return updateTimestamp.build();
    }

    public int getNReplicas() {
        return _numReplicas;
    }

    public synchronized Integer[] getUpdate() {
        return _update;
    }

    public synchronized void increment(int replica) {
        _update[replica - 1]++;
    }

    public synchronized Integer getReplicaVersion(Integer replica) {
        return _update[replica - 1];
    }

    public synchronized void merge(VecTimestamp vecTimestamp) {
        Integer[] update = vecTimestamp.getUpdate();
        for (int i = 0; i < _numReplicas; i++) {
            _update[i] = Integer.max(_update[i], update[i]);
        }
    }

    public synchronized void mergeMin(VecTimestamp vecTimestamp) {
        Integer[] update = vecTimestamp.getUpdate();
        for (int i = 0; i < _numReplicas; i++) {
            _update[i] = Integer.min(_update[i], update[i]);
        }
    }
}
