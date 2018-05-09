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
package projects.sample6;

import projects.sample6.nodes.nodeImplementations.LeafNode;
import projects.sample6.nodes.nodeImplementations.TreeNode;
import sinalgo.configuration.Configuration;
import sinalgo.nodes.Node;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.Tools;

import java.awt.*;
import java.util.Vector;

/**
 * This class holds customized global state and methods for the framework. The
 * only mandatory method to overwrite is <code>hasTerminated</code> <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 *
 * @see sinalgo.runtime.AbstractCustomGlobal for more details. <br>
 * In addition, this class also provides the possibility to extend the
 * framework with custom methods that can be called either through the menu
 * or via a button that is added to the GUI.
 */
public class CustomGlobal extends AbstractCustomGlobal {

    @Override
    public boolean hasTerminated() {
        return false;
    }

    /**
     * Dummy button to create a tree.
     */
    @AbstractCustomGlobal.CustomButton(buttonText = "Build Tree", toolTipText = "Builds a tree")
    public void sampleButton() {
        int numLeaves = Integer.parseInt(Tools.showQueryDialog("Number of leaves:"));
        int fanOut = Integer.parseInt(Tools.showQueryDialog("Max fanout:"));
        this.buildTree(fanOut, numLeaves);
    }

    /**
     * remove the markings from all nodes
     */
    @AbstractCustomGlobal.CustomButton(buttonText = "unmark", toolTipText = "unmarks all nodes")
    public void unMark() {
        for (Node n : Tools.getNodeList()) {
            n.setColor(Color.BLACK);
        }
        Tools.repaintGUI();
    }

    // a vector of all non-leaf nodes
    private Vector<TreeNode> treeNodes = new Vector<>();
    // the leaves of the node
    private Vector<LeafNode> leaves = new Vector<>();

    /**
     * Builds a tree for the specified number of leaves and fan-out, and removes all
     * nodes in the framework that were added prior to this method call.
     * <p>
     * The method places all leaves on a line at the bottom of the screen and builds
     * a balanced tree on top (bottom up), such that each tree-node is is parentGUI of
     * fanOut children.
     *
     * @param fanOut    The max. fan-out of tree-nodes. E.g. 2 results in a binary tree
     * @param numLeaves The number of leaf-nodes the tree should contain.
     */
    private void buildTree(int fanOut, int numLeaves) {
        if (fanOut < 2) {
            Tools.showMessageDialog("The fanOut needs to be at least 2.\nCreation of tree aborted.");
            return;
        }
        if (numLeaves <= 0) {
            Tools.showMessageDialog("The number of leaves needs to be at least 1.\nCreation of tree aborted.");
            return;
        }

        // remove all nodes (if any)
        SinalgoRuntime.clearAllNodes();
        this.treeNodes.clear();
        this.leaves.clear();
        // Reset ID counter of leaf-nodes
        LeafNode.setSmallIdCounter(0);

        // some vectors to store the nodes that we still need to process
        Vector<TreeNode> toProcess = new Vector<>();
        Vector<TreeNode> toProcess2 = new Vector<>();
        Vector<TreeNode> swap;

        double dx = ((double) Configuration.getDimX()) / (numLeaves + 1); // distance between two leaf-nodes
        double posY = Configuration.getDimY() - 30; // y-offset of all leave nodes

        // create the leaves (incl. assigning their position)
        for (int i = 0; i < numLeaves; i++) {
            LeafNode ln = new LeafNode();
            ln.setPosition((i + 1) * dx, posY, 0);
            ln.finishInitializationWithDefaultModels(true);
            this.leaves.add(ln);
            toProcess.add(ln);
        }

        // the toProcess vector contains nodes that need to be processed.
        // initially, it contains all the leaf-nodes. In the second iteration,
        // all parents of the leaf-nodes, then the grand-paerents of the leaf-nodes
        // and so on.
        while (toProcess.size() > 1) {
            posY -= 100; // the distance along the y-axis between the nodes

            TreeNode tn = null; // the new tree-node to be added
            double leftMostXOffset = 0;
            int numAdded = 0;
            TreeNode currentNode = null;

            // loop over all nodes in the list, and group fanOut nodes, attach them
            // to a new parentGUI (tn), which will be placed in the center of the
            // associated nodes.
            for (TreeNode toProces : toProcess) {
                currentNode = toProces;
                if (tn == null) { // start new parentGUI
                    tn = new TreeNode();
                    tn.finishInitializationWithDefaultModels(true);
                    this.treeNodes.add(tn);
                    toProcess2.add(tn);
                    leftMostXOffset = currentNode.getPosition().getXCoord();
                    numAdded = 0;
                }
                currentNode.addConnectionTo(tn);
                currentNode.setParent(tn);
                numAdded++;
                if (numAdded >= fanOut) {
                    tn.setPosition((leftMostXOffset + currentNode.getPosition().getXCoord()) / 2, posY, 0);
                    tn = null;
                }
            }
            // Cleanup-code. If at the right-side of the tree, the we don't have enough
            // children
            // for the parentGUI, we need to finish the parentGUI's placement outside the loop.
            if (tn != null) {
                tn.setPosition((leftMostXOffset + currentNode.getPosition().getXCoord()) / 2, posY, 0);
            }

            // prepare the toProcess lists to contain the new parents
            toProcess.clear();
            swap = toProcess;
            toProcess = toProcess2;
            toProcess2 = swap;
        }

        // Repaint the GUI as we have added some nodes
        Tools.repaintGUI();
    }

}
