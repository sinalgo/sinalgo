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
package sinalgo.models;


import java.util.Enumeration;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.runtime.Runtime;


/**
 * A helper class that helps you implement the connectivity model. This class iterates over all 
 * possible neighbors of a given node and asks you for each node pair whether the two nodes are 
 * connected.  
 * <p>
 * A class that is used to update the Connections of a Node. This is a dummy implementation. 
 * It can be overridden by specific Connectivity Models like the UDG or others. 
 * <br>
 * The ConnectivityModelInterface requires only the method <code>updateConnectsions(Node n)</code>. 
 * This implementation scans over all neighbors of n and executes the method <code>isConnected(Node n1, Node n2</code>
 * for each node-pair. Therefore, we suggest that you overwrite the isConnected method in your subclasses - if 
 * you prefer this approach. 
 */
public abstract class ConnectivityModelHelper extends ConnectivityModel {
	
	/* (non-Javadoc)
	 * @see connectivityModels.ConnectivityModelInterface#updateConnections(nodes.Node)
	 */
	public boolean updateConnections(Node n) throws WrongConfigurationException{
		boolean edgeAdded = false;
		
		// For the given node n, retrieve only the nodes which are possible neighbor candidates. This 
		// is possible because of the rMax filed of the GeometricNodeCollection, which indicates the maximum
		// distance between any two connected points.
		Enumeration<Node> pNE = Runtime.nodes.getPossibleNeighborsEnumeration(n);
		while( pNE.hasMoreElements() ){
			Node possibleNeighbor = pNE.nextElement();
			if(n.ID != possibleNeighbor.ID){
				// if the possible neighbor is connected with the the node: add the connection to the outgoing connection of n 
				if(isConnected(n, possibleNeighbor)){
					// add it to the outgoing Edges of n. The EdgeCollection itself checks, if the Edge is already contained
					
					edgeAdded = !n.outgoingConnections.add(n, possibleNeighbor, true) || edgeAdded; // note: don't write it the other way round, otherwise, the edge is not added if edgeAdded is true.
				} 
			}
		}
		// loop over all edges again and remove edges that have not been marked 'valid' in this round
		boolean dyingLinks = n.outgoingConnections.removeInvalidLinks(); 

		return edgeAdded || dyingLinks; // return whether an edge has been added or removed.
	}	

	/**
	 * Function to find out, if two nodes are connected when evaluating the current Connectivity Model.
	 * The connectivity is always checked only in one direction. This Function only checks, if there 
	 * should be a connection from node 'from' to node 'to'.
	 * This function is normally overwritten by the concrete implementation of a Mobility Model.
	 * 
	 * @param from The origin of a connection you want to check.
	 * @param to The Target of a connection you want to check.
	 * @return If the two Nodes are connected in the specified direction.
	 */
	protected abstract boolean isConnected(Node from, Node to);
}
