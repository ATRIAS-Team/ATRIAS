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

public class RingBuffer<E> {
    private final int DEFAULT_CAPACITY = 16;
    private int capacity;
    public volatile int readSequence;   //tail
    public volatile int writeSequence;  //head

    public E[] data;

    public RingBuffer(int capacity) {
        this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
        this.data = (E[]) new Object[this.capacity];
        this.readSequence = 0;
        this.writeSequence = 0;
    }

    public boolean isFull(){return writeSequence - readSequence == capacity;}

    public boolean isEmpty(){return writeSequence == readSequence;}

    public boolean write(E element) {
        if (!isFull()) {
            data[writeSequence++ % capacity] = element;
            return true;
        }
        return false;
    }

    public E read() {
        if (!isEmpty()) {
            return data[readSequence++ % capacity];
        }
        return null;
    }
}
