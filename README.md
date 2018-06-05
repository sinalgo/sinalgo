<img src="icon/sinalgo_512.png" width="100" height="100" align="right"/> 

# Sinalgo - Simulator for Network Algorithms

## Welcome to Sinalgo

Sinalgo is a simulation framework for testing and validating network algorithms. 
Unlike most other network simulators, which spend most time simulating the different 
layers of the network stack, Sinalgo focuses on the verification of network algorithms, 
and abstracts from the underlying layers: It offers a message passing view of the network, 
which captures well the view of actual network devices. Sinalgo was designed, but is not 
limited to simulate wireless networks.

The key to successful development of network algorithms is a comprehensive test suite. Thanks to the
fast algorithm prototyping in JAVA, Sinalgo offers itself as a first test environment, prior to deploy the
algorithm to the hardware. Prototyping in JAVA instead of the hardware specific language is not only
much faster and easier, but also simplifies debugging. Sinalgo offers a broad set of network conditions,
under which you may test your algorithms. In addition, Sinalgo may be used as a stand­alone
application to obtain simulation results in network algorithms research.

Sinalgo's view of network devices is close to the view of real hardware devices (e.g. in TinyOS): A
node may send a message to a specific neighbor or all its neighbors, react to received messages, set
timers to schedule actions in the future, and much more.

Some of the key features of Sinalgo:
* Quick prototyping of your network algorithms in JAVA
* Straight forward extensibility to cover nearly any simulation scenario
* Many built-in, but still adjustable plug­ins
* High performance - run simulations with 100000s of nodes in acceptable time
* Support for 2D and 3D
* Asynchronous and synchronous simulation
* Customizable visualization of the network graph
* Platform independent - the project is written in Java
* Sinalgo is for free, published under a BSD license

To guarantee easy extensibility, Sinalgo offers a set of extension points, the so called models. The
following list gives an overview of the available models, to each of which you may add your own
extension. To facilitate your life, Sinalgo ships with a set of frequently used models.

* The mobility model describes how the nodes change their position over time. Examples are
random waypoint, random walk, random direction, and many others.
* The connectivity model defines when two nodes are in communication range. The best known
examples are the unit disk graph (UDG) and the quasi­UDG (QUDG).
* The distribution model is responsible to initially place the network nodes in the simulation area.
E.g. place the nodes randomly, evenly distributed on a line or grid or according to a stationary
regime of a mobility model.
* Use the interference model to define whether simultaneous message transmissions may
interfere.
* The reliability model is a simplified form of the interference model and lets you define for every
message whether it should arrive or not. E.g. drop one percent of all messages.
* Last but not least, the transmission model lets you define how long a message takes until it
arrives at its destination.

DISCLAIMER:
Sinalgo was originally developed by the [DCG (Distributed Computing Group)](https://disco.ethz.ch/) at [ETHzürich](http://www.ethz.ch/).

## Installation and usage

Sinalgo has been ported to use the Gradle build system beginning with version 0.77. It uses a Gradle Wrapper so 
users don't have to download the specific Gradle version that's compatible with Sinalgo. Running and building 
Sinalgo is as simple as running:

```
./gradlew run 
```

If you need to pass command line arguments, you can use the following syntax:

```
./gradlew run -PappArgs="['arg1', 'arg2', 'arg3']"
```

And so on. As an example, the following command will run the sample2 project straight from the command line, 
skipping the project selector window.

```
./gradlew run -PappArgs="['-project', 'sample2']"
```

When using Windows, replace ```./gradlew``` with ```gradlew.bat```.

That will downloaded whichever Gradle version Sinalgo needs, build the application and run it. That includes 
all projects.

Gradle includes a ton of functionality. Some plugins being used here provide some nice resources not provided
by Gradle by default.

Right now, the following commands might be useful:

* ```./gradlew build``` will build a zip for distribution plus a jar with every dependency needed to run Sinalgo.

* ```./gradlew javadoc``` will generate a set of HTML documents containing documentation on Sinalgo's classes.
This also includes projects.

## Tutorial

Sinalgo's original tutorial has been reconstructed [in the GitHub Pages for this repository](https://andrebrait.github.io/sinalgo). 
While this fork has departed quite a bit from the original,
most of the things in the tutorial are still valid as for building simulations and the general 
usage of Sinalgo.

The source code for these pages can be found in the ```gh-pages``` branch under this repository.
