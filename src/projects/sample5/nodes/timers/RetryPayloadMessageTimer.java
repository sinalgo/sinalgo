package projects.sample5.nodes.timers;

import projects.sample5.nodes.messages.PayloadMsg;
import projects.sample5.nodes.nodeImplementations.FNode;
import sinalgo.nodes.timers.Timer;

/**
 * Fires when there is no ACK within a given time to the
 * payload message
 */
public class RetryPayloadMessageTimer extends Timer {
	
	PayloadMsg message; // the msg to send
	boolean isActive = true; // flag that indicates whether the timer should still perform its action
	
	/**
	 * Call this method if this timer should not perform any action anymore.
	 * (This is cheaper than to remove the timer manually from the list) 
	 */
	public void deactivate() {
		isActive = false;
	}
	
	/**
	 * @param message The message to resend
	 */
	public RetryPayloadMessageTimer(PayloadMsg message) {
		this.message = message;
	}
	
	@Override
	public void fire() {
		if(isActive) {
			FNode n = (FNode) this.node;
			n.sendPayloadMessage(message); 
			// we could also invalidate the routing entry, and search again
		}
	}

}
