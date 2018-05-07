package projects.defaultProject.models.distributionModels;

import sinalgo.io.positionFile.PositionFileIO;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;

import java.io.LineNumberReader;

/**
 * A simple helper distribution model that allows to capture the position of a
 * set of nodes in a running simulation, and reproduce the same distribution
 * later on.
 * <p>
 * The first time the framework retrieves a position from this distribution
 * model, the user is asked to specify a file to read the positions from.
 * <p>
 * The file can be specified as a parameter to the model on the command line.
 */
public class PositionFile extends DistributionModel {

    private LineNumberReader reader = null;

    @Override
    public Position getNextPosition() {
        if (this.reader == null) {
            if (super.getParamString().equals("")) {
                this.reader = PositionFileIO.getPositionFileReader(null);
            } else {
                this.reader = PositionFileIO.getPositionFileReader(super.getParamString());
            }
        }
        return PositionFileIO.getNextPosition(this.reader);
    }
}
