package projects.defaultProject.nodes.timers;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.timers.Timer;

/**
 * A timer that sends a message to a given node when it fires, 
 * independent of the underlying network graph. I.e. the timer
 * uses the sendDirect() method to deliver the message.
 * <p>
 * The message is sent by the node who starts the timer.
 */
public class DirectMessageTimer extends Timer {
	Message msg; // the msg to send
	Node target; // the node to send the msg to
	
	/**
	 * Create a new timer for the given message.
	 * @param m The message to send to the node n.
	 * @param n The node to send the msg to
	 */
	public DirectMessageTimer(Message m, Node n) {
		msg = m;
		target = n;
	}
	
	@Override
	public void fire() {
		this.node.sendDirect(msg, target);
	}
}
