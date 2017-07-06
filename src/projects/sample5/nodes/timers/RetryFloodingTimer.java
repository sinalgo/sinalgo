package projects.sample5.nodes.timers;

import projects.sample5.nodes.nodeImplementations.FNode;
import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

/**
 * A timer that is set when flooding. When there is no response after
 * the given amount of time, this timer tirggers another flooding
 * search for the destination, with increased TTL.
 */
public class RetryFloodingTimer extends Timer {
	
	Node destination; // the node to look for
	int currentTTL; // the TTL used in the current search
	boolean isActive = true; // used to disable this timer. If false, this timer does perform its action anymore.
	
	/**
	 * Removing a timer-event from the global list of all events may 
	 * be quite expensive. Therefore, we simply deactivate this timer
	 * obejct by setting a flag to false. When the timer fires, 
	 * it will not perform action if the isActive flag is not set.  
	 */
	public void deactivate() {
		isActive = false;
	}
	
	/**
	 * @param destination The node to find
	 * @param currentTTL The TTL used in the current flooding search
	 */
	public RetryFloodingTimer(Node destination, int currentTTL) {
		this.destination = destination;
		this.currentTTL = currentTTL;
		this.isActive = true;
	}

	@Override
	public void fire() {
		if(isActive){
			FNode n = (FNode) this.node;
			n.lookForNode(destination, currentTTL * 2); // restart a flooding search with TTL twice as big
		}
	}
}
