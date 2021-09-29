package pt.tecnico.sauron.silo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.gossip.Update;
import pt.tecnico.sauron.silo.gossip.VecTimestamp;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.util.SortedMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates gRPC channel and stub for remote service. All remote calls from
 * client should use this object.
 */
public class Gossiper {
    private final String _zooHost;
    private final String _zooPort;

    public Gossiper(String zooHost, String zooPort) {
        _zooHost = zooHost;
        _zooPort = zooPort;
    }

    public void gossipWith (Integer serverId, VecTimestamp vecTimestamp, Vector<SortedMap<Integer, Update>> updateLog, Integer myId) throws ZKNamingException, InterruptedException {
        ZKNaming zkNaming = new ZKNaming(_zooHost, _zooPort);
        ZKRecord record = zkNaming.lookup("/grpc/sauron/silo/" + serverId);
        String target = record.getURI();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // Create a blocking stub.
        SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);
        // stub.camJoin(CamJoinRequest.newBuilder().setName("cam").setCoord1(1).setCoord2(2).build());
        UpdtLog updtLog = Transform.UpdateLogIntoUpdateLog(vecTimestamp, updateLog, myId);
        stub.gossip(updtLog);
        channel.shutdown();
        channel.awaitTermination(5, TimeUnit.SECONDS);
    }
}
