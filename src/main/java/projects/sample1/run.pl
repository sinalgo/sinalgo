#!/usr/bin/perl

# Demo script to automatically start project sample1 several times with
# a different set of node speeds and node densities. Note that this script
# needs severe adaptations to be used for any real-life Sinalgo project. 

# Usage:         Copy this script in the ROOT DIRECTORY OF YOUR SINALGO
#                installation and execute it.
# Requirements:  A working perl installation, e.g. download from http://www.perl.org/
# Hints:         If you are new to perl, a good starting point may be 
#                http://perldoc.perl.org

$numRounds = 1000;  # Number of rounds to perform for each simulation
$refreshRate = 100; # Refresh rate

$startTime = time; # Capture the total time
$SimCount = 0;     # Count # of simulations

# run the simulation for different node speeds (2, 5, 8)
for($speed=2; $speed<9; $speed+=3) {
  $speedVariance = $speed / 2;

  # and run with some different node densities (200, 300, 400, 500 nodes)
  for($numNodes=200; $numNodes<=500; $numNodes+=100) {
	$SimCount++;
	print "Simulation $SimCount - Speed mean=$speed var=$speedVariance, NumNodes = $numNodes\n";

	die "Terminated prematurely" unless
	  system("java -Xmx1000m -cp \"binaries/bin;binaries/jdom.jar\" sinalgo.Main " .
			 "-project sample1 " .             # choose the project
			 "-gen $numNodes sample1:S1Node Random RandomDirection " . # generate nodes
			 "-overwrite randomDirection/NodeSpeed/Mean=$speed " .  # Node speed
			 "randomDirection/NodeSpeed/Variance=$speedVariance " . # Node speed
			 "exitAfter=true exitAfter/Rounds=$numRounds " . # number of rounds to perform & stop
			 "exitOnTerminationInGUI=true " .  # Close GUI when hasTerminated() returns true
			 "AutoStart=true " .               # Automatically start communication protocol
			 "outputToConsole=false " .        # Create a framework log-file for each run
			 "extendedControl=false " .        # When in GUI mode, don't show the extended control
			 "-rounds $numRounds " .           # Number of rounds to start simulation
			 "-refreshRate $refreshRate"       # Don't draw GUI often
			) == 0;
  }
}
print "Time elapsed: " . (time - $startTime) . " seconds";
