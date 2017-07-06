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
package sinalgo.tools.storage;

import java.util.Vector;

/**
 * Each entry of the DoublyLinkedList implementation must implement this rather
 * unconventional interface. 
 * <p>
 * The doubly linked list requires each entry to store a list of fingers, which 
 * has to be implemented in the implementation of the entry. The only method required
 * by this interface returns this list. E.g. add the following lines to your entry 
 * implementation:   
 * 
 <pre>
 private DLLFingerList dllFingerList = new DLLFingerList();
 public DLLFingerList getDLLFingerList() {
    return dllFingerList;
 }  
 </pre>
 * This finger-list allows this entry to be contained in several lists at the same time,
 * as it stores a finger for each list.
 */
public interface DoublyLinkedListEntry {

	/**
	 * This object has to be contained in each entry that is stored in the doubly linked list.  
	 * It manages the 'next' and 'previous' pointers to the different lists this entry is stored in.
	 */
	public class DLLFingerList {
		/**
		 * The list of fingers. The fingers in this list 'point' to the doublyLinkedLists this element
		 * is contained in.
		 */
		private Vector<Finger> list = new Vector<Finger>(1);
		/**
		 * THe number of fingers this list.
		 */
		private int numberOfUsedFingers;
		
		/**
		 * Gets the finger of this entry associated with a given list. 
		 * @param dll The list to which the searched finger should be associated with.
		 * @return The finger of this entry associated with the given list, 
		 * null if no finger is associated with the list.
		 */
		public Finger getFinger(DoublyLinkedList<?> dll) {
			for(int i=0; i<numberOfUsedFingers; i++) {
				Finger f = list.elementAt(i);
				if(f != null && f.list == dll) {
					return f;
				}
			}
			return null;
		}
		
		/**
		 * Returns a new (or free) finger and associates the finger to the given list and entry.
		 * <p>
		 * <b>Note:</b> This method does not check whether this entry is already contained in the list.
		 * @param dll The list to which the new finger should be associated with.
		 * @param entry The entry fow which the finger is used.
		 * @return a new (or free) finger.
		 */
		public Finger getNewFinger(DoublyLinkedList<?> dll, DoublyLinkedListEntry entry) {
			Finger f;
			if(numberOfUsedFingers < list.size()) {
				f = list.elementAt(numberOfUsedFingers);
			} else {
				f = new Finger();
				list.add(f);
			}
			f.list = dll;
			f.object = entry;
			numberOfUsedFingers++;
			return f;
		}
		
		/**
		 * Removes a finger from the list of fingers. Nothing happens if the finger 
		 * is not contained in the list of fingers of this entry. 
		 * @param f The finger to remove. 
		 * @param keep Set to true if the finger-object is to be kept for reuse, otherwise false.
		 */
		public void releaseFinger(Finger f, boolean keep) {
			for(int i=0; i<numberOfUsedFingers; i++) {
				if(f == list.elementAt(i)) {
					if(keep) {
						releaseFingerAt(i);
					} else {
						list.remove(i);
						numberOfUsedFingers--;
					}
					break;
				}
			}
			resizeVector();
		}
		
		/**
		 * Removes a finger from the list of fingers. Nothing happens if the finger 
		 * is not contained in the list of fingers of this entry. 
		 * @param dll The list to which the finger points, which needs to be removed.
		 * @param keep Set to true if the finger-object is to be kept for reuse, otherwise false.
		 */
		public void releaseFinger(DoublyLinkedList<?> dll, boolean keep) {
			for(int i=0; i<numberOfUsedFingers; i++) {
				Finger f = list.elementAt(i);
				if(f != null && f.list == dll) {
					if(keep) {
						releaseFingerAt(i);
					} else {
						list.remove(i);
						numberOfUsedFingers--;
					}
					break; // at most one finger per dll
				}
			}
			resizeVector();
		}
		
		/**
		 * Whenever an element is removed from a doubly linked list, the corresponding finger is not used anymore.
		 * This method ensures that the number of free finger does not grwo too large.  
		 * <b>This functionality has not been impleented.</b> 
		 */
		private void resizeVector() {
			// TODO.  resize of finger list:
			// a) remove free fingers (but not all free fingers)
			// b) resize the vector if necessary
			// can work with
			// list.setSize(45); set to more than numberOfUsedFingers
			// list.trimToSize();
		}
		
		/**
		 * Sets a finger to unused and moves it to the back of the list, where the unused
		 * fingers are stored. (all unused fingers are stored at the end of the list.) 
		 * @param offset
		 */
		private void releaseFingerAt(int offset) {
			Finger f = list.elementAt(offset);
			f.reset();
			numberOfUsedFingers--; // is now offset that currently points to last used finger
			if(offset < numberOfUsedFingers) {
				list.set(offset, list.elementAt(numberOfUsedFingers));
				list.set(numberOfUsedFingers, f);
			} // else: is already last used finger
		}
	} // end of class DLLFingerList

	/**
	 * @return Retrives the list of fingers of the entry.
	 */
	public DLLFingerList getDoublyLinkedListFinger(); // getDLLFingerList

	/**
	 * In the doubly linked list, each entry must be equiped with two pointers: the
	 * next and previous element in the lest. This Finger class contains this information
	 * along with implementation specific info for the doubly linked list.  
	 */
	public class Finger {
		/**
		 * the next entry in the list
		 */
		public Finger next;
		/**
		 * the previous entry in the list
		 */
		public Finger previous;
		/**
		 * the entry-object
		 */
		public DoublyLinkedListEntry object;
		/**
		 * the list this object belongs to.
		 */
		public DoublyLinkedList<?> list;
		
		/**
		 * Sets all pointers of this finger to null. 
		 */
		public void reset() {
			next = previous = null;
			object = null;
			list = null;
		}

		/**
		 * Creates a new finger for the specified list.
		 * @param list the list the finger has to point to.
		 * @param entry the entry this finger is for.
		 */
		public Finger(DoublyLinkedList<?> list, DoublyLinkedListEntry entry) {
			this.list = list;
			this.object = entry;
		}
		
		/**
		 * Default constructor. 
		 */
		public Finger() {
		}
	}

}
