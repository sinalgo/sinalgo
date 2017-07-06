package sinalgo.io.versionTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.runtime.Main;

public class VersionTester extends Thread {
	
	private static boolean isRunning = false;
	private static boolean displayIfOK = false;
	
	/**
	 * Tests whether the installed version of Sinalgo is the most recent one.
	 * @param testIfEnabled Set to true if the check should only be performed
	 * if the AppConfig file is configured to perform update checks.
	 * @param displayIfVersionMatch Set to true if a message should also be displayed if the installed version is up to date.
	 */
	public static void testVersion(boolean testIfEnabled, boolean displayIfVersionMatch) {
		displayIfOK = displayIfVersionMatch;
		if(isRunning) {
			return;
		}
		if(testIfEnabled) {
			if(!AppConfig.getAppConfig().checkForSinalgoUpdate) {
				return;
			}
			long last = AppConfig.getAppConfig().timeStampOfLastUpdateCheck;
			if(last + 86400000 > System.currentTimeMillis()) {
				return;
			}
		}
		VersionTester vt = new VersionTester();
		vt.start();
	}
	
	public void run() {
		isRunning = true;
		try {
			URL url = new URL("http://dcg.ethz.ch/projects/sinalgo/version");
			URLConnection con = url.openConnection();
			con.setDoOutput(true);
			con.setDoInput(true);
			con.connect();
			PrintStream ps = new PrintStream(con.getOutputStream());
			ps.println("GET index.html HTTP/1.1");
			ps.flush();
			
			// read the input
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line = in.readLine(); // we're only interested in the very first line
			if(line != null) {
				//System.out.println("Most recent version: " + line);
				//System.out.println("Current version    : " + Configuration.versionString);
				if(line.equals(Configuration.versionString)) {
					if(displayIfOK) {
						Main.info("You are using the most recent version of Sinalgo.");
					}
				} else {
					String msg =
						"\n" +
						"+----------------------------------------------------------------------\n" +
						"| You are currently running Sinalgo " + Configuration.versionString + ".\n" + 
						"| A more recent version of Sinalgo is available (" + line + ")\n" +
						"+---------------------------------------------------------------------\n" +
						"| To download the latest version, please visit\n" +
						"| http://sourceforge.net/projects/sinalgo/\n" +
						"+---------------------------------------------------------------------\n" +
						"| You may turn off these version checks through the 'Settings' dialog.\n" +
						"| Note:   Sinalgo automatically tests for updates at most once\n" +
						"|         every 24 hours.\n" + 
						"+---------------------------------------------------------------------\n";
					Main.warning(msg);
				}
			}
		} catch(Exception e) {
			String msg = "\n" +
			">----------------------------------------------------------------------\n" +
			"> Unable to test for updates of Sinalgo. The installed version\n" +
			"> is "+ Configuration.versionString + "\n" +  
			">---------------------------------------------------------------------\n" +
			"> To check for more recent versions, please visit\n" +
			"> http://sourceforge.net/projects/sinalgo/\n" +
			">---------------------------------------------------------------------\n" +
			"> You may turn off these version checks through the 'Settings' dialog.\n" + 
			"| Note:   Sinalgo automatically tests for updates at most once\n" +
			"|         every 24 hours.\n" + 
			">---------------------------------------------------------------------\n";
			Main.warning(msg);
		} finally {
			isRunning = false;
			AppConfig.getAppConfig().timeStampOfLastUpdateCheck = System.currentTimeMillis();
		}
	}
}


