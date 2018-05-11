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
package sinalgo.runtime.nodeCollection;

import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * The class to save the nodes depending on their position. They are stored in a
 * grid. Like this the possible neighbors can fast and easy be discovered.
 */
public class Geometric2DNodeCollection extends AbstractNodeCollection {

    // the dimension of the array. This means how many squares with sidelength rMax
    // are needed to
    // cover the whole playground..
    private int xDim, yDim;

    // private boolean sensitiveInformationChanged = false;

    //
    private double rMax;

    // the local nodes to be able to simply traverse the nodes. A Vector is ok...
    // well...
    private Vector<Node> localNodes = new Vector<>();
    // TODO: removing nodes from this list is expensive and costs O(n). The main
    // difficulty to replace this datastructure with a better one is the
    // 'getRandomNode'
    // method, which returns a random entry in this list. For a vector, this is easy
    // to achieve.
    // Proposed solution: Implement a modified TreeSet, which is a red-black tree,
    // and add
    // to each entry of the tree a field that indicates the number of entries stored
    // in
    // its descendants. This would permit to access entries from the tree using an
    // array-style
    // method (return the i-th element) in O(log n) time. add, delete, contains also
    // take O(log n)
    // time. Furthermore, iteration is also very cheap with a stack of at most
    // log(n) size.
    // Use the same datastructure for the 3D implementation.

    // the core datastructure able to store the nodes depending on the position.
    private NodeListInterface[][] lists;

    // This instance of the Squarepos is used to return the squarePosition of a
    // node. It seams wierd to declare it
    // here but this is necessary to get rid of the allocation and garbage
    // collection of the returned squarePos
    private SquarePos oneSquarePos = new SquarePos(0, 0);

    // The instance of the GeometricNodeEnumeration. This is the instance that is
    // either created or reset by the
    // getPossibleNeighborsEnumeration method.
    private GeometricNodeEnumeration geometricNodeEnumeration;

    /**
     * The constructor for the GeometricNodeCollection class.
     */
    public Geometric2DNodeCollection() {
        // Immediately stop execution if rMax is not defined in the xml config file.
        try {
            this.rMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }

        // the size of the playground
        this.xDim = (int) Math.ceil(Configuration.getDimX() / this.rMax);
        this.yDim = (int) Math.ceil(Configuration.getDimY() / this.rMax);

        this.lists = new DLLNodeList[this.xDim][this.yDim];
        for (int i = 0; i < this.xDim; i++) {
            for (int j = 0; j < this.yDim; j++) {
                this.lists[i][j] = new DLLNodeList(true);
            }
        }
    }

    @Override
    public Enumeration<Node> getPossibleNeighborsEnumeration(Node n) {
        if (this.geometricNodeEnumeration == null) {
            this.geometricNodeEnumeration = new GeometricNodeEnumeration(n);
        } else {
            this.geometricNodeEnumeration.resetForNode(n);
        }
        return this.geometricNodeEnumeration;
    }

    @Override
    protected void _addNode(Node n) {
        n.setHoldInNodeCollection(true);

        // sensitiveInformationChanged = true;

        SquarePos location = this.getPosOfNode(n);
        // the node stores its position in the datastructure itself. This is ugly, but
        // it makes
        // searching faster
        n.setNodeCollectionInfo(new SquarePos(location.getX(), location.getY()));

        this.lists[location.getX()][location.getY()].addNode(n);

        this.localNodes.add(n);
    }

    @Override
    protected void _updateNodeCollection(Node n) {
        if (!n.isHoldInNodeCollection()) {
            return; // the node is not yet hold by this node collection
        }
        // sensitiveInformationChanged = true;

        SquarePos newPosition = this.getPosOfNode(n);
        SquarePos oldPosition = (SquarePos) n.getNodeCollectionInfo();
        if ((oldPosition.getX() != newPosition.getX()) || oldPosition.getY() != newPosition.getY()) {

            // do not call this.remove. Already calculated the new position and thus we can
            // save time to directly call the remove on the list and on the localNodes.
            NodeListInterface list = this.lists[oldPosition.getX()][oldPosition.getY()];
            list.removeNode(n);

            oldPosition.setX(newPosition.getX());
            oldPosition.setY(newPosition.getY());

            this.lists[newPosition.getX()][newPosition.getY()].addNode(n);
        }

    }

    @Override
    protected void _removeNode(Node n) {
        n.setHoldInNodeCollection(false);
        SquarePos pos = this.getPosOfNode(n);
        NodeListInterface nList = this.lists[pos.getX()][pos.getY()];
        if (!nList.removeNode(n)) {
            // the node was not located where it said! ERROR!
            throw new SinalgoFatalException("Geometric2DNodeCollection.removeNode(Node):\n" + "A node is being removed, but it is not"
                    + "located in the matrix cell " + "in which it claims to be.");
        }
        this.localNodes.remove(n);
    }

    private SquarePos getPosOfNode(Node n) {
        Position p = n.getPosition();
        this.oneSquarePos.setX((int) Math.floor(p.getXCoord() / this.rMax));
        this.oneSquarePos.setY((int) Math.floor(p.getYCoord() / this.rMax));
        return this.oneSquarePos;
    }

    class GeometricNodeEnumeration implements Enumeration<Node> {

        private GeometricNodeListEnumeration sNLE;
        private Iterator<Node> nI;

        /**
         * The constructor for the GeometricNodeEnumeration class. This Enumeration is
         * used to find out all the possible neighbors and pass them to the
         * connectivityModel.
         *
         * @param n The node to get the neighbors for.
         */
        public GeometricNodeEnumeration(Node n) {
            if (this.sNLE == null) {
                this.sNLE = new GeometricNodeListEnumeration(n);
            } else {
                this.sNLE.resetForNode(n);
            }

            if (this.sNLE.hasMoreElements()) {
                this.nI = this.sNLE.nextElement().iterator();
            }
        }

        /**
         * This method resets the Enumeration to the initial state without allocating a
         * new Enumeration instance.
         *
         * @param n The node to reset the Enumeration for.
         * @see GeometricNodeEnumeration#GeometricNodeEnumeration(Node)
         */
        public void resetForNode(Node n) {
            if (this.sNLE == null) {
                this.sNLE = new GeometricNodeListEnumeration(n);
            } else {
                this.sNLE.resetForNode(n);
            }

            if (this.sNLE.hasMoreElements()) {
                this.nI = this.sNLE.nextElement().iterator();
            }
        }

        @Override
        public boolean hasMoreElements() {
            if (this.nI.hasNext()) {
                return true;
            } else {
                while (this.sNLE.hasMoreElements()) {
                    this.nI = this.sNLE.nextElement().iterator();
                    if (this.nI.hasNext()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Node nextElement() {
            return this.nI.next();
        }

    }

    @Override
    public Enumeration<Node> getSortedNodeEnumeration(boolean backToFront) {
        // in 2D returns the same as getNodeEnumeration()
        return this.localNodes.elements();
    }

    @Override
    public Enumeration<Node> getNodeEnumeration() {
        return this.localNodes.elements();
    }

    @Override
    public Iterator<Node> iterator() {
        return this.localNodes.iterator();
    }

    // public boolean hasSensitiveInfoChanged() {
    // boolean rval = this.sensitiveInformationChanged;
    // this.sensitiveInformationChanged = false;
    // return rval;
    // }

    /**
     * Enumeration to traverse the Lists of nodes to find possible neighbors.
     */
    class GeometricNodeListEnumeration implements Enumeration<NodeListInterface> {

        // this is the collection of all the squares where neighbors may be.
        private SquarePositionCollection squares;
        // the location of the node this enumeration is for
        private SquarePos location;
        // the enumeration over the
        private Enumeration<SquarePos> listEnumeration;
        // the mask where the possible neighbors nay be. Initially all neighboring
        // squares are possible. If the node is in a
        // "border"-square some 1's are changed to 0's.
        private int[][] mask = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};

        /**
         * The constructor for the GeometricNodeListEnumeration class. This class is
         * used to traverse all the nodelists saved in the datastructure. It returns a
         * enumeration over NodeLists which are used by the GeometricNodeEnumeration.
         *
         * @param n The Node to get all the NodeLists of potential neighbors.
         */
        public GeometricNodeListEnumeration(Node n) {
            this.squares = new SquarePositionCollection();
            // calculate the position in the datastructure of the node
            this.location = Geometric2DNodeCollection.this.getPosOfNode(n);

            // fill the vector with the addresses of the neighborhood squares
            if (this.location.getX() == 0) {
                this.mask[0][0] = 0;
                this.mask[0][1] = 0;
                this.mask[0][2] = 0;
            }
            if (this.location.getX() + 1 == Geometric2DNodeCollection.this.xDim) {
                this.mask[2][0] = 0;
                this.mask[2][1] = 0;
                this.mask[2][2] = 0;
            }
            if (this.location.getY() == 0) {
                this.mask[0][0] = 0;
                this.mask[1][0] = 0;
                this.mask[2][0] = 0;
            }
            if (this.location.getY() + 1 == Geometric2DNodeCollection.this.yDim) {
                this.mask[0][2] = 0;
                this.mask[1][2] = 0;
                this.mask[2][2] = 0;
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (this.mask[j][i] != 0) {
                        this.squares.add(this.location.getX() + (j - 1), this.location.getY() + (i - 1));
                    }
                }
            }

            // initialize the enumeration over the vector
            this.listEnumeration = this.squares.elements();
        }

        /**
         * This method resets the nodeListIterator. Do not allocate a new Enumeration in
         * every step but reset the current one. This reduces the number of allocated
         * objects and thus the amount of garbage the garbage collector has to collect.
         *
         * @param n The node to create reset the enumeration for.
         */
        public void resetForNode(Node n) {

            this.squares.clear();

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    this.mask[j][i] = 1;
                }
            }

            // calculate the position in the datastructure of the node
            this.location = Geometric2DNodeCollection.this.getPosOfNode(n);

            // fill the vector with the addresses of the neighborhood squares
            if (this.location.getX() == 0) {
                this.mask[0][0] = 0;
                this.mask[0][1] = 0;
                this.mask[0][2] = 0;
            }
            if (this.location.getX() + 1 == Geometric2DNodeCollection.this.xDim) {
                this.mask[2][0] = 0;
                this.mask[2][1] = 0;
                this.mask[2][2] = 0;
            }
            if (this.location.getY() == 0) {
                this.mask[0][0] = 0;
                this.mask[1][0] = 0;
                this.mask[2][0] = 0;
            }
            if (this.location.getY() + 1 == Geometric2DNodeCollection.this.yDim) {
                this.mask[0][2] = 0;
                this.mask[1][2] = 0;
                this.mask[2][2] = 0;
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (this.mask[j][i] != 0) {
                        this.squares.add(this.location.getX() + (j - 1), this.location.getY() + (i - 1));
                    }
                }
            }
            // initialize the enumeration over the vector
            this.listEnumeration = this.squares.elements();
        }

        @Override
        public boolean hasMoreElements() {
            return this.listEnumeration.hasMoreElements();
        }

        @Override
        public NodeListInterface nextElement() {
            SquarePos sp = this.listEnumeration.nextElement();
            return Geometric2DNodeCollection.this.lists[sp.getX()][sp.getY()];
        }
    }

    @Override
    public Node getRandomNode() {
        return super.defaultGetRandomNode(this.localNodes);
    }

    @Override
    public int size() {
        return this.localNodes.size();
    }
}
