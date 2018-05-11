package projects.defaultProject.models.distributionModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private LineNumberReader reader;

    @Override
    public Position getNextPosition() {
        if (this.getReader() == null) {
            if (super.getParamString().equals("")) {
                this.setReader(PositionFileIO.getPositionFileReader(null));
            } else {
                this.setReader(PositionFileIO.getPositionFileReader(super.getParamString()));
            }
        }
        return PositionFileIO.getNextPosition(this.getReader());
    }

}
