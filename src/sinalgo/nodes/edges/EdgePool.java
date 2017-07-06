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
package sinalgo.nodes.edges;


import java.util.Hashtable;
import java.util.Stack;

import sinalgo.configuration.Configuration;

/**
 * This class stores unused Edges to recycle them when a new one is used. This is used to reduce the garbage collection load.
 */
public class EdgePool{
	
	private static Hashtable<String, Stack<Edge>> stacks = new Hashtable<String, Stack<Edge>>();
	private static Stack<Edge> lastStack = null;
	private static String lastStackTypeName = "";
	
	/**
	 * @return The number of freed edges, ready to be reused
	 */
	public static int getNumFreedEdges() {
		if(lastStack != null) {
			return lastStack.size();
		} else {
			return 0;
		}
	}
	
	/**
	 * Removes all edges stored for reuse 
	 */
	public static void clear() {
		for(Stack<Edge> s : stacks.values()) {
			s.clear();
		}
		stacks.clear();
		if(lastStack != null) {
			lastStack.clear();
		}			
		lastStack = null;
	}
	
	/**
	 * This method frees the given edge. This means that it adds it to the edgePool.
	 * 
	 * @param e The edge to be added to the Edge Pool
	 */
	public void add(Edge e) {
		String typename = e.getClass().getName();
		Stack<Edge> st = null;
		if(typename.equals(lastStackTypeName)) {
			st = lastStack;
		} else {
			st = stacks.get(typename);
		}
		if(st == null) {
			st = new Stack<Edge>();
			stacks.put(typename, st);
		}
		st.push(e);
	}
	
	/**
	 * This method returns a Edge from the edge pool.
	 * The type of the edge is defined through the config file.
	 * @return An edge of the type given as parameter, null if there is no edge to reuse.
	 */
	public Edge get(){
		if(lastStack == null || Configuration.hasEdgeTypeChanged()){
			lastStackTypeName = Configuration.getEdgeType();
			lastStack = stacks.get(lastStackTypeName);
			if(lastStack == null) {
				lastStack = new Stack<Edge>();
				stacks.put(Configuration.getEdgeType(), lastStack);
			}
		}
		if(lastStack.empty()){
			return null;
		}
		return lastStack.pop();
	}
}
