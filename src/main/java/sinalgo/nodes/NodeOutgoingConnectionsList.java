/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.nodes;

import sinalgo.exception.WrongConfigurationException;
import sinalgo.nodes.edges.Edge;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.ReusableListIterator;

/**
 * A list that holds the links to all neighbors of a given node.
 * <p>
 * This implementation is designed to be efficient for insertion and deletion of
 * edges.
 */
public class NodeOutgoingConnectionsList extends DoublyLinkedList<Edge> implements Connections {

    private ReusableListIterator<Edge> edgeIterator = this.iterator();

    /**
     * The constructor for the DLLConnections-class.
     *
     * @param keepFinger If set to true, entries keep their finger for later reuse (in this
     *                   or a different list) when they are removed from this list. When
     *                   set to false, the finger is removed.
     */
    public NodeOutgoingConnectionsList(boolean keepFinger) {
        super(keepFinger);
    }

    @Override
    public boolean add(Node startNode, Node endNode, boolean valid) throws WrongConfigurationException {
        // upon visiting or adding: set the value to true
        // Each entry is allowed only once in the collection
        if (!this.containsAndSetVisited(startNode, endNode, valid)) {
            Edge e = Edge.fabricateEdge(startNode, endNode);
            e.setValid(valid);
            this.append(e);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Edge remove(Node from, Node to) {
        // remove the edge from the EdgeCollection
        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge edge = this.edgeIterator.next();
            if ((from.getID() == edge.getStartNode().getID()) && (to.getID() == edge.getEndNode().getID())) {
                this.edgeIterator.remove();
                edge.removeEdgeFromGraph(); // does not free the edge
                return edge;
            }
        }
        return null;
    }

    @Override
    public void removeAndFreeAllEdges() {
        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge edge = this.edgeIterator.next();
            this.edgeIterator.remove();
            edge.removeEdgeFromGraph(); // called after the edge is removed from the outgoingConnectionList
            edge.free();
        }
    }

    @Override
    public boolean contains(Node startNode, Node endNode) {
        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge e = this.edgeIterator.next();
            if ((e.getStartNode().getID() == startNode.getID()) && (e.getEndNode().getID() == endNode.getID())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeInvalidLinks() {
        // go over all the links and remove the ones set to false (set all the values to
        // false)

        // a boolean to indicate, if something has changed
        boolean rval = false;

        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge edge = this.edgeIterator.next();
            if (!edge.isValid()) {
                this.edgeIterator.remove(); // remove the edge from the list of outgoing connections from this node
                edge.removeEdgeFromGraph();
                edge.free(); // return this edge to the edge factory s.t. it can be reused
                rval = true;
            } else {
                // sets the valid flag of each 'surviving' edge to false, such that in the next
                // round, this edge
                // needs to be confirmed again by the connectivity model
                edge.setValid(false);
            }
        }
        return rval;
    }

    @Override
    public ReusableListIterator<Edge> iterator() {
        return super.iterator();
    }

    @Override
    public int size() {
        return super.size();
    }

    /**
     * A slightly different method form the contains method. This method not only
     * returns, whether the edge is in the current vector, but also sets the valid
     * flag of the edge, if it is in the vector. This seems to be an undesired and
     * unpredictable side effect, but it prevents the user to traverse the vector
     * twice for checking for existence and then once again traverse it to set the
     * valid flag.
     *
     * @param edge  The Edge to search for in the vector.
     * @param valid The value, the valid flag of the edge has to be set, if it is
     *              contained in the vector.
     * @return If the specified edge is in the vector.
     */
    protected boolean containsAndSetVisited(Edge edge, boolean valid) {
        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge e = this.edgeIterator.next();
            if ((e.getStartNode().getID() == edge.getStartNode().getID()) && (e.getEndNode().getID() == edge.getEndNode().getID())) {
                e.setValid(valid);
                return true;
            }
        }
        return false;
    }

    /**
     * A slightly different method form the contains method. This method not only
     * returns, whether the edge is in the current vector, but also sets the valid
     * flag of the edge, if it is in the vector. This seems to be an undesired and
     * unpredictable side effect, but it prevents the user to traverse the vector
     * twice for checking for existence and then once again traverse it to set the
     * valid flag.
     *
     * @param startNode The startNode of the edge to search for.
     * @param endNode   The endNode of the edge to search for.
     * @param valid     The value, the valid flag of the edge has to be set, if it is
     *                  contained in the vector.
     * @return If the specified edge is in the vector.
     */
    protected boolean containsAndSetVisited(Node startNode, Node endNode, boolean valid) {
        this.edgeIterator.reset();
        while (this.edgeIterator.hasNext()) {
            Edge e = this.edgeIterator.next();
            if ((e.getStartNode().getID() == startNode.getID()) && (e.getEndNode().getID() == endNode.getID())) {
                e.setValid(valid);
                return true;
            }
        }
        return false;
    }

    @Override
    public void randomPermutation() {
        // copy this list into a temporary list
        NodeOutgoingConnectionsList tmp = new NodeOutgoingConnectionsList(false);
        while (!this.isEmpty()) {
            tmp.push(this.pop());
        }
        java.util.Random rand = sinalgo.tools.statistics.Distribution.getRandom();
        while (!tmp.isEmpty()) {
            int offset = rand.nextInt(tmp.size()); // [0..size-1]
            this.append(tmp.remove(offset));
        }
    }

    @Override
    public void hackyAppend(Edge e) {
        this.append(e);
    }
}
