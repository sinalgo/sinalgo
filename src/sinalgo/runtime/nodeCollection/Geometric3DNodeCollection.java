/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.runtime.nodeCollection;


import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;


/**
 * This 3D node collection implementation stores nodes placed in a 3 dimensional
 * field whose connectivity has a maximal range. I.e. nodes within connection
 * range have a known maximal distance between each other. This critical distance, 
 * call it rMax, is used by this node collection to teselate the space into 
 * cells of side length rMax. This allows to efficiently retrive a subset of potentail
 * neighbors of a node, given the position of the node by returning the matrix cells
 * adjacent to the cell containing the node.  
 */
public class Geometric3DNodeCollection extends NodeCollectionInterface {
	
	private int dimX = Configuration.dimX;
	private int dimY = Configuration.dimY;
	private int dimZ = Configuration.dimZ;
	
	private int numX, numY, numZ; // cardinality of matrix in each dimension

	// a hierarchical matrix to access the nodes based on their position
	private NodeListInterface[][][] list;

	// a flat list for fast iteration over all nodes
	private Vector<Node> flatList = new Vector<Node>();
	private boolean flatListChanged = false;
	
	private Node[] sortedNodeArray = new Node[1];
	private int sortedNodeArraySize = 0; // Number of non-null nodes in sortedNodeArray
	private DepthComparator myDepthComparator = null; 
	             
	// the maximal distance between any two connected nodes
	private double rMax = 0;
	
	// Flag indicating whether some nodes have changed the matrix cell
	//private boolean sensitiveInformationChanged = false;
	
	// The enumeration object used repeatedly to access the potential neighbors of a node
	Geometric3DNodeEnumeration enumeration = new Geometric3DNodeEnumeration();
	
	/**
	 * Default constructor. Creates and initializes the geometric node collection. 
	 */
	public Geometric3DNodeCollection() {
		// Immediately stop execution if rMax is not defined in the xml config file.  
		try {
			rMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
		} catch(CorruptConfigurationEntryException e) {
			Main.fatalError(e.getMessage());
		}
		if(rMax <= 0) {
			Main.fatalError("Geometric3DNodeCollection: The value of rMax from the config file entry " +
			                "<GeometricNodeCollection rMax=\"" + rMax + "\"/>" +
			                "is not valid. The value of rMax must be positive."
			                );
		}
		// determine cardinality of matrix in each dimension
		numX = (int) Math.ceil(dimX / rMax);
		numY = (int) Math.ceil(dimY / rMax);
		numZ = (int) Math.ceil(dimZ / rMax);
		// create and initialize the matrix
		list = new DLLNodeList[numX][numY][numZ];
		for(int i=0; i<numX; i++) {
			for(int j=0; j<numY; j++) {
				for(int k=0; k<numZ; k++) {
					list[i][j][k] = new DLLNodeList(true);
				}
			}
		}
	}
	
	/**
	 * Maps a component of a position-coordinate to the corresponding offset
	 * into the matrix.
	 */
	private int mapCoord(double c) {
		return (int) Math.floor(c / rMax);
	}
	
	
	private int lastVersionNumber = 0;
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#getSortedNodeEnumeration(java.util.Comparator)
	 */
	public Enumeration<Node> getSortedNodeEnumeration(boolean backToFront){
		if(!Configuration.draw3DGraphNodesInProperOrder) {
			return flatList.elements();
		}
		PositionTransformation t3d = Main.getRuntime().getTransformator();
		int actualVersionNumber = t3d.getVersionNumber();
		if((lastVersionNumber != actualVersionNumber)||(flatListChanged)){
			//the transformation has changed. Need to resort the array.
			lastVersionNumber = actualVersionNumber;
			flatListChanged = false;
			
			sortedNodeArray = flatList.toArray(sortedNodeArray);
			sortedNodeArraySize = flatList.size();
			if(sortedNodeArraySize > 1) {
				if(myDepthComparator == null) {
					myDepthComparator = new DepthComparator();
				}
				Arrays.sort(sortedNodeArray, 0, sortedNodeArraySize, myDepthComparator);
			}
		}
	
		return new ArrayEnumeration(backToFront);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#getNodeEnumeration()
	 */
	public Enumeration<Node> getNodeEnumeration() {
		return flatList.elements();
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#iterator()
	 */
	public Iterator<Node> iterator() {
		return new StateSensitiveIterator();
	}

	/**
	 * This is a wrapper class around an Iterator. It sets the flatListChanted field to true when someone 
	 * deletes a Node from the flatList by calling the remove method of the iterator.
	 */
	private class StateSensitiveIterator implements Iterator<Node>{

		Iterator<Node> iter;
		
		/**
		 * The constructor to create a new StateSensitiveIterator.
		 */
		private StateSensitiveIterator(){
			iter = flatList.iterator();
		}
		
		public boolean hasNext() {
			return iter.hasNext();
		}

		public Node next() {
			return iter.next();
		}

		public void remove() {
			iter.remove();
			flatListChanged = true;
		}
		
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#getPossibleNeighborsEnumeration(sinalgo.nodes.Node)
	 */
	public Enumeration<Node> getPossibleNeighborsEnumeration(Node n) {
		enumeration.resetForNode(n);
		return enumeration;
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#addNode(sinalgo.nodes.Node)
	 */
	public void _addNode(Node n) {
		n.holdInNodeCollection = true;
		
		Position pos = n.getPosition();
		int x = mapCoord(pos.xCoord);
		int y = mapCoord(pos.yCoord);
		int z = mapCoord(pos.zCoord);
		n.nodeCollectionInfo = new CubePos(x,y,z);
		
		list[x][y][z].addNode(n);
		flatList.add(n);
		flatListChanged = true;
		//sensitiveInformationChanged = true;
	}

	public void _removeNode(Node n) {
		n.holdInNodeCollection = false;
		CubePos pos = (CubePos) n.nodeCollectionInfo;
		if(!list[pos.x][pos.y][pos.z].removeNode(n)) {
			// the node was not located where it said! ERROR! 
			Main.fatalError("Geometric3DNodeCollection.removeNode(Node):\n" +
			                "A node is being removed, but it is not " +
			                "located in the matrix cell " + "in which it claims to be.");
		}
		flatList.remove(n);
		flatListChanged = true;
		n.nodeCollectionInfo = null;
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#updateNodeCollection(sinalgo.nodes.Node)
	 */
	public void _updateNodeCollection(Node n) {
		if(!n.holdInNodeCollection) {
			return; // the node is not yet hold by this node collection 
		}
		//sensitiveInformationChanged = true;
		// test whether the node has changed the cell of the matrix
		
		// the old position in the matrix
		CubePos oldPos = (CubePos) n.nodeCollectionInfo;
		// the new position in the matrix
		Position pos = n.getPosition();
		int x = mapCoord(pos.xCoord);
		int y = mapCoord(pos.yCoord);
		int z = mapCoord(pos.zCoord);
		if(oldPos.x != x || oldPos.y != y || oldPos.z != z) { 
			// the node needs to be stored in a different cell of the matrix
			// remove it from the old matrix cell...
			if(!list[oldPos.x][oldPos.y][oldPos.z].removeNode(n)) {
				Main.fatalError("Geometric3DNodeCollection.updateNodeCollection(Node):\nA node is being removed from the matrix, but it is not located in the matrix cell in which it claims to be.");
			}
			// ... and add it to the new matrix cell
			list[x][y][z].addNode(n);
			// update the matrix-cell info stored at the node 
			oldPos.x = x;
			oldPos.y = y;
			oldPos.z = z;
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#hasSensitiveInfoChanged()
	 */
//	public boolean hasSensitiveInfoChanged() {
//		boolean tmp = sensitiveInformationChanged;
//		sensitiveInformationChanged = false;
//		return tmp;
//	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#getRandomNode()
	 */
	public Node getRandomNode() {
		if(flatList.size() > 0){
			java.util.Random rand = Distribution.getRandom();
			int position = rand.nextInt(flatList.size());
			return flatList.elementAt(position);
		}
		else{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#numberOfNodes()
	 */
	public int size() {
		return flatList.size();
	}
	
	/**
	 * An enumeration that helps to iterate over all potential nodes
	 * given a certain node.
	 * 
	 * Note that you must call resetForNode(Node) prior to using
	 * any instance of this class.
	 */
	class Geometric3DNodeEnumeration implements Enumeration<Node>{
		int ox, oy, oz; // base position for the 3-dimensional iteration
		int dx = 0, dy = 0, dz = -1; // the offset from the base position 
		Iterator<Node> iterator;
		
		/**
		 * Prepares this enumeration for a given node n. This 
		 * method needs to be called prior to using the enumeration object.
		 * @param n The node for which the potential neighbors should
		 * be enumerated.
		 */
		public void resetForNode(Node n) {
			Position pos = n.getPosition();
			// The base position is located below the matrix cell where this node is contained, s.t. we can add {0|1|2} to each coordinate in all different permuations to obtain the 27 different fields. 
			ox = Math.min(numX - 1, mapCoord(pos.xCoord)) - 1;
			oy = Math.min(numY - 1, mapCoord(pos.yCoord)) - 1;
			oz = Math.min(numZ - 1, mapCoord(pos.zCoord)) - 1;
			dx = dy = 0; 
			dz = -1; // is incremented in call 'getNextValidMatrixCell'
			gotoNextValidMatrixCell(); // get the first iterator - there's at least one
		}
		
		/**
		 * Loops through ox, oy and oz to the next valid cell
		 * @return true if another valid matrix cell was found, otherwise false.
		 */
		private boolean gotoNextValidMatrixCell() {
			do {
				dz++;
				if(dz > 2) {
					dz = 0; dy++;
				}
				if(dy > 2) {
					dy = 0; dx++;
				}
				if(dx > 2) {
					return false; // we have visited all neighboring matrix cells
				}
			} while(ox + dx < 0 || oy + dy < 0 || oz + dz < 0 ||	
					ox + dx >= numX || oy + dy >= numY || oz + dz >= numZ);
			iterator = list[ox + dx][oy + dy][oz + dz].iterator(); // get new iterator
			return true;
		}
				
		/* (non-Javadoc)
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements() {
			if(iterator.hasNext()) {
				return true;
			} else {
				if(!gotoNextValidMatrixCell()) { // we have visited all neighboring matrix cells
					return false;
				}
				return hasMoreElements(); // tail recursive call
			}
		}

		public Node nextElement() {
			return iterator.next();
		}
	}
	
	//An enumeration over the array of sorted nodes. 
	private class ArrayEnumeration implements Enumeration<Node>{
		boolean backToFront;
		/**
		 * Constructs an arrayEnumeration with the given modal.
		 *
		 * @param backToFront Indicates whether the array has to be sorted forward or backward. Set to true to sort it 
		 * so that the element with the biggest distance to the viewer has is traversed first.
		 */
		private ArrayEnumeration(boolean backToFront) {
			this.backToFront = backToFront;
		}
		
		int currentIndex = 0;
		
		public boolean hasMoreElements() {
			return currentIndex < sortedNodeArraySize; 
		}

		public Node nextElement() {
			if(backToFront) {
				return sortedNodeArray[currentIndex++]; //implicit incrementation
			} else {
				return sortedNodeArray[sortedNodeArraySize - ++currentIndex]; //implicit incrementation BEFORE evaulation to have the offset one smaller
			}
		}		
	}
	
	/**
	 * Sorts the elements such that nodes in the back are drawn first
	 */
	private class DepthComparator implements Comparator<Node> {
		PositionTransformation pt= null;
		Transformation3D t3d = null; 
		
		/**
		 * Creates a new DepthComparator instance. Note that the DepthComparator only does something
		 * in 3 Dimensions.
		 */
		private DepthComparator() {
			pt = Main.getRuntime().getTransformator();
			if(pt instanceof Transformation3D) {
				t3d = (Transformation3D) pt;
			}
		}

		public int compare(Node n1, Node n2)  {
			if(t3d != null) {
				double zN1 = t3d.translateToGUIPositionAndGetZOffset(n1.getPosition());
				double zN2 = t3d.translateToGUIPositionAndGetZOffset(n2.getPosition());
				return (int)(zN1 - zN2);
			} else {
				//The deptcompator is not used in 2 dimensions.
				return 0;
			}
		}
	}
}
