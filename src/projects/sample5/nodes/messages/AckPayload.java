package projects.sample5.nodes.messages;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

/**
 * Sent back upon receipt of a payload msg to acknowledge the receipt.
 * <p>
 * The superclass contains the message that was originally sent to the receiver.
 */
public class AckPayload extends PayloadMsg {
	
	/**
	 * Constructor
	 * @param destination The node to whom the ACK should be sent
	 * @param sender The node that acknowledges receipt of a message
	 */
	public AckPayload(Node destination, Node sender) {
		super(destination, sender);
	}

	@Override
	public Message clone() {
		return this; // read-only policy
	}

}
