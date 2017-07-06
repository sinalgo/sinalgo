package projects.sample3.nodes.timers;

import java.util.TreeSet;

import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

/**
 * Timer to clear a tree-set
 */
public class AntennaNeighborhoodClearTimer extends Timer {
	TreeSet<Node> set = null;
	public AntennaNeighborhoodClearTimer(TreeSet<Node> ts) {
		set = ts;
	}
	
	@Override
	public void fire() {
		set.clear();
	}

}
