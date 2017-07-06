package projects.sample5.nodes.timers;

import projects.sample5.nodes.messages.PayloadMsg;
import projects.sample5.nodes.nodeImplementations.FNode;
import sinalgo.nodes.timers.Timer;

/**
 * A timer to initially send a message. This timer
 * is used in synchronous simulation mode to handle user
 * input while the simulation is not running. 
 */
public class PayloadMessageTimer extends Timer {
	PayloadMsg msg = null; // the msg to send

	/**
	 * @param msg The message to send
	 */
	public PayloadMessageTimer(PayloadMsg msg) {
		this.msg = msg;
	}
	
	@Override
	public void fire() {
		((FNode) node).sendPayloadMessage(msg);
	}

}
