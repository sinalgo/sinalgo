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

import sinalgo.nodes.Node;

/**
 * The node collection is responsible to store the nodes in a convenient
 * way for the simulation, such that they can be accessed efficiently. 
 * 
 * For instance, the Geometric2DNodeColleciton stores the nodes based on their
 * position and on the maximal transmission radius a node may have, such that 
 * the NodeCollection can quickly return a list of potential neighbors of any
 * node.
 * 
 * Other implementations are available, e.g. for nodes in the 3 dimensional room.
 * 
 * Each node has a member nodeCollectionInfo, which can be freely used by the
 * NodeCollection. By convention, this field is only considered to be initialized
 * after the node has been added to the node collection by calling the addNode() method.
 */
public abstract class NodeCollectionInterface implements Iterable<Node> {
	
	/**
	 * This method returns an enumeration over all the nodes.
	 *
	 * @return An enumeration over all the nodes.
	 */
	public abstract Enumeration<Node> getNodeEnumeration();
	
	/**
	 * Returns a sorted enumeration over the nodes. Compared to the getNodeEnumeration method this method
	 * returns an enumeration over a sorted version of the nodes.
	 * 
	 * The boolean flag backToFront lets you choose in which order to traverse the nodes, e.g.
	 * starting with the nodes in the back (backToFront = true) or starting with the nodes
	 * in front (backToFront = false).
	 * 
	 * This flag has no effect if Configuration#draw3DGraphNodesInProperOrder is set to false
	 * or if the simulation is not running in 3D. In that case, the nodes are traversed
	 * in arbitrary order.  
	 * 
	 * @param backToFront set true to get the nodes in back first, false to get nodes in front first.
	 * @return An enumeration over the sorted version of the nodes collection. 
	 */
	public abstract Enumeration<Node> getSortedNodeEnumeration(boolean backToFront);
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public abstract Iterator<Node> iterator();
	
	/**
	 * For the given node n, retrive only the nodes which are possible neighbor candidates. This 
	 * is possible because of the rMax filed of the GeometricNodeCollection, which indicates the maximum
	 * distance between any two connected points.
	 * 
	 *  <br><b>Note:</b> The enumeration will also contain the provided node n.<br>
	 *  
	 * Subsequent calls to this method may return the same enumeration object, 
	 * initialized to the specific call. I.e. you may not call this method
	 * while an interator previously returned by this method is still in use.
	 * 
	 * @param n The node to get the neighbor candidates for.
	 * @return The Enumeration over the possible neighbors of node n.
	 */
	public abstract Enumeration<Node> getPossibleNeighborsEnumeration(Node n);
	
	/**
	 * Adds a node to this node collection. 
	 * 
	 * Note that this method is responsible to initialize the nodeCollectionInfo member
	 * of the node. Previous to adding the node to this nodeCollection, this filed may
	 * be null. By convention, after adding the node to the node collection, this field
	 * is considered to be set (if the node collection uses it). 
	 * <p>
	 * This method needs to set the filed holdInNodeCollection of the added node to true. 
	 *
	 * @param n The node to add to the collection.
	 */
	public void addNode(Node n) {
		_addNode(n);
		for(NodeCollectionListener l : listeners) {
			l.nodeAdded(n);
		}
	}
	

	/**
	 * The subclass implements this method to add a node.
	 * @param n
	 */
	protected abstract void _addNode(Node n);
			
	/**
	 * This method removes a node from the local datastructure.
	 * This method is primarily called by the framework. To remove a node from user
	 * code, you probably should call Runtime.removeNode(Node).
	 * <p>
	 * This method needs to set the filed holdInNodeCollection of the removed node to false. 
	 * @param n The node to remove.
	 */
	public void removeNode(Node n) {
		_removeNode(n);
		for(NodeCollectionListener l : listeners) {
			l.nodeRemoved(n);
		}
	}
	
	/**
	 * The subclass implements this method to remove a node.
	 * @param n
	 */
	protected abstract void _removeNode(Node n);

	/**
	 * This method is to update important information in the local datastructure. This method
	 * must be called by the node if it changed some information, that is important for the 
	 * datastructure. For example: The GeometricNodeCollection stores the nodes according to their
	 * position. So if a Node changes its position, it has to call this method afterwards to 
	 * give the collection the possibility to update.
	 *
	 * @param n The node which changed its sensitive information.
	 */
	public void updateNodeCollection(Node n) {
		_updateNodeCollection(n);
		for(NodeCollectionListener l : listeners) {
			l.nodeUpdated(n);
		}
	}
	
	protected abstract void _updateNodeCollection(Node n);
	
//	/**
//	 * This method can be called to find out if someone called checkNode and thus has
//	 * changed sensitive information. You can use it for example to find out, if some node
//	 * changed its position since the last call of this method. This means that the flag is
//	 * reset to false when calling this method. 
//	 *
//	 * @return A boolean to indicate whether there has been a change of sensitive information.
//	 */
//	public abstract boolean hasSensitiveInfoChanged();

	/**
	 * Returns a random node from the node collection. If there are no nodes in the system, it
	 * returns null.
	 *
	 * @return A random node from all nodes of the network. Returns null if there are no nodes
	 * available.
	 */
	public abstract Node getRandomNode();
	
	/**
	 * Returns the number of nodes stored in this collection.
	 * @return the number of nodes stored in this collection.
	 */
	public abstract int size();
	
	/**
	 * A listener that is notified whenever sensitive information of this
	 * node collection changes.   
	 */
	public interface NodeCollectionListener {

		/**
		 * Called when the node changes storage-sensitive data.
		 * @param n
		 */
		public void nodeUpdated(Node n);

		/**
		 * Called when a node is added to the framework. This method
		 * is also called for each node that is added through the 
		 * generate nodes dialog, e.g. when multiple nodes are generated.
		 * @param n
		 */
		public void nodeAdded(Node n);
		
		/**
		 * Called when a node is removed from the framework.
		 * This method is not called when the entire graph is cleared. In that 
		 * case, the entire node collection is replaced with a new instance.
		 * @param n
		 */
		public void nodeRemoved(Node n);
	}
	
	// the list of listeners must be static, s.t. when the node collection 
	// is replaced with a new collection, the listeners remain installed.
	// (Upon clearing the graph, the node collection object is replaced.)
	static Vector<NodeCollectionListener> listeners = new Vector<NodeCollectionListener>();
	
	/**
	 * Add a collection listener to this node collection.
	 * @param ncl
	 */
	public void addCollectionListener(NodeCollectionListener ncl) {
		if(!listeners.contains(ncl)) { // avoid duplicates
			listeners.add(ncl);
		}
	}
	
	/**
	 * Remove a collection listener from this node collection.
	 * @param ncl
	 */
	public void removeCollectionListener(NodeCollectionListener ncl) {
		listeners.remove(ncl);
	}
}
