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
package projects.defaultProject.models.connectivityModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.models.ConnectivityModelHelper;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;

/**
 * Class to implement the UnitDiskGraph Connectivity Model. This means, that two
 * nodes are only connected, if they are closer than a certain distance,
 * otherwise not.
 * <p>
 * The critical distance that determines whether two nodes are in proximite must
 * be specified in the XML configuration file in the following way:
 *
 * <pre>
 * &lt;UDG&gt;
 * &nbsp;&lt;rMax value="10"/&gt; &lt;!-- The maximal radius for nodes to have a connection --&gt;
 * &lt;/UDG&gt;
 * </pre>
 * <p>
 * If this entry is not present, the value of <code>rMax</code> from the
 * GeometricNodeCollection is used.
 */
public class UDG extends ConnectivityModelHelper {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private double squareRadius;

    @Override
    protected boolean isConnected(Node from, Node to) {
        Position p1 = from.getPosition();
        Position p2 = to.getPosition();

        double distance = p1.squareDistanceTo(p2);
        return (distance < this.getSquareRadius());
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // -
    // Code to initialize the static variables of this class
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // -

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static boolean initialized; // indicates whether the static fields of this class have already been
    // initialized

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static double rMaxSquare; // we reuse the rMax value from the GeometricNodeCollection.

    /**
     * @return The maximum transmission range of this UDG model.
     */
    public double getMaxTransmissionRange() {
        return Math.sqrt(this.getSquareRadius());
    }

    /**
     * Sets the maximum transmission range of this UDG model.
     *
     * @param rMax The new max. transmission range.
     */
    public void setMaxTransmissionRange(double rMax) {
        this.setSquareRadius(rMax * rMax);
    }

    public UDG(double rMax) {
        this.setSquareRadius(rMax * rMax);
    }

    /**
     * The default constructor for this class.
     * <p>
     * The first time this constructor is called, it initializes the static
     * parameters of this class.
     *
     * @throws CorruptConfigurationEntryException If one of the initialization steps fails.
     */
    public UDG() throws CorruptConfigurationEntryException {
        if (!isInitialized()) {
            double geomNodeRMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
            try {
                setRMaxSquare(Configuration.getDoubleParameter("UDG/rMax"));
            } catch (CorruptConfigurationEntryException e) {
                Global.getLog().logln(
                        "\nWARNING: There is no entry 'UDG/rMax' in the XML configuration file. This entry specifies the max. transmission range for the UDG connectivity model.\nThe simulation now uses GeometricNodeCollection/rMax instead.\n");
                setRMaxSquare(geomNodeRMax);
            }
            if (getRMaxSquare() > geomNodeRMax) { // dangerous! This is probably not what the user wants!
                Main.minorError(
                        "WARNING: The maximum transmission range used for the UDG connectivity model is larger than the maximum transmission range specified for the GeometricNodeCollection.\nAs a result, not all connections will be found! Either fix the problem in the project-specific configuration file or the '-overwrite' command line argument.");
            }

            setRMaxSquare(getRMaxSquare() * getRMaxSquare());

            setInitialized(true);
        }
        this.setSquareRadius(getRMaxSquare());
    }
    
}
