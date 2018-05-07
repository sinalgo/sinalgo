package projects.sample3.nodes.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

@Getter
@Setter
@AllArgsConstructor
public class SmsMessage extends Message {

    private int seqID; // sequence ID of the sender
    private Node receiver;
    private Node sender;
    private String text;
    private SmsTimer smsTimer;

    @Override
    public Message clone() {
        return this;
    }

}
