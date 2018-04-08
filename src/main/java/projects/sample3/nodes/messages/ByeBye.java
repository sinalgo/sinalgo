package projects.sample3.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * Sent by a mobile node when it decides to switch to another antenna
 */
public class ByeBye extends Message {

	@Override
	public Message clone() {
		return this; // read-only policy
	}

}
