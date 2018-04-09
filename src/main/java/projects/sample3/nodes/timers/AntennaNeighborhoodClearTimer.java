package projects.sample3.nodes.timers;

import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

import java.util.TreeSet;

/**
 * Timer to clear a tree-set
 */
public class AntennaNeighborhoodClearTimer extends Timer {

    private TreeSet<Node> set;

    public AntennaNeighborhoodClearTimer(TreeSet<Node> ts) {
        set = ts;
    }

    @Override
    public void fire() {
        set.clear();
    }

}
