/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.defaultProject.models.interferenceModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.models.InterferenceModel;
import sinalgo.nodes.Position;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.Global;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.logging.LogL;

/**
 * This interference model determines a quotient q = s / (i+n) between the
 * received signal and the sum of the ambient background noise n and the
 * interference caused by all concurrent transmissions. The transmission
 * succeeds if q > beta, where beta is a small constant. <br>
 * This model assumes that the intensity of an electric signal decays
 * exponentially with the distance from the sender. This decrease is
 * parameterized by the path-loss exponent alpha: Intensity(r) =
 * sendPower/r^alpha. The value of alpha is often chosen in the range between 2
 * and 6. <br>
 * To the interference caused by concurrent transmissions, we add an ambient
 * noise level N. <br>
 * <br>
 * This model requires the following entry in the configuration file: <br>
 * &lt;SINR alpha="..." beta="..." noise="..."/&gt; <br>
 * where alpha, beta, and noise are three floating point values.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class SINR extends InterferenceModel {

    private int alpha;// the path-loss exponent, good fefault value would be 2
    private double beta;// the threshold, good default value would be 0.5
    private double ambientNoise; // the ambient noise, good default value would be 0

    /**
     * The constructor for the SignalToInterference class.
     */
    public SINR() {
        try {
            this.setAlpha(Configuration.getIntegerParameter("SINR/alpha"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException("The configuration entry SINR/alpha is not a valid double:\n\n" + e.getMessage());
        }
        try {
            this.setBeta(Configuration.getDoubleParameter("SINR/beta"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException("The configuration entry SINR/beta is not a valid double:\n\n" + e.getMessage());
        }
        try {
            this.setAmbientNoise(Configuration.getDoubleParameter("SINR/noise"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException("The configuration entry SINR/noise is not a valid double:\n\n" + e.getMessage());
        }
    }

    @Override
    public boolean isDisturbed(Packet p) {
        Position receiverPos = p.getDestination().getPosition();
        double distanceFromSource = p.getOrigin().getPosition().distanceTo(receiverPos);
        double poweredDistanceFromSource = Math.pow(distanceFromSource, this.getAlpha());

        double signal = p.getIntensity() / poweredDistanceFromSource;

        double noise = this.getAmbientNoise();

        for (Packet pack : SinalgoRuntime.getPacketsInTheAir()) { // iterate over all active packets
            if (pack == p) {
                continue; // that's the packet we want
            }
            if (pack.getOrigin().getID() == p.getDestination().getID()) {
                // the receiver node of p is sending a packet itself
                if (!Configuration.isCanReceiveWhileSending()) {
                    return true;
                }
                continue; // the interference created from this sender is not considered
            }
            // Detect multiple packets that want to arrive in parallel at the same
            // destination.
            if (!Configuration.isCanReceiveMultiplePacketsInParallel() && pack.getDestination().getID() == p.getDestination().getID()) {
                return true;
            }

            Position pos = pack.getOrigin().getPosition();
            double distance = pos.distanceTo(receiverPos);
            double poweredDistance = Math.pow(distance, this.getAlpha());
            noise += pack.getIntensity() / poweredDistance;
        }

        boolean disturbed = signal < this.getBeta() * noise;

        if (LogL.INTERFERENCE_DETAIL) {
            Global.getLog().logln("Node " + p.getDestination().getID() + " is checking a packet from " + p.getOrigin().getID());
            if (disturbed) {
                Global.getLog().logln("Dropped the message due to too much interference.");
            }
        }

        return disturbed;
    }

}
