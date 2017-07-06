package projects.defaultProject.models.distributionModels;

import java.io.LineNumberReader;

import sinalgo.io.positionFile.PositionFileIO;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;


/**
 * A simple helper distribution model that allows to capture the position
 * of a set of nodes in a running simulation, and reproduce the same
 * distribution later on.
 * <p>
 * The first time the framework retrieves a position from this distribution model,
 * the user is asked to specify a file to read the positions from.
 * <p>
 * The file can be specified as a parameter to the model on the command line.
 */
public class PositionFile extends DistributionModel {

	LineNumberReader reader = null;	
	
	@Override
	public Position getNextPosition() {
		if(reader == null) {
			if(super.getParamString().equals("")) {
				reader = PositionFileIO.getPositionFileReader(null);
			} else {
				reader = PositionFileIO.getPositionFileReader(super.getParamString());
			}
		}
		return PositionFileIO.getNextPosition(reader);
	}
}
