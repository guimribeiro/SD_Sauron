package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.Silo.*;

public class CacheData {
    private TrackResponse _trackResponse;
    private TrackMatchResponse _trackMatchResponse;
    private TraceResponse _traceResponse;
    private CamInfoResponse _camInfoResponse;
    private final Integer _index;

    CacheData(TrackResponse trackResponse, Integer index) {
        _trackResponse = trackResponse;
        _index = index;
    }

    CacheData(TrackMatchResponse trackMatchResponse, Integer index) {
        _trackMatchResponse = trackMatchResponse;
        _index = index;
    }

    CacheData(TraceResponse traceResponse, Integer index) {
        _traceResponse = traceResponse;
        _index = index;
    }

    CacheData(CamInfoResponse camInfoResponse, Integer index) {
        _camInfoResponse = camInfoResponse;
        _index = index;
    }

    public TrackResponse getTrackResponse() {
        return _trackResponse;
    }

    public void setTrackResponse(TrackResponse trackResponse) {
        _trackResponse = trackResponse;
    }

    public TrackMatchResponse getTrackMatchResponse() {
        return _trackMatchResponse;
    }

    public void setTrackMatchResponse(TrackMatchResponse trackMatchResponse) {
        _trackMatchResponse = trackMatchResponse;
    }

    public TraceResponse getTraceResponse() {
        return _traceResponse;
    }

    public void setTraceResponse(TraceResponse traceResponse) {
        _traceResponse = traceResponse;
    }

    public CamInfoResponse getCamInfoResponse() {
        return _camInfoResponse;
    }

    public void setCamInfoResponse(CamInfoResponse camInfoResponse) {
        _camInfoResponse = camInfoResponse;
    }

    public Integer getIndex() {
        return _index;
    }
}
