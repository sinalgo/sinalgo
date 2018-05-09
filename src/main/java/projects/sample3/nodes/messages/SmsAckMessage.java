package projects.sample3.nodes.messages;

import lombok.Getter;
import lombok.Setter;
import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.nodes.Node;

public class SmsAckMessage extends SmsMessage {

    @Getter
    @Setter
    private int smsSeqID; // the sequence ID of the message to ACK

    public SmsAckMessage(int aSeqID, Node aReceiver, Node aSender, String aText, SmsTimer aTimer) {
        super(aSeqID, aReceiver, aSender, aText, aTimer);
    }

}
