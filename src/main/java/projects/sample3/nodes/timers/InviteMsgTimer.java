package projects.sample3.nodes.timers;

import lombok.Getter;
import lombok.Setter;
import projects.sample3.nodes.messages.InviteMessage;
import projects.sample3.nodes.nodeImplementations.Antenna;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.nodes.timers.Timer;
import sinalgo.tools.statistics.Distribution;

/**
 * The antennas send periodically an invite message - this timer triggers the
 * broadcast.
 */
public class InviteMsgTimer extends Timer {

    private Distribution dist;
    private int refreshRate;
    private int refreshCounter;

    // If set to true, the antenna requires the nodes to register again
    // such that it can drop old mobileNodes
    @Getter
    @Setter
    private boolean requireSubscription;

    public InviteMsgTimer() {
        try {
            this.dist = Distribution.getDistributionFromConfigFile("Antenna/InviteIntervall");
            this.refreshRate = Configuration.getIntegerParameter("Antenna/refreshRate");
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
    }

    @Override
    public void fire() {
        InviteMessage msg = new InviteMessage();
        this.refreshCounter--;
        if (this.refreshCounter <= 0) {
            ((Antenna) this.getTargetNode()).resetNeighborhood();
            msg.setRequireSubscription(true);
            this.refreshCounter = this.refreshRate; // reset the counter
        }

        this.getTargetNode().broadcast(msg);
        double time = this.dist.nextSample();
        if (time <= 0) {
            throw new SinalgoFatalException("Invalid offset time for inviteInterval: " + time + " is <= 0.");
        }
        this.startRelative(time, this.getTargetNode());
    }

}
