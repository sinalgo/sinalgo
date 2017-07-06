package projects.sample3.nodes.messages;

import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

public class SmsMessage extends Message {
	public int seqID; // sequence ID of the sender
	public Node receiver;
	public Node sender;
	public String text;
	public SmsTimer smsTimer;
	
	public SmsMessage(int aSeqID, Node aReceiver, Node aSender, String aText, SmsTimer aTimer) {
		this.seqID = aSeqID; 
		this.receiver = aReceiver;
		this.sender = aSender;
		this.text = aText;
		this.smsTimer = aTimer;
	}
	
	@Override
	public Message clone() {
		return this;
	}

}
