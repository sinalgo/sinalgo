package sinalgo.io.positionFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;

import javax.swing.JFileChooser;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.io.eps.Exporter.PositionFileFilter;
import sinalgo.io.eps.Exporter.SingleFileFilter;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.tools.Tools;

/**
 * @author rflury
 *
 */
public class PositionFileIO {

	private static final String separator = "#####----- start of node posiitons -----#####";
	
	/**
	 * Creates a file containing a list of the positions
	 * of all nodes currently hold by the framework.
	 * @param The name of the file, null if the user should be asked using a file-dialog 
	 * @return true upon success, otherwise false. 
	 */
	public static boolean printPos(String name) {
		if(name == null) {
			JFileChooser fc = new JFileChooser(AppConfig.getAppConfig().getLastSelectedFileDirectory());
			fc.setDialogTitle("Select destination file");
			SingleFileFilter posFf = new PositionFileFilter();
			fc.setFileFilter(posFf);
			if(fc.showSaveDialog(Tools.getGUI()) == JFileChooser.APPROVE_OPTION){
				name = fc.getSelectedFile().getAbsolutePath();
				String p = name;
				p = p.substring(0, p.length() - fc.getSelectedFile().getName().length()); // remember the selected path
				AppConfig.getAppConfig().lastSelectedFileDirectory = p;
			} else {
				return false; // (aborted)
			}
		}

		try {
			PrintStream ps = new PrintStream(name);
			// header contains # of nodes and dimension of deployment field
			ps.println("Number of nodes: " + Tools.getNodeList().size());
			Configuration.printConfiguration(ps);
			ps.println(separator);

			for(Node n : Tools.getNodeList()) {
				Position p = n.getPosition();
				ps.println(p.xCoord + ", " + p.yCoord + ", " + p.zCoord);
			}
			ps.close();
			return true;
		} catch (FileNotFoundException e) {
			Tools.minorError(e.getMessage());
		}
		return false;
	}
	
	/**
	 * Opens a file dialog that lets the user select the position file
	 * to read from.
	 * @param  fileName The name of the file to open, null if a dialog to choose the file should be shown
	 * @return A reader instance of the selected file
	 * @throws PositionFileException
	 */
	public static LineNumberReader getPositionFileReader(String fileName) throws PositionFileException {
		LineNumberReader reader = null;
		String name = null;
		
		if(fileName == null) {
			JFileChooser fc = new JFileChooser(AppConfig.getAppConfig().getLastSelectedFileDirectory());
			fc.setDialogTitle("Select input file");
			SingleFileFilter posFf = new PositionFileFilter();
			fc.setAcceptAllFileFilterUsed(true);
			fc.setFileFilter(posFf);
			if(fc.showOpenDialog(Tools.getGUI()) != JFileChooser.APPROVE_OPTION){
				throw new PositionFileException("Aborted file selection");
			}
			name = fc.getSelectedFile().getPath();
			String p = name;
			p = p.substring(0, p.length() - fc.getSelectedFile().getName().length()); // remember the selected path
			AppConfig.getAppConfig().lastSelectedFileDirectory = p;
		} else {
			name = fileName;
		}

		try {
			reader = new LineNumberReader(new FileReader(new File(name)));
		} catch (FileNotFoundException e) {
			throw new PositionFileException(e.getMessage());
		}
		try {
			// skip the first lines
			String numNodes = reader.readLine();
			while(numNodes != null && !numNodes.equals(separator)) {
				numNodes = reader.readLine(); 
			}
		} catch (IOException e) {
			throw new PositionFileException(e.getMessage());
		}
		return reader;
	}
	
	public static Position getNextPosition(LineNumberReader reader) {
		try {
			String line = reader.readLine();
			if(line == null) {
				throw new PositionFileException("The specified file contains not enough positions");	
			}
			try {
				String[] parts = line.split(",");
				if(parts.length < 3) {
					throw new PositionFileException("Illegal line: expected three doubles, separated by comma. Found \n" + line);	
				}
				double x = Double.parseDouble(parts[0]);
				double y = Double.parseDouble(parts[1]);
				double z = Double.parseDouble(parts[2]);
				return new Position(x,y,z);
			} catch(NumberFormatException e) {
				throw new PositionFileException("Illegal line: expected three doubles, separated by comma. Found \n" + line);
			}
		} catch (IOException e) {
			throw new PositionFileException(e.getMessage());
		}
	}
	
	
	@SuppressWarnings("serial")
	public static class PositionFileException extends WrongConfigurationException { // needs not be caught
		public PositionFileException(String msg) {
			super(msg);
		}
	}
}
