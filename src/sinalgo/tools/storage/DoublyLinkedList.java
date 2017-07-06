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

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import sinalgo.tools.logging.Logging;

/**
 * A doubly linked list implementation which allows insertion and deletion of objects in O(1).
 * <p>
 * <i>This list implementation does not support multiple entries of the same object, neither 
 * null objects.</i> I.e. each object may only be contained once per list.  
 * <p>
 * This special implementation of a linked list targets applications that store huge amount of
 * objects in a list, and often remove specific elements. In the java.util.LinkedList implementation,
 * removal of an object requires to iterate over the list to find the object to be removed. However,
 * if the object itself knew the 'next' and 'previous' pointers normally used in a doubly linked list,
 * this would not be necessary.
 * <p>
 * In this implementation, we provide a generic linked list, which requires that each entry knows 
 * the 'next' and 'previous' pointer. To do so, each entry must implement the 
 * DoubleLinkedListEntry interface.
 * <p>
 * When storing an entry only in one list, a single pointer pair ['next', 'previous'] would be sufficient.
 * However, adding the entry to several lists would not be possible. Therefore, the DoubleLinkedListEntry 
 * interface requires its subclass to keep a vector of <code>Finger</code> objects, which hold the two pointers 
 * ['next', 'previous'] for each list this entry is contained in.   
 * <p>
 * In order to allow for increased performance, each list can specify whether the <code>Finger</code> is also
 * removed from the entry when it is removed from the list. Not removing the <code>Finger</code> may increase 
 * performance if the same object is repeatedly added and removed from to lists (not necessarily the same). 
 * Remember that not deleting the <code>Finger</code> from an entry when it is removed implies, that the size 
 * of the entry object is actually bigger than it needs be. Therefore, only set the flag to not delete the 
 * <code>Finger</code> for lists where the objects contained in the lists exist long and are added and removed 
 * often to the lists. 
 *   
 * @param <E> The generic type the DLL is created for.
 */
public class DoublyLinkedList<E extends DoublyLinkedListEntry> implements Iterable<E>{

	private boolean keepFinger; // if true, the finger is not removed from the objects list after the object is removed from this list.
	private int size = 0; // # of elements in the list
	private int modCount = 0; // # of modifications
	private DoublyLinkedListEntry.Finger head = new DoublyLinkedListEntry.Finger(null, null); // before the first entry of the list, the terminator 
	private DoublyLinkedListEntry.Finger tail = head; // the last entry, points to head if the list is empty
	
	/**
	 * Creates a new instance of a Doubly Linked List.
	 * <p>
	 * This method lets you specify whether entries keep their finger-entry 
	 * when they are removed from this list. This may increase performance if the
	 * same entries are added and removed several times to/from this list. Note that the 
	 * iterator() method always returns a new iterator ignoring the parameters set at the
	 * constructor. Use the getIterator method to get a iterator depending on the parameters.
	 * 
	 * 
	 * @param keepFinger If set to true, entries keep their finger for later reuse (in this or a different list)
	 * when they are removed from this list. When set to false, the finger is removed.
	 */
	public DoublyLinkedList(boolean keepFinger) {
		this.keepFinger = keepFinger;
	}

	/**
	 * Default constructor. Creates a new doubly linked list that 
	 * removes the finger from removed entries and creates a new iterator object
	 * for each call to <code>iterator()</code>.
	 */
	public DoublyLinkedList() {
		keepFinger = false;
	}
	
	/**
	 * Appends an entry to the end of the list if it is not already contained in the list.
	 * <p>
	 * <b>NOTE:</b> An entry can be present at most once per list.
	 * @param entry The entry to be added
	 * @return True if the entry was added, false if it was already contained in the list.
	 */
	public boolean append(E entry) {
		return addAfter(entry, tail);
	}
	
	/**
	 * Adds an entry after another entry already in the list.
	 * @param entry The entry to be added
	 * @param after The entry after which the new entry is added
	 * @return True if the entry was added, false if it was already contained in the list.
	 * @throws DoublyLinkedListErrorException if <code>after</code> is not contained in the list.
	 */
	public boolean addAfter(E entry, E after) throws DoublyLinkedListErrorException {
		DoublyLinkedListEntry.Finger pos = after.getDoublyLinkedListFinger().getFinger(this);
		if(pos == null || (pos.next == null && pos.previous == null)) {
			throw new DoublyLinkedListErrorException("Cannot add an element into doubly linked list after an element which is not contained in the list.");
		}
		return addAfter(entry, pos);
	}
	
	/**
	 * Adds an entry after a given finger.
	 * @param entry The entry to be added
	 * @param pos The finger after which this entry is added
	 * @return True if the entry was added, false if it was already contained in the list.
	 */
	private boolean addAfter(E entry, DoublyLinkedListEntry.Finger pos) {
		DoublyLinkedListEntry.Finger f = entry.getDoublyLinkedListFinger().getFinger(this);
		if(f != null) {
			return false; // already in list
		}
		f = entry.getDoublyLinkedListFinger().getNewFinger(this, entry); // get new finger 
		if(pos == tail) { // insert at the end
			f.previous = tail;
			tail.next = f;
			tail = f;
		} else { // insert not after last entry
			f.next = pos.next;
			f.previous = pos;
			pos.next.previous = f; // must exist, as pos != tail
			pos.next = f;
		}
		size ++;
		modCount++;
		return true;
	}
	
	/**
	 * Adds an entry before another entry already in the list.
	 * @param entry The entry to be added
	 * @param before The entry before which the new entry is added
	 * @return True if the entry was added, false if it was already contained in the list.
	 * @throws DoublyLinkedListErrorException if <code>before</code> is not contained in the list.
	 */
	public boolean addBefore(E entry, E before) throws DoublyLinkedListErrorException {
		DoublyLinkedListEntry.Finger pos = before.getDoublyLinkedListFinger().getFinger(this); 
		if(pos == null || (pos.next == null && pos.previous == null)) {
			throw new DoublyLinkedListErrorException("Cannot add an element into doubly linked list before an element which is not contained in the list.");
		}
		return addBefore(entry, pos);
	}
	
	private boolean addBefore(E entry, DoublyLinkedListEntry.Finger pos) {
		DoublyLinkedListEntry.Finger f = entry.getDoublyLinkedListFinger().getFinger(this);
		if(f != null) {
			return false; // already in list
		}
		f = entry.getDoublyLinkedListFinger().getNewFinger(this, entry); // get new finger 
		if(pos == head) { // insert in front (actually, we don't insert BEFORE the head, but after the head)
			f.next = head.next;
			f.previous = head;
			if(head != tail) { // not empty list
				head.next.previous = f;
			} else {
				tail = f;
			}
			head.next = f;
		} else { // insert not before first entry
			f.next = pos;
			f.previous = pos.previous;
			pos.previous.next = f;
			pos.previous = f;
		}
		size ++;
		modCount++;
		return true;
	}
	
	/**
	 * Removes an entry from this list.
	 * @param entry The entry to be removed from this list.
	 * @return True if the entry was in the list, otherwise false.
	 */
	public boolean remove(E entry) {
		DoublyLinkedListEntry.Finger f = entry.getDoublyLinkedListFinger().getFinger(this); 
		return remove(f);
	}

	/**
	 * Same as remove, but with different arguments.
	 * @param entry The entry to be removed from this list.
	 * @return True if the entry was in the list, otherwise false.
	 */
	private boolean remove2(DoublyLinkedListEntry entry) {
		DoublyLinkedListEntry.Finger f = entry.getDoublyLinkedListFinger().getFinger(this); 
		return remove(f);
	}
	
	/**
	 * Removes an entry given its finger from this list.
	 * @param f The finger of the entry
	 * @return True if the entry was in this list, otherwise false.
	 */
	private boolean remove(DoublyLinkedListEntry.Finger f) {
		if(f == null) {
			return false; // not in list and no finger
		}
		if(f.next == null && f.previous == null) {
			f.object.getDoublyLinkedListFinger().releaseFinger(f, keepFinger);
			return false; // not in list, but had a dummy finger. 
		}
		f.previous.next = f.next; // there's always a previous
		if(f.next != null) {
			f.next.previous = f.previous;
		} else { // was last entry
			tail = f.previous;
		}
		f.object.getDoublyLinkedListFinger().releaseFinger(f, keepFinger);
		size--;
		modCount++;
		return true;
	}

	/**
	 * Removes and returns the first entry of the list.
	 * @return The first entry of the list and removes it. Null if the list is empty.
	 */
	@SuppressWarnings("unchecked")
	public E pop() {
		if(head.next != null) {
			DoublyLinkedListEntry e = head.next.object;
			remove(head.next);
			return (E) e;
		}
		return null;
	}
	
	/**
	 * Returns the first entry of the list
	 * @return The first entry of the list, null if the list is empty.
	 */
	@SuppressWarnings("unchecked")
	public E peek() {
		if(head.next != null) {
			return (E) head.next.object;
		}
		return null;
	}
	
	/**
	 * Inserts an entry at the beginning of the list.
	 * @param entry The entry to be added.
	 * @return True if the entry was added to the list, false if the entry was already contained in the list.
	 */
	public boolean push(E entry) {
		return addBefore(entry, head); // note that this does not insert the element BEFORE the special entry 'head', but after head as first elelemtn of the list.
	}
	
	/**
	 * @return The number of entries in this list.
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns true if the list is empty, otherwise false.
	 * @return True if the list is empty, otherwise false.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public ReusableListIterator<E> iterator() {
		return new ListItr(0);
	}
	
	/**
	 * Retrieves the element at a given index in the list.
	 * @param index The zero-based index of the element to retrieve in the list. 
	 *        E.g. 0 for the first element, 1 for the second, ..., (size-1) for the last.
	 * @return The element at the given index 
	 * @throws: ArrayIndexOutOfBoundsException if the index is negative or not less than the current size of this Vector object. given.
	 */
	public E elementAt(int index) throws ArrayIndexOutOfBoundsException {
		for(E e : this) {
			if(index == 0) {
				return e;
			}
			index--;
		}
		throw new ArrayIndexOutOfBoundsException(Logging.getCodePosition() + " Invalid index: index=" + index + " size of list=" + size);
	}

	/**
	 * Removes and returns the element at a given index in the list.
	 * @param index The zero-based index of the element to retrieve in the list. 
	 *        E.g. 0 for the first element, 1 for the second, ..., (size-1) for the last.
	 * @return The element at the given index 
	 * @throws: ArrayIndexOutOfBoundsException if the index is negative or not less than the current size of this Vector object. given.
	 */
	public E remove(int index) throws ArrayIndexOutOfBoundsException {
		E e = elementAt(index);
		this.remove(e);
		return e;
	}

	
	
	
//	/**
//	 * This method returns a Iterator. It either resets the reusable iterator or creates a 
//	 * new one depending on the parameters set in the constructor.
//	 * 
//	 * @return An iterator over the list
//	 */
//	public ReusableListIterator<E> getIterator(){
//		if(reuseIterator) {
//			return reusableIterator();
//		} else {
//			return newIterator();
//		}		
//	}
	
//	/**
//	 * Returns an iterator object associated with this list. Successive calls to this
//	 * method return the same iterator object. In each call, the iterator is reset, such
//	 * that iteration over all elements is possilbe.
//	 * <p> 
//	 * <b>Note:</b> If this list was initialized with the <code>reuseIterator</code> flag set
//	 * to true, a call to <code>getIterator()</code> is equivalent to calling this method.
//	 * @return The iterator associated with this list. 
//	 */
//	public ReusableListIterator<E> reusableIterator() {
//		iterator.reset();
//		return iterator;
//	}
	
//	/**
//	 * Creates and returns a new, independent instance of an iterator for this list. 
//	 * @return A new iterator instance for this list.
//	 */
//	public ReusableListIterator<E> newIterator() {
//		return new ListItr(0);
//	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "[";
		int count = 0;
		for(E e : this) {
			count++;
			s += e.toString() + ((count < size) ? ", " : "");
		}
		return s + "]";
	}

	/**
	 * An iterator implementation, mostly copied from java.util.LinkedList.
	 * @see java.util.LinkedList
	 */
	private class ListItr implements ReusableListIterator<E> {
		private DoublyLinkedListEntry.Finger lastReturned = head;
		private DoublyLinkedListEntry.Finger next; // finger of next element to be returned
		private int nextIndex; // 0-based index of next element to be returned
		private int expectedModCount = modCount;
		
		/**
		 * Create a new ListItr Object and initialize it such that the next
		 * returned element is at position with index, where the index starts with
		 * 0 for the first element.
		 * @param index The zero-based offset into the list from where the iterator is initialized.
		 */
		private ListItr(int index) {
			if (index < 0 || index > size) {
				throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
			}
			if (index < (size >> 1)) {
				next = head.next;
				for (nextIndex=0; nextIndex<index; nextIndex++) {
					next = next.next;
				}
			} else {
				next = tail;
				for (nextIndex=size-1; nextIndex>index; nextIndex--) {
					next = next.previous;
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see sinalgo.tools.storage.ReusableIterator#reset()
		 */
		public void reset() {
			nextIndex = 0;
			expectedModCount = modCount;
			lastReturned = head;
			next = head.next;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			if(size == 0){
				return false;
			}
			return nextIndex != size;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@SuppressWarnings("unchecked")
		public E next() {
			checkForComodification();
			if (nextIndex == size) { // reached end of list.
				throw new NoSuchElementException();
			}
			
			lastReturned = next;
			next = next.next;
			nextIndex++;
			return (E) lastReturned.object;
		}
		
		/* (non-Javadoc)
		 * @see java.util.ListIterator#hasPrevious()
		 */
		public boolean hasPrevious() {
			return nextIndex != 0;
		}
		
		/* (non-Javadoc)
		 * @see java.util.ListIterator#previous()
		 */
		@SuppressWarnings("unchecked")
		public E previous() {
			if (nextIndex == 0) {
				throw new NoSuchElementException();
			}
			if(next != null) {
				lastReturned = next = next.previous;
			} else {
				lastReturned = next = tail.previous; // index > 0 => not tail is not head.
			}
			nextIndex--;
			checkForComodification();
			return (E) lastReturned.object;
		}
		
		/* (non-Javadoc)
		 * @see java.util.ListIterator#nextIndex()
		 */
		public int nextIndex() { // the (zero-based) index of the next element to be returned. 
			return nextIndex;
		}
		
		/* (non-Javadoc)
		 * @see java.util.ListIterator#previousIndex()
		 */
		public int previousIndex() { // corresponds to the (zero-based) index of the currently returned element
			return nextIndex-1;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			checkForComodification();
			if(lastReturned == head) {
				throw new IllegalStateException();
			}
			DoublyLinkedListEntry.Finger lastNext = lastReturned.next;
			if(!DoublyLinkedList.this.remove2(lastReturned.object)) {
				// could not remove the object
				throw new IllegalStateException();
			}
			if (next==lastReturned) { // when previous() was called before, lastReturned == next
				next = lastNext;
			} else {
				nextIndex--;
			}
			lastReturned = head; // cannot remove twice
			expectedModCount++;
		}
		
		/* (non-Javadoc)
		 * @see java.util.ListIterator#set(E)
		 */
		public void set(E o) {
			if (lastReturned == head) {
				throw new IllegalStateException();
			}
			checkForComodification();
			
			if(o.getDoublyLinkedListFinger().getFinger(DoublyLinkedList.this) != null) {
				throw new IllegalStateException("Cannot replace the current entry with an entry that is already in the list. This exception occured while iterating over the list.");
			}
			DoublyLinkedListEntry.Finger f = o.getDoublyLinkedListFinger().getNewFinger(DoublyLinkedList.this, o);
			f.next = lastReturned.next;
			f.previous = lastReturned.previous;
			if(lastReturned.next != null) {
				lastReturned.next.previous = f;
			}
			lastReturned.previous.next = f; // there's always a previous
			// release the finger of the old entry
			lastReturned.object.getDoublyLinkedListFinger().releaseFinger(lastReturned, keepFinger);
			if(lastReturned == next) { // restore the pointers
				lastReturned = next = f;
			} else {
				lastReturned = f;
			}
		}
		
		/**
		 * Adds an element to the list if it does not already exist in the list.
		 * @param o The element to be inserted.
		 * @see java.util.ListIterator#add(Object)
		 */
		public void add(E o) {
			checkForComodification();
			lastReturned = head;
			if(next == null) { // append to the end of the list
				if(append(o)) {
					expectedModCount++; 
				}
			} else {
				if(addBefore(o, next)) { // returns true if successfully inserted
					nextIndex++;
					expectedModCount++;
				}
			}
		}
		
		/**
		 * Tests whether the list has been modified other than through the iteration commands. If this is
		 * the case, the method throws a ConcurrentModificationException. 
		 * @throws ConcurrentModificationException if this list was modified other than through the iterators methods.  
		 */
		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}
	} 
}
