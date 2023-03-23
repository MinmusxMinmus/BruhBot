package util;

public class Buffer<T> { // TODO add a queue

    private boolean hasData;
    private int opcode;
    private T data;

    public Buffer() {
        this.opcode = 0;
        hasData = false;
    }

    // Basic insertion functions
    public synchronized void queue(int opcode) {
        while (hasData) try {
            wait();
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.opcode = opcode;
        this.hasData = true;
        notify();
    }

    public synchronized void queue(int opcode, T data) {
        while (hasData) try {
            wait();
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.opcode = opcode;
        this.data = data;
        this.hasData = true;
        notify();
    }

    public synchronized void queue(T data) {
        while (hasData) try {
            wait();
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.data = data;
        this.hasData = true;
        notify();
    }

    public synchronized void queueOverride(int opcode) {
        this.opcode = opcode;
        if (!hasData) {
            this.hasData = true;
            notify();
        }
    }

    public synchronized void queueOverride(int opcode, T data) {
        this.opcode = opcode;
        this.data = data;
        if (!hasData) {
            this.hasData = true;
            notify();
        }
    }

    public synchronized void queueOverride(T data) {
        this.data = data;
        if (!hasData) {
            this.hasData = true;
            notify();
        }
    }

    // Basic extraction function
    public synchronized Pair<Integer, T> getData() {
        while (!hasData) try {
            wait();
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        hasData = false;
        notify();
        return new Pair<>(opcode, data);
    }

    public synchronized T getObject() {
        while (!hasData) try {
            wait();
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        hasData = false;
        notify();
        return data;
    }
}
