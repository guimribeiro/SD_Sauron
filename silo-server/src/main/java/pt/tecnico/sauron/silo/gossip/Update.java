package pt.tecnico.sauron.silo.gossip;

import pt.tecnico.sauron.silo.grpc.Silo.*;


import java.util.List;

public class Update {
    private final List<Obs> _obsList;
    private final List<Cam> _camList;

    public Update(List<Obs> obsList, List<Cam> camList) {
        _obsList = obsList;
        _camList = camList;
    }

    public List<Obs> getObsList() {
        return _obsList;
    }

    public List<Cam> getCamList() {
        return _camList;
    }
}
