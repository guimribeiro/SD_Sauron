package pt.tecnico.sauron.silo.gossip;

import pt.tecnico.sauron.silo.gossip.GossipManager;

import java.util.TimerTask;

public class TimedGossip extends TimerTask {
    GossipManager _gossipManager;
    TimedGossip(GossipManager gossipManager) {
        _gossipManager = gossipManager;
    }
    public void run() {
        _gossipManager.gossip();
    }
}

