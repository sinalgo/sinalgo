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


import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;


/**
 * The class to save the nodes depending on their position. They are stored in a grid.
 * Like this the possible neighbors can fast and easy be discovered.
 */
public class Geometric2DNodeCollection extends NodeCollectionInterface {

	//the dimension of the array. This means how many squares with sidelength rMax are needed to 
	//cover the whole playground..
	private int xDim;
	private int yDim;
	
	//private boolean sensitiveInformationChanged = false;
	
	//the size of the playground
	private int dimX = Configuration.dimX;
	private int dimY = Configuration.dimY;
	//
	private double rMax;
	
	//the local nodes to be able to simply traverse the nodes. A Vector is ok... well...
	private Vector<Node> localNodes = new Vector<Node>();
	// TODO: removing nodes from this list is expensive and costs O(n). The main 
	// difficulty to replace this datastructure with a better one is the 'getRandomNode' 
	// method, which returns a random entry in this list. For a vector, this is easy 
	// to achieve. 
	// Proposed solution: Implement a modified TreeSet, which is a red-black tree, and add
	// to each entry of the tree a field that indicates the number of entries stored in 
	// its descendants. This would permit to access entries from the tree using an array-style
	// method (return the i-th element) in O(log n) time. add, delete, contains also take O(log n)
	// time. Furthermore, iteration is also very cheap with a stack of at most log(n) size.
	// Use the same datastructure for the 3D implementation. 
	
	
	//the core datastructure able to store the nodes depending on the position.
	private NodeListInterface[][] lists = null;
	
	//This instance of the Squarepos is used to return the squarePosition of a node. It seams wierd to declare it
	//here but this is necessary to get rid of the allocation and garbage collection of the returned squarePos
	private SquarePos oneSquarePos = new SquarePos(0, 0);
	
	//The instance of the GeometricNodeEnumeration. This is the instance that is either created or reset by the 
	//getPossibleNeighborsEnumeration method.
	private GeometricNodeEnumeration geometricNodeEnumeration = null;
	
	/**
	 * The constructor for the GeometricNodeCollection class.
	 */
	public Geometric2DNodeCollection(){
		// Immediately stop execution if rMax is not defined in the xml config file.  
		try {
			rMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
		} catch(CorruptConfigurationEntryException e) {
			Main.fatalError(e.getMessage());
		}
		
		double ratio = dimX / rMax;
		xDim = (int)Math.ceil(ratio);

		ratio = dimY/rMax;
		yDim = (int)Math.ceil(ratio);
		
		lists = new DLLNodeList[xDim][yDim];
		for(int i = 0; i < xDim; i++){
			for(int j = 0; j < yDim; j++){
				lists[i][j] = new DLLNodeList(true);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#getPossibleNeighborsEnumeration(nodes.Node)
	 */
	public Enumeration<Node> getPossibleNeighborsEnumeration(Node n){
		if(geometricNodeEnumeration == null){
			geometricNodeEnumeration = new GeometricNodeEnumeration(n);
		}
		else{
			geometricNodeEnumeration.resetForNode(n);
		}
		return geometricNodeEnumeration;
	}
	
	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#addNode(nodes.Node)
	 */
	public void _addNode(Node n) {
		n.holdInNodeCollection = true;

		//sensitiveInformationChanged = true;
		
		SquarePos location = getPosOfNode(n);
		//the node stores its position in the datastructure itself. This is ugly, but it makes
		//searching faster
		n.nodeCollectionInfo = new SquarePos(location.x, location.y);
		
		lists[location.x][location.y].addNode(n);
		
		localNodes.add(n);
	}

	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#checkNode(nodes.Node)
	 */
	public void _updateNodeCollection(Node n) {
		if(!n.holdInNodeCollection) {
			return; // the node is not yet hold by this node collection 
		}
		//sensitiveInformationChanged = true;
		
		SquarePos newPosition = getPosOfNode(n);
		SquarePos oldPosition = (SquarePos) n.nodeCollectionInfo;
		if((oldPosition.x != newPosition.x)||
				oldPosition.y != newPosition.y){
			
			//do not call this.remove. Already calculated the new position and thus we can 
			//save time to directly call the remove on the list and on the localNodes.
			NodeListInterface list = lists[oldPosition.x][oldPosition.y];
			list.removeNode(n);
	
			oldPosition.x = newPosition.x;
			oldPosition.y = newPosition.y;
			
			lists[newPosition.x][newPosition.y].addNode(n);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#removeNode(sinalgo.nodes.Node)
	 */
	public void _removeNode(Node n){
		n.holdInNodeCollection = false;
		SquarePos pos = getPosOfNode(n);
		NodeListInterface nList = lists[pos.x][pos.y];
		if(!nList.removeNode(n)) {
			// the node was not located where it said! ERROR! 
			Main.fatalError("Geometric2DNodeCollection.removeNode(Node):\n" +
			                "A node is being removed, but it is not" +
			                "located in the matrix cell " + "in which it claims to be.");
		}
		localNodes.remove(n);
	}
	
	private SquarePos getPosOfNode(Node n){
		Position p = n.getPosition();
		oneSquarePos.x = (int)Math.floor(p.xCoord/rMax);
		oneSquarePos.y = (int)Math.floor(p.yCoord/rMax);
		return oneSquarePos;
	}
	
	class GeometricNodeEnumeration implements Enumeration<Node>{

		private GeometricNodeListEnumeration sNLE = null;
		private Iterator<Node> nI = null;
		
		/**
		 * The constructor for the GeometricNodeEnumeration class. This Enumeration is used
		 * to find out all the possible neighbors and pass them to the connectivityModel.
		 *
		 * @param n The node to get the neighbors for.
		 */
		public GeometricNodeEnumeration(Node n){
			if(sNLE == null){
				sNLE = new GeometricNodeListEnumeration(n);
			}
			else{
				sNLE.resetForNode(n);
			}
			
			if(sNLE.hasMoreElements()){
				nI = sNLE.nextElement().iterator();
			}
		}
		
		/**
		 * This method resets the Enumeration to the initial state without allocating a 
		 * new Enumeration instance.
		 * 
		 * @param n The node to reset the Enumeration for.
		 * @see GeometricNodeEnumeration#GeometricNodeEnumeration(Node)
		 */
		public void resetForNode(Node n){
			if(sNLE == null){
				sNLE = new GeometricNodeListEnumeration(n);
			}
			else{
				sNLE.resetForNode(n);
			}
			
			if(sNLE.hasMoreElements()){
				nI = sNLE.nextElement().iterator();
			}
		}
		
		public boolean hasMoreElements() {
			if(nI.hasNext()){
				return true;
			}
			else{
				while(sNLE.hasMoreElements()){
					nI = sNLE.nextElement().iterator();
					if(nI.hasNext()){
						return true;
					}
				}
			}
			return false;
		}

		public Node nextElement() {
			return nI.next();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.nodeCollection.NodeCollectionInterface#getSortedNodeEnumeration(boolean)
	 */
	public Enumeration<Node> getSortedNodeEnumeration(boolean backToFront) {
		//in 2D returns the same as getNodeEnumeration()
		return localNodes.elements();
	}
	
	
	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#getNodeEnumeration()
	 */
	public Enumeration<Node> getNodeEnumeration() {
		return localNodes.elements();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Node> iterator() {
		return localNodes.iterator();
	}
	
	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#hasSensitiveInfoChanged()
	 */
//	public boolean hasSensitiveInfoChanged() {
//		boolean rval = this.sensitiveInformationChanged;
//		this.sensitiveInformationChanged = false;
//		return rval;
//	}
	
	
	/**
	 * Enumeration to traverse the Lists of nodes to find possible neighbors.
	 */
	class GeometricNodeListEnumeration implements Enumeration<NodeListInterface>{
	
		//this is the collection of all the squares where neighbors may be.
		private SquarePositionCollection squares = null;
		//the location of the node this enumeration is for
		private SquarePos location = null;
		//the enumeration over the 
		private Enumeration<SquarePos> listEnumeration = null;
		//the mask where the possible neighbors nay be. Initially all neighboring squares are possible. If the node is in a 
		//"border"-square some 1's are changed to 0's.
		private int[][] mask = {{1,1,1},{1,1,1},{1,1,1}};
		
		/**
		 * The constructor for the GeometricNodeListEnumeration class. This class is used to 
		 * traverse all the nodelists saved in the datastructure. It returns a enumeration
		 * over NodeLists which are used by the GeometricNodeEnumeration.
		 *
		 * @param n The Node to get all the NodeLists of potential neighbors.
		 */
		public GeometricNodeListEnumeration(Node n){
			squares = new SquarePositionCollection();
			//calculate the position in the datastructure of the node
			location = getPosOfNode(n);
			
			//fill the vector with the addresses of the neighborhood squares
			if(location.x == 0){ mask[0][0] = 0; mask[0][1] = 0; mask[0][2] = 0; }
			if(location.x+1 == xDim){ mask[2][0] = 0; mask[2][1] = 0; mask[2][2] = 0; }
			if(location.y == 0){ mask[0][0] = 0; mask[1][0] = 0; mask[2][0] = 0; }
			if(location.y+1 == yDim){ mask[0][2] = 0; mask[1][2] = 0; mask[2][2] = 0; }

			for(int i = 0; i < 3; i++){
				for(int j = 0; j < 3; j++){
					if(mask[j][i] != 0){
						squares.add(location.x+(j-1), location.y+(i-1));
					}
				}
			}
			
			//initialize the enumeration over the vector
			listEnumeration = squares.elements();
		}
		
		/**
		 * This method resets the nodeListIterator. Do not allocate a new Enumeration in every step but reset the current one. This
		 * reduces the number of allocated objects and thus the amount of garbage the garbage collector has to collect.
		 * 
		 * @param n The node to create reset the enumeration for.
		 */
		public void resetForNode(Node n){
			
			squares.clear();
			
			for(int i = 0; i < 3; i++){
				for(int j = 0; j < 3; j++){
					mask[j][i] = 1;
				}
			}
			
			//calculate the position in the datastructure of the node
			location = getPosOfNode(n);
			
			//fill the vector with the addresses of the neighborhood squares
			if(location.x == 0){ mask[0][0] = 0; mask[0][1] = 0; mask[0][2] = 0; }
			if(location.x+1 == xDim){ mask[2][0] = 0; mask[2][1] = 0; mask[2][2] = 0; }
			if(location.y == 0){ mask[0][0] = 0; mask[1][0] = 0; mask[2][0] = 0; }
			if(location.y+1 == yDim){ mask[0][2] = 0; mask[1][2] = 0; mask[2][2] = 0; }

			for(int i = 0; i < 3; i++){
				for(int j = 0; j < 3; j++){
					if(mask[j][i] != 0){
						squares.add(location.x+(j-1), location.y+(i-1));
					}
				}
			}
			//initialize the enumeration over the vector
			listEnumeration = squares.elements();
		}

		public boolean hasMoreElements() {
			return listEnumeration.hasMoreElements();
		}

		public NodeListInterface nextElement() {
			SquarePos sp = listEnumeration.nextElement();
			return lists[sp.x][sp.y];
		}
	}

	/* (non-Javadoc)
	 * @see nodeCollection.NodeCollection#getRandomNode()
	 */
	public Node getRandomNode() {
		if(localNodes.size() > 0){
			java.util.Random rand = Distribution.getRandom();
			int position = rand.nextInt(localNodes.size());
			return localNodes.elementAt(position);
		}
		else{
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see runtime.nodeCollection.NodeCollectionInterface#numberOfNodes()
	 */
	public int size() {
		return localNodes.size();
	}
}
