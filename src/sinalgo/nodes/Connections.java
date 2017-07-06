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
package sinalgo.nodes;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.edges.Edge;
import sinalgo.tools.storage.ReusableListIterator;


/**
 * Interface of all Edge collections. If you want to replace the concrete implementation
 * please be sure to implement the methods according to their specifications in the comments.
 * Some of the methods require side effects.
 */
public interface Connections  extends Iterable<Edge> {
	
	/**
	 * This Method adds an edge form startNode to endNode to the collection. It also has an boolean flag 
	 * to indicate whether the edge has been validated in this round or not. In the normal case, this 
	 * flag is set to true, because the edge is validated on adding. But especially when 
	 * the user inserts an edge in the gui by hand, that does not agree with the selected
	 * connectivity model, it has to be removed in the first round after insertion. Therefore 
	 * the edge has to be inserted with valid set to false. Upon validating the edge in the next
	 * round, the flag gets set to true by the connectivity model. All edges with a valid flag set
	 * on false at the end of the round get deleted. <br>
	 * <br>
	 * true == for this step, the edge corresponds to the connectivity model<br>
	 * false == in the next step, the edge will be removed. Used for user-added (by hand) edges, which are not added due to the connectivity model.<br>
	 * <br>
	 * If the edge is already contained in the collection the method sets the valid flag to
	 * true.
	 * 
	 * @param startNode The start node of the Edge to insert.
	 * @param endNode The end Node of the Edge to insert.
	 * @param valid A boolean indicating, if the Edge was already valid in this round.
	 * @return A Boolean indicating whether the edge was already in the collection.
	 * @throws WrongConfigurationException Thrown when something with the configuration went wrong. Mostly
	 * this would be a misconfiguration like an Edge, that does not conform to the nodes used. 
	 */
	public abstract boolean add(Node startNode, Node endNode, boolean valid) throws WrongConfigurationException;
	
	/**
	 * This method removes an edge with given start and end node from the collection.
	 * <p>
	 * Does not free() the returned edge! 
	 * 
	 * @param from The start node of the edge to remove.
	 * @param to The end node of the edge to remove.
	 * @return The removed edge, null if no edge was removed / found.
	 */
	public abstract Edge remove(Node from, Node to);
	
	
	/**
	 * Removes all edges, invalidates all messages sent over these edges, 
	 * and frees the edges hold in this collection.
	 */
	public abstract void removeAndFreeAllEdges();
	
	/**
	 * This method returns whether there is an edge from the start node to the end node. It does not 
	 * search for the same instance but for the same start and end node.
	 * 
	 * @param startNode The startNode of the Edge to check.
	 * @param endNode The endNode of the Edge to check.
	 * @return Returns whether there is an edge in the collection from the start node to the end
	 * node.
	 */
	public abstract boolean contains(Node startNode, Node endNode);
	
	/**
	 * Removes all edges whose valid flag is set to false. 
	 * For all remaining edges (the edges whose valid flag was set
	 * to true upon calling this method), set the valid flag to false.
	 *
	 * Thus, after calling this method, the valid flag of all edges in 
	 * this collection is set to false.
	 * @return True if at least one edge was removed, otherwise false.  
	 */
	public abstract boolean removeInvalidLinks();
	
	/**
	 * This method returns the number of elements in the collection.
	 *
	 * @return Returns the number of elements in the collection.
	 */
	public abstract int size();
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public abstract ReusableListIterator<Edge> iterator(); 
	
	/**
	 * Mixes this list randomly. This is a rather expensive functionality
	 * and should be used with care. 
	 */
	public abstract void randomPermutation();
	
	/**
	 * ONLY USE THIS METHOD IF YOU REALLY KNOW WHAT YOU DO.
	 * 
	 * Appends an edge to the connections. Note that this is not the method to be used to add a connection to the 
	 * connections. Use the add(Node startNode, Node endNode, boolean valid) to add an edge properly.
	 * 
	 * @param e The edge to append to the edge
	 */
	public void hackyAppend(Edge e);
}
