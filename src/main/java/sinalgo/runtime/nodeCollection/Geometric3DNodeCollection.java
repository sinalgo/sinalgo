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
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * This 3D node collection implementation stores nodes placed in a 3 dimensional
 * field whose connectivity has a maximal range. I.e. nodes within connection
 * range have a known maximal distance between each other. This critical
 * distance, call it rMax, is used by this node collection to teselate the space
 * into cells of side length rMax. This allows to efficiently retrive a subset
 * of potentail neighbors of a node, given the position of the node by returning
 * the matrix cells adjacent to the cell containing the node.
 */
public class Geometric3DNodeCollection extends AbstractNodeCollection {

    private int numX, numY, numZ; // cardinality of matrix in each dimension

    // a hierarchical matrix to access the nodes based on their position
    private NodeListInterface[][][] list;

    // a flat list for fast iteration over all nodes
    private Vector<Node> flatList = new Vector<>();
    private boolean flatListChanged;

    private Node[] sortedNodeArray = new Node[1];
    private int sortedNodeArraySize; // Number of non-null nodes in sortedNodeArray
    private DepthComparator myDepthComparator;

    // the maximal distance between any two connected nodes
    private double rMax;

    // Flag indicating whether some nodes have changed the matrix cell
    // private boolean sensitiveInformationChanged = false;

    // The enumeration object used repeatedly to access the potential neighbors of a
    // node
    private Geometric3DNodeEnumeration enumeration = new Geometric3DNodeEnumeration();

    /**
     * Default constructor. Creates and initializes the geometric node collection.
     */
    public Geometric3DNodeCollection() {
        // Immediately stop execution if rMax is not defined in the xml config file.
        try {
            this.rMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
        if (this.rMax <= 0) {
            throw new SinalgoFatalException("Geometric3DNodeCollection: The value of rMax from the config file entry "
                    + "<GeometricNodeCollection rMax=\"" + this.rMax + "\"/>"
                    + "is not valid. The value of rMax must be positive.");
        }
        // determine cardinality of matrix in each dimension
        int dimX = Configuration.getDimX();
        this.numX = (int) Math.ceil(dimX / this.rMax);
        int dimY = Configuration.getDimY();
        this.numY = (int) Math.ceil(dimY / this.rMax);
        int dimZ = Configuration.getDimZ();
        this.numZ = (int) Math.ceil(dimZ / this.rMax);
        // create and initialize the matrix
        this.list = new DLLNodeList[this.numX][this.numY][this.numZ];
        for (int i = 0; i < this.numX; i++) {
            for (int j = 0; j < this.numY; j++) {
                for (int k = 0; k < this.numZ; k++) {
                    this.list[i][j][k] = new DLLNodeList(true);
                }
            }
        }
    }

    /**
     * Maps a component of a position-coordinate to the corresponding offset into
     * the matrix.
     */
    private int mapCoord(double c) {
        return (int) Math.floor(c / this.rMax);
    }

    private long lastVersionNumber;

    @Override
    public Enumeration<Node> getSortedNodeEnumeration(boolean backToFront) {
        if (!Configuration.isDraw3DGraphNodesInProperOrder()) {
            return this.flatList.elements();
        }
        PositionTransformation t3d = Main.getRuntime().getTransformator();
        long actualVersionNumber = t3d.getVersionNumber();
        if ((this.lastVersionNumber != actualVersionNumber) || (this.flatListChanged)) {
            // the transformation has changed. Need to resort the array.
            this.lastVersionNumber = actualVersionNumber;
            this.flatListChanged = false;

            this.sortedNodeArray = this.flatList.toArray(this.sortedNodeArray);
            this.sortedNodeArraySize = this.flatList.size();
            if (this.sortedNodeArraySize > 1) {
                if (this.myDepthComparator == null) {
                    this.myDepthComparator = new DepthComparator();
                }
                Arrays.sort(this.sortedNodeArray, 0, this.sortedNodeArraySize, this.myDepthComparator);
            }
        }

        return new ArrayEnumeration(backToFront);
    }

    @Override
    public Enumeration<Node> getNodeEnumeration() {
        return this.flatList.elements();
    }

    @Override
    public Iterator<Node> iterator() {
        return new StateSensitiveIterator();
    }

    /**
     * This is a wrapper class around an Iterator. It sets the flatListChanted field
     * to true when someone deletes a Node from the flatList by calling the remove
     * method of the iterator.
     */
    private class StateSensitiveIterator implements Iterator<Node> {

        Iterator<Node> iter;

        /**
         * The constructor to create a new StateSensitiveIterator.
         */
        private StateSensitiveIterator() {
            this.iter = Geometric3DNodeCollection.this.flatList.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @Override
        public Node next() {
            return this.iter.next();
        }

        @Override
        public void remove() {
            this.iter.remove();
            Geometric3DNodeCollection.this.flatListChanged = true;
        }

    }

    @Override
    public Enumeration<Node> getPossibleNeighborsEnumeration(Node n) {
        this.enumeration.resetForNode(n);
        return this.enumeration;
    }

    @Override
    protected void _addNode(Node n) {
        n.setHoldInNodeCollection(true);

        Position pos = n.getPosition();
        int x = this.mapCoord(pos.getXCoord());
        int y = this.mapCoord(pos.getYCoord());
        int z = this.mapCoord(pos.getZCoord());
        n.setNodeCollectionInfo(new CubePos(x, y, z));

        this.list[x][y][z].addNode(n);
        this.flatList.add(n);
        this.flatListChanged = true;
        // sensitiveInformationChanged = true;
    }

    @Override
    protected void _removeNode(Node n) {
        n.setHoldInNodeCollection(false);
        CubePos pos = (CubePos) n.getNodeCollectionInfo();
        if (!this.list[pos.getX()][pos.getY()][pos.getZ()].removeNode(n)) {
            // the node was not located where it said! ERROR!
            throw new SinalgoFatalException("Geometric3DNodeCollection.removeNode(Node):\n" + "A node is being removed, but it is not "
                    + "located in the matrix cell " + "in which it claims to be.");
        }
        this.flatList.remove(n);
        this.flatListChanged = true;
        n.setNodeCollectionInfo(null);
    }

    @Override
    protected void _updateNodeCollection(Node n) {
        if (!n.isHoldInNodeCollection()) {
            return; // the node is not yet hold by this node collection
        }
        // sensitiveInformationChanged = true;
        // test whether the node has changed the cell of the matrix

        // the old position in the matrix
        CubePos oldPos = (CubePos) n.getNodeCollectionInfo();
        // the new position in the matrix
        Position pos = n.getPosition();
        int x = this.mapCoord(pos.getXCoord());
        int y = this.mapCoord(pos.getYCoord());
        int z = this.mapCoord(pos.getZCoord());
        if (oldPos.getX() != x || oldPos.getY() != y || oldPos.getZ() != z) {
            // the node needs to be stored in a different cell of the matrix
            // remove it from the old matrix cell...
            if (!this.list[oldPos.getX()][oldPos.getY()][oldPos.getZ()].removeNode(n)) {
                throw new SinalgoFatalException(
                        "Geometric3DNodeCollection.updateNodeCollection(Node):\nA node is being removed from the matrix, but it is not located in the matrix cell in which it claims to be.");
            }
            // ... and add it to the new matrix cell
            this.list[x][y][z].addNode(n);
            // update the matrix-cell info stored at the node
            oldPos.setX(x);
            oldPos.setY(y);
            oldPos.setZ(z);
        }
    }

    // public boolean hasSensitiveInfoChanged() {
    // boolean tmp = sensitiveInformationChanged;
    // sensitiveInformationChanged = false;
    // return tmp;
    // }

    @Override
    public Node getRandomNode() {
        return super.defaultGetRandomNode(this.flatList);
    }

    @Override
    public int size() {
        return this.flatList.size();
    }

    /**
     * An enumeration that helps to iterate over all potential nodes given a certain
     * node.
     * <p>
     * Note that you must call resetForNode(Node) prior to using any instance of
     * this class.
     */
    class Geometric3DNodeEnumeration implements Enumeration<Node> {

        int ox, oy, oz; // base position for the 3-dimensional iteration
        int dx, dy, dz = -1; // the offset from the base position
        Iterator<Node> iterator;

        /**
         * Prepares this enumeration for a given node n. This method needs to be called
         * prior to using the enumeration object.
         *
         * @param n The node for which the potential neighbors should be enumerated.
         */
        public void resetForNode(Node n) {
            Position pos = n.getPosition();
            // The base position is located below the matrix cell where this node is
            // contained, s.t. we can add {0|1|2} to each coordinate in all different
            // permuations to obtain the 27 different fields.
            this.ox = Math.min(Geometric3DNodeCollection.this.numX - 1, Geometric3DNodeCollection.this.mapCoord(pos.getXCoord())) - 1;
            this.oy = Math.min(Geometric3DNodeCollection.this.numY - 1, Geometric3DNodeCollection.this.mapCoord(pos.getYCoord())) - 1;
            this.oz = Math.min(Geometric3DNodeCollection.this.numZ - 1, Geometric3DNodeCollection.this.mapCoord(pos.getZCoord())) - 1;
            this.dx = this.dy = 0;
            this.dz = -1; // is incremented in call 'getNextValidMatrixCell'
            this.gotoNextValidMatrixCell(); // get the first iterator - there's at least one
        }

        /**
         * Loops through ox, oy and oz to the next valid cell
         *
         * @return true if another valid matrix cell was found, otherwise false.
         */
        private boolean gotoNextValidMatrixCell() {
            do {
                this.dz++;
                if (this.dz > 2) {
                    this.dz = 0;
                    this.dy++;
                }
                if (this.dy > 2) {
                    this.dy = 0;
                    this.dx++;
                }
                if (this.dx > 2) {
                    return false; // we have visited all neighboring matrix cells
                }
            }
            while (this.ox + this.dx < 0 || this.oy + this.dy < 0 || this.oz + this.dz < 0 || this.ox + this.dx >= Geometric3DNodeCollection.this.numX || this.oy + this.dy >= Geometric3DNodeCollection.this.numY
                    || this.oz + this.dz >= Geometric3DNodeCollection.this.numZ);
            this.iterator = Geometric3DNodeCollection.this.list[this.ox + this.dx][this.oy + this.dy][this.oz + this.dz].iterator(); // get new iterator
            return true;
        }

        @Override
        public boolean hasMoreElements() {
            if (this.iterator.hasNext()) {
                return true;
            } else {
                if (!this.gotoNextValidMatrixCell()) { // we have visited all neighboring matrix cells
                    return false;
                }
                return this.hasMoreElements(); // tail recursive call
            }
        }

        @Override
        public Node nextElement() {
            return this.iterator.next();
        }
    }

    // An enumeration over the array of sorted nodes.
    private class ArrayEnumeration implements Enumeration<Node> {

        boolean backToFront;

        /**
         * Constructs an arrayEnumeration with the given modal.
         *
         * @param backToFront Indicates whether the array has to be sorted forward or backward.
         *                    Set to true to sort it so that the element with the biggest
         *                    distance to the viewer has is traversed first.
         */
        private ArrayEnumeration(boolean backToFront) {
            this.backToFront = backToFront;
        }

        int currentIndex;

        @Override
        public boolean hasMoreElements() {
            return this.currentIndex < Geometric3DNodeCollection.this.sortedNodeArraySize;
        }

        @Override
        public Node nextElement() {
            if (this.backToFront) {
                return Geometric3DNodeCollection.this.sortedNodeArray[this.currentIndex++]; // implicit incrementation
            } else {
                return Geometric3DNodeCollection.this.sortedNodeArray[Geometric3DNodeCollection.this.sortedNodeArraySize - ++this.currentIndex]; // implicit incrementation BEFORE
                // evaulation to have the offset one
                // smaller
            }
        }
    }

    /**
     * Sorts the elements such that nodes in the back are drawn first
     */
    private class DepthComparator implements Comparator<Node> {

        PositionTransformation pt;
        Transformation3D t3d;

        /**
         * Creates a new DepthComparator instance. Note that the DepthComparator only
         * does something in 3 Dimensions.
         */
        private DepthComparator() {
            this.pt = Main.getRuntime().getTransformator();
            if (this.pt instanceof Transformation3D) {
                this.t3d = (Transformation3D) this.pt;
            }
        }

        @Override
        public int compare(Node n1, Node n2) {
            if (this.t3d != null) {
                double zN1 = this.t3d.translateToGUIPositionAndGetZOffset(n1.getPosition());
                double zN2 = this.t3d.translateToGUIPositionAndGetZOffset(n2.getPosition());
                return (int) (zN1 - zN2);
            } else {
                // The deptcompator is not used in 2 dimensions.
                return 0;
            }
        }
    }
}
