package pt.tecnico.sauron.silo.client;

public class VecTimestamp {
    private final int _numReplicas;
    private final Integer[] _update;

    public VecTimestamp(Integer numReplicas) {
        _numReplicas = numReplicas;
        _update = new Integer[numReplicas];

        for (int i = 0; i < _numReplicas; i++) {
            _update[i] = 0;
        }
    }

    public VecTimestamp(Integer[] update) {
        _numReplicas = update.length;
        _update = update;
    }

    public void increment(int index) {
       _update[index]++;
    }

    public boolean greater_than(Integer[] update) {
        for (int i = 0; i < _numReplicas; i++) {
            if (_update[i] < update[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean lesser_than(Integer[] update) {
        for (int i = 0; i < _numReplicas; i++) {
            if (_update[i] > update[i]) {
                return false;
            }
        }
        return true;
    }

    public void merge(Integer[] update) {
        for (int i = 0; i < _numReplicas; i++) {
            _update[i] = Integer.max(_update[i], update[i]);
        }
    }

    public Integer[] get_update() {
        return _update;
    }
}
