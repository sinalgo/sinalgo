package projects.sample3.nodes.timers;

import projects.sample3.nodes.messages.InviteMessage;
import projects.sample3.nodes.nodeImplementations.Antenna;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.nodes.timers.Timer;
import sinalgo.tools.Tools;
import sinalgo.tools.statistics.Distribution;

/**
 * The antennas send periodically an invite message - this timer triggers 
 * the broadcast.
 */
public class InviteMsgTimer extends Timer {
	Distribution dist = null;
	int refreshRate = 0;
	int refreshCounter = 0;
	
	// If set to true, the antenna requires the nodes to register again
	// such that it can drop old mobileNodes 
	public boolean requireSubscription = false; 
	
	public InviteMsgTimer() {
		try {
			dist = Distribution.getDistributionFromConfigFile("Antenna/InviteIntervall");
			refreshRate = Configuration.getIntegerParameter("Antenna/refreshRate");
		} catch (CorruptConfigurationEntryException e) {
			Tools.fatalError(e.getMessage());
		}
	}
	
	@Override
	public void fire() {
		InviteMessage msg = new InviteMessage();
		refreshCounter--;
		if(refreshCounter <= 0) {
			((Antenna) this.node).resetNeighborhood();
			msg.requireSubscription = true;
			refreshCounter = refreshRate; // reset the counter
		}
		
		this.node.broadcast(msg);
		double time = dist.nextSample();
		if(time <=0) {
			Tools.fatalError("Invalid offset time for inviteInterval: " + time + " is <= 0.");
		}
		this.startRelative(time, this.node);
	}
}
