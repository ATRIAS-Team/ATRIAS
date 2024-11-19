package io.github.agentsoz.ees.util;

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
