package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBuffer<E> {
    private final int DEFAULT_CAPACITY = 16;
    private int capacity;
    public AtomicInteger readSequence;   //tail
    public AtomicInteger writeSequence;  //head

    public PropertyChangeSupport pcs;

    public E[] data;

    public RingBuffer(int capacity) {
        this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
        this.data = (E[]) new Object[this.capacity];
        this.readSequence = new AtomicInteger(0);
        this.writeSequence = new AtomicInteger(0);
        this.pcs = new PropertyChangeSupport(this);
    }

    public boolean isFull(){return writeSequence.get() - readSequence.get() == capacity;}

    public boolean isEmpty(){return writeSequence.get() == readSequence.get();}

    public boolean write(E element) {
        if (!isFull()) {
            data[writeSequence.incrementAndGet() % capacity] = element;
            pcs.firePropertyChange("data", null, data);
            return true;
        }
        return false;
    }

    public E read() {
        if (!isEmpty()) {
            int index = readSequence.incrementAndGet() % capacity;
            E result = data[index];
            data[index] = null;
            return result;
        }
        return null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener){
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener){
        pcs.removePropertyChangeListener(listener);
    }
}
