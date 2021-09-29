package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.util.*;

import static java.lang.System.exit;

/**
 * Encapsulates gRPC channel and stub for remote service. All remote calls from
 * client should use this object.
 */
public class Frontend implements AutoCloseable {
    private ManagedChannel channel;
    private SauronGrpc.SauronBlockingStub stub;
    private final HashMap<String, CacheData> cacheTrack; private int cacheTrackIndex = 1;
    private final HashMap<String, CacheData> cacheTrackMatch; private int cacheTrackMatchIndex = 1;
    private final HashMap<String, CacheData> cacheTrace; private int cacheTraceIndex = 1;
    private final HashMap<String, CacheData> cacheCamInfo; private int cacheCamInfoIndex = 1;
    private CamJoinRequest camJoinRequest;
    private final ZKNaming zkNaming;
    private int cacheLimit = 5;
    private String serverId = null;

    public Frontend(String zooHost, String zooPort, String serverId) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);
        this.serverId = serverId;

        // System.out.println(zkNaming.listRecords("/grpc/sauron/silo"));
        ZKRecord record = zkNaming.lookup("/grpc/sauron/silo/" + serverId);
        String target = record.getURI();

        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // Create a blocking stub.
        this.stub = SauronGrpc.newBlockingStub(channel);

        // create caches
        this.cacheCamInfo = new HashMap<>();
        this.cacheTrack = new HashMap<>();
        this.cacheTrackMatch = new HashMap<>();
        this.cacheTrace = new HashMap<>();
    }

    public Frontend(String zooHost, String zooPort) throws ZKNamingException {
        zkNaming = new ZKNaming(zooHost, zooPort);

        connect();

        // create caches
        this.cacheCamInfo = new HashMap<>();
        this.cacheTrack = new HashMap<>();
        this.cacheTrackMatch = new HashMap<>();
        this.cacheTrace = new HashMap<>();
    }

    private void connect() throws ZKNamingException {
        if (channel != null) {
            close();
        }

        ZKRecord[] records = zkNaming.listRecords("/grpc/sauron/silo").toArray(ZKRecord[]::new);
        // System.out.println(zkNaming.listRecords("/grpc/sauron/silo"));
        int len = records.length;
        if (len == 0) {
            System.out.println("There are no replicas running");
            exit(0);
        }

        int index = (int) Math.round(Math.random() * (len - 1));
        String path = records[index].getPath();

        ZKRecord record = zkNaming.lookup(path);
        String target = record.getURI();

        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // Create a blocking stub.
        this.stub = SauronGrpc.newBlockingStub(channel);
    }

    public CamJoinResponse camJoin(CamJoinRequest request) {
        try {
            camJoinRequest = request;
            return stub.camJoin(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return camJoin(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
            else throw e;
        }
        return null;
    }

    public CamInfoResponse camInfo(CamInfoRequest request) {
        try {
            String name = request.getName();
            CamInfoResponse replicaResponse = stub.camInfo(request);

            if (cacheCamInfo.get(name) != null) { // if there is a response saved in cache for that request
                CamInfoResponse cachedResponse = cacheCamInfo.get(name).getCamInfoResponse(); // get that response
                return (CamInfoResponse) getNewest (cacheCamInfo, name, replicaResponse.getTimestamp(),
                        cachedResponse.getTimestamp(), replicaResponse, cachedResponse, new CacheData(replicaResponse, cacheCamInfoIndex));
            } else { // there is no cached response, save replica response
                saveReplicaResponse(cacheCamInfo, name, cacheCamInfoIndex, request, new CacheData(replicaResponse, cacheCamInfoIndex));
                cacheCamInfoIndex++;
                return replicaResponse;
            }
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return camInfo(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            } else if (cacheCamInfo.get(request.getName()) != null) {
                return cacheCamInfo.get(request.getName()).getCamInfoResponse();

            } else throw e;
        }
        return null;
    }

    public ReportResponse report(ReportRequest request) {
        try {
            return stub.report(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    camJoin(camJoinRequest);
                    return report(request);
                }
                else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
            else throw e;
        }
        return null;
    }

    public void saveReplicaResponse (HashMap<String, CacheData> cache, String id, int cacheIndex,
                                                              com.google.protobuf.GeneratedMessageV3 request, CacheData cacheData) {
        if (cacheIndex <= cacheLimit) {
            cache.put(id, cacheData);
        } else {
            Map.Entry<String, CacheData> minEntry = null;
            for (Map.Entry<String, CacheData> entry : cacheTrack.entrySet()) { // get the oldest cache entry to remove
                if (minEntry == null || entry.getValue().getIndex() < minEntry.getValue().getIndex()) {
                    minEntry = entry;
                }
            }
            assert minEntry != null;
            cacheTrack.remove(minEntry.getKey()); // remove the oldest cache entry
            cacheTrack.put(id, cacheData);
        }
    }

    public com.google.protobuf.GeneratedMessageV3 getNewest (HashMap<String, CacheData> cache, String id,
                                                             UpdateTimestamp timestampReplica, UpdateTimestamp timestampCache,
                                                             com.google.protobuf.GeneratedMessageV3 replicaResponse,
                                                             com.google.protobuf.GeneratedMessageV3 cachedResponse, CacheData cacheData) {
        VecTimestamp replicaTimestamp = Transform.UpdateTimestampIntoVecTimestamp(timestampReplica);
        Integer[] cachedUpdateArray = Transform.UpdateTimestampIntoUpdateArray(timestampCache);

        if (replicaTimestamp.greater_than(cachedUpdateArray)) { // if replica is response more recent than cached response
            /* it's irrelevant if the index of this entry is greater than the cache limit, because
             * this entry will replace an already existent entry in the cache. so, the limit will not be exceeded. */
            cache.put(id, cacheData);
            return replicaResponse;
        }
        return cachedResponse;
    }

    public TrackResponse track(TrackRequest request) {
        try {
            String id = request.getId();
            TrackResponse replicaResponse = stub.track(request);

            if (cacheTrack.get(id) != null) { // if there is a response saved in cache for that request
                TrackResponse cachedResponse = cacheTrack.get(id).getTrackResponse(); // get that response
                return (TrackResponse) getNewest (cacheTrack, id, replicaResponse.getTimestamp(),
                        cachedResponse.getTimestamp(), replicaResponse, cachedResponse, new CacheData(replicaResponse, cacheTrackIndex));

            } else if (!replicaResponse.getObs().getObjectId().isEmpty()) { // there is no cached response, save replica response
                saveReplicaResponse(cacheTrack, id, cacheTrackIndex, request, new CacheData(replicaResponse, cacheTrackIndex));
                cacheTrackIndex++;
            }
            return replicaResponse;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return track(request);
                }
                else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
            else throw e;
        }
        return null;
    }

    public TrackMatchResponse trackMatch(TrackMatchRequest request) {
        try {
            String id = request.getId();
            TrackMatchResponse replicaResponse = stub.trackMatch(request);

            if (cacheTrackMatch.get(id) != null) { // if there is a response saved in cache for that request
                TrackMatchResponse cachedResponse = cacheTrackMatch.get(id).getTrackMatchResponse(); // get that response

                return (TrackMatchResponse) getNewest (cacheTrackMatch, id, replicaResponse.getTimestamp(),
                        cachedResponse.getTimestamp(), replicaResponse, cachedResponse, new CacheData(replicaResponse, cacheTrackMatchIndex));
            } else if (!replicaResponse.getObsListList().isEmpty()) { // there is no cached response, save replica response
                saveReplicaResponse(cacheTrackMatch, id, cacheTrackMatchIndex, request, new CacheData(replicaResponse, cacheTrackMatchIndex));
                cacheTrackMatchIndex++;
            }
            return replicaResponse;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return trackMatch(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
            else throw e;
        }
        return null;
    }

    public TraceResponse trace(TraceRequest request) {
        try {
            String id = request.getId();
            TraceResponse replicaResponse = stub.trace(request);

            if (cacheTrace.get(id) != null) { // if there is a response saved in cache for that request
                TraceResponse cachedResponse = cacheTrace.get(id).getTraceResponse(); // get that response

                return (TraceResponse) getNewest (cacheTrace, id, replicaResponse.getTimestamp(),
                        cachedResponse.getTimestamp(), replicaResponse, cachedResponse, new CacheData(replicaResponse, cacheTraceIndex));
            } else if (!replicaResponse.getObsListList().isEmpty()) { // there is no cached response, save replica response
                saveReplicaResponse(cacheTrace, id, cacheTraceIndex, request, new CacheData(replicaResponse, cacheTraceIndex));
                cacheTraceIndex++;
            }
            return replicaResponse;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return trace(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
            else throw e;
        }
        return null;
    }

    public void setCacheLimit(int limit) {
        // set the cache limit value, the default is 5
        cacheLimit = limit;
    }

    public PingResponse ctrl_ping(PingRequest request) {
        try {
            return stub.ctrlPing(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return ctrl_ping(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
        }
        return null;
    }

    public ClearResponse ctrl_clear(ClearRequest request) {
        try {
            return stub.ctrlClear(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return ctrl_clear(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
        }
        return null;
    }

    public InitResponse ctrl_init(InitRequest request) {
        try {
            return stub.ctrlInit(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (serverId == null) {
                    try {
                        connect();
                    } catch (ZKNamingException exception) {
                        exception.getMessage();
                    }
                    return ctrl_init(request);
                } else {
                    System.out.println("Impossible to communicate with the instance " + serverId);
                    exit(0);
                }
            }
        }
        return null;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}