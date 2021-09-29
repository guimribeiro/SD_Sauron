package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.Storage;
import pt.tecnico.sauron.silo.exceptions.Storage.StorageException;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import static io.grpc.Status.INVALID_ARGUMENT;

public class SiloServiceImpl extends SauronGrpc.SauronImplBase {
    private static final Logger LOGGER = Logger.getLogger(SiloServiceImpl.class.getName());

    private final Storage storage;

    SiloServiceImpl(String zooHost, String zooPort, Integer myReplicaId, Integer numReplicas, long gossipInterval) {
        storage = new Storage(zooHost, zooPort, myReplicaId, numReplicas, gossipInterval);
    }

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        String name = request.getName();
        Double coord1 = request.getCoord1();
        Double coord2 = request.getCoord2();
        try {
            storage.camJoin(name, coord1, coord2);
            CamJoinResponse response = CamJoinResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        String name = request.getName();

        try {
            UpdateTimestamp updateTimestamp = storage.getTimestamp().transform();
            Double[] coords = storage.camInfo(name);

            CamInfoResponse response = CamInfoResponse.newBuilder()
                .setTimestamp(updateTimestamp)
                .setCoord1(coords[0])
                .setCoord2(coords[1])
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch(StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        String name = request.getName();
        List<Obs> obs = request.getObsListList();
        try {
            storage.report(name, obs);
            ReportResponse response = ReportResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        ObjectType objectType = request.getObjectType();
        String id = request.getId();
        UpdateTimestamp updateTimestamp = storage.getTimestamp().transform();
        Observation observation = storage.track(objectType, id);
        Obs obs;
        if (observation == null) {
            obs = Obs.newBuilder().build();
        }
        else {
            obs = Transform.ObservationIntoObs(observation);
        }
        TrackResponse response = TrackResponse.newBuilder()
                .setTimestamp(updateTimestamp)
                .setObs(obs)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        ObjectType objectType = request.getObjectType();
        String id = request.getId();
        try {
            UpdateTimestamp updateTimestamp = storage.getTimestamp().transform();
            List<Obs> obsList = Transform.ObservationListToObsList(storage.trackMatch(objectType, id));
            TrackMatchResponse response = TrackMatchResponse.newBuilder()
                    .setTimestamp(updateTimestamp)
                    .addAllObsList(obsList)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        ObjectType objectType = request.getObjectType();
        String id = request.getId();
        UpdateTimestamp updateTimestamp = storage.getTimestamp().transform();
        List<Observation> observation = storage.trace(objectType, id);
        List<Obs> obsList;
        if (observation == null) {
            obsList = new ArrayList<>();
        }
        else {
            obsList = Transform.ObservationListToObsList(storage.trace(objectType, id));
        }
        TraceResponse response = TraceResponse.newBuilder()
                .setTimestamp(updateTimestamp)
                .addAllObsList(obsList)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlPing(PingRequest request,
                         StreamObserver<PingResponse> responseObserver) {

        String input = request.getTxt();
        if (input == null || input.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Input cannot be empty!").asRuntimeException());
        }
        String output = "Hello " + input + "!";
        PingResponse response = PingResponse.newBuilder().
                setTxt(output).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlClear(ClearRequest request,
                          StreamObserver<ClearResponse> responseObserver) {
        storage.clear();
        ClearResponse response = ClearResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlInit(InitRequest request, StreamObserver<InitResponse> responseObserver){
        List<Obs> obsList = request.getObsListList();
        List<Cam> camList = request.getCamListList();
        try {
            storage.init(obsList, camList);
            InitResponse response = InitResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void gossip(UpdtLog request, StreamObserver<GossipResponse> responseObserver){
        try {
            storage.receiveGossip(request);
            GossipResponse response = GossipResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StorageException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    public void closeThreads() {
        storage.closeThreads();
    }
}
