package pt.tecnico.sauron.silo.gossip;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.Gossiper;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.*;

public class GossipManager {
    private final VecTimestamp _vecTimestamp;
    private ReplicasInfo _replicasInfo;
    private final UpdateLog _updateLog;
    private final Gossiper _gossiper;
    private final Integer _myReplicaId;
    private final Integer[] _neighbours;
    private final Timer _timer;

    public GossipManager(String zooHost, String zooPort, Integer myReplicaId, Integer numReplicas, long gossipInterval){
        _myReplicaId = myReplicaId;
        _vecTimestamp = new VecTimestamp(numReplicas);
        _neighbours = findNeighbours(myReplicaId, numReplicas, getNumNeighbours(numReplicas));
        if (numReplicas != 1) {
            _replicasInfo = new ReplicasInfo(_neighbours, numReplicas);
        }
        _updateLog = new UpdateLog(numReplicas);
        _gossiper = new Gossiper(zooHost, zooPort);
        _timer = new Timer();
        if (numReplicas != 1) {
            _timer.scheduleAtFixedRate(new TimedGossip(this), gossipInterval * 1000, gossipInterval * 1000);
        }
    }

    public Integer getNumNeighbours(Integer numReplicas) {
        if (numReplicas == 1) {
            return 0;
        }
        if (numReplicas/3 != 0)
            return numReplicas/3;
        return 1;
    }

    public synchronized VecTimestamp getTimestamp() {
        return _vecTimestamp;
    }

    public void gossip() {
        int i = 0;
        for (int neighbour: _neighbours) {
            VecTimestamp myTimestamp;
            Vector<SortedMap<Integer, Update>> log;
            VecTimestamp vecTimestamp;
            synchronized (this) {
                myTimestamp = new VecTimestamp(getTimestamp());
                vecTimestamp = _replicasInfo.getInfoFromReplica(neighbour);
                log = _updateLog.getUpdatesFrom(vecTimestamp);
            }
            new Thread(() -> {
                try {
                    // gossip with another replica
                    _gossiper.gossipWith(neighbour, myTimestamp, log, _myReplicaId);
                    _replicasInfo.updateReplica(neighbour, myTimestamp);
                    System.out.println("Gossip with replica " + neighbour + " succeeded");
                } catch (ZKNamingException e) {
                    if (neighbour == 2) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println("Gossip with replica " + neighbour + " failed, ZkNaming");
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                        System.out.println("Gossip with replica " + neighbour + " failed, Unavailable");
                    }
                    else {
                        System.out.println("Gossip with replica " + neighbour + " threw an exception but succeeded");
                        _replicasInfo.updateReplica(neighbour, myTimestamp);
                    }
                } catch (InterruptedException e) {
                    System.out.println("The channel was interrupted due to time limit");
                }
            }).start();
        }
        cleanLog();
    }

    public synchronized ArrayList<Update> update(UpdtLog updateLog) {
        Integer replica = updateLog.getReplica();
        System.out.println("Received update from replica " + replica + " " + updateLog.getReplicasUpdatesList());
        ArrayList<Update> updates = _updateLog.merge(getTimestamp(), updateLog.getReplicasUpdatesList());
        VecTimestamp replicaTimestamp = new VecTimestamp(updateLog.getUpdateTimestamp());
        _vecTimestamp.merge(replicaTimestamp);
        _replicasInfo.updateReplica(replica, replicaTimestamp);
        return updates;
    }

    public void cleanLog() {
        VecTimestamp vecTimestamp = _replicasInfo.getLeastUpdated();
        _updateLog.removeUpdatesUntil(vecTimestamp);
    }

    public void addToObsLog(ArrayList<Obs> obsList) {
        Integer version = _vecTimestamp.getReplicaVersion(_myReplicaId) + 1;
        _updateLog.add(_myReplicaId, version, obsList, null);
        getTimestamp().increment(_myReplicaId);
    }

    public void addToCamLog(ArrayList<Cam> camList) {
        Integer version = _vecTimestamp.getReplicaVersion(_myReplicaId) + 1;
        _updateLog.add(_myReplicaId, version, null, camList);
        getTimestamp().increment(_myReplicaId);
    }

    //method that allows each replica to find the next n neighbours in a sequence so that they can gossip with them
    //example for 9 replicas and each one gossips with another 3:
    //         replica 1 gossips with 2, 3 and 4
    //         replica 2 gossips with 5, 6 and 7
    //         replica 3 gossips with 8, 9 and 1
    //         replica 4 gossips with 2, 3 and 5 and so on...
    public Integer[] findNeighbours(Integer myId, Integer numReplicas, Integer numNeighbours) {
        if (numReplicas == 1) {
            return null;
        }
        int firstNeig = ((myId - 1) * numNeighbours) % (numReplicas - 1) + 1;
        int followNeig = (firstNeig + numNeighbours) % (numReplicas - 1);
        if (followNeig == 0)
            followNeig = numReplicas - 1;
        Integer[] neighbours = new Integer[numNeighbours];
        int pos = 0;
        if (firstNeig < followNeig) {
            if (myId >= firstNeig && myId < followNeig) {
                for (int i = firstNeig; i < myId; i ++) {
                    neighbours[pos] = i;
                    pos++;
                }
                for (int i = myId; i < followNeig; i++) {
                    neighbours[pos] = i + 1;
                    pos++;
                }
            }
            else if (myId > followNeig){
                for (int i = firstNeig; i < followNeig; i ++){
                    neighbours[pos] = i;
                    pos++;
                }
            }
            else {
                for (int i = firstNeig; i < followNeig; i ++){
                    neighbours[pos] = i + 1;
                    pos++;
                }
            }
        }
        else {
            if (myId > firstNeig){
                for (int i = firstNeig; i < myId; i ++) {
                    neighbours[pos] = i;
                    pos++;
                }
                for (int i = myId; i <= (numReplicas - 1); i++) {
                    neighbours[pos] = i + 1;
                    pos++;
                }
                for (int i = 1; i < followNeig; i++) {
                    neighbours[pos] = i + 1;
                    pos++;
                }
            }
            else if (myId < followNeig){
                for (int i = firstNeig; i < (numReplicas - 1); i ++) {
                    neighbours[pos] = i;
                    pos++;
                }
                for (int i = 1; i < myId; i++) {
                    neighbours[pos] = i;
                    pos++;
                }
                for (int i = myId; i < followNeig; i++) {
                    neighbours[pos] = i + 1;
                    pos++;
                }
            }
            else {
                for (int i = firstNeig; i <= (numReplicas - 1); i ++){
                    neighbours[pos] = i + 1;
                    pos++;
                }
                for (int i = 1; i < followNeig; i ++){
                    neighbours[pos] = i;
                    pos++;
                }
            }

        }
        StringBuilder neig = new StringBuilder();
        for (int neighbour: neighbours) {
            neig.append(", ").append(neighbour);
        }
        System.out.println("I will gossip with" + neig);
        return neighbours;
    }

    public void closeThreads() {
        _timer.cancel();
    }
}
