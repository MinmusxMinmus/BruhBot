package events;

import java.time.OffsetDateTime;

public abstract class Event {

    private final OffsetDateTime timeReceived;
    private final long delay;
    private final long id;

    public Event(long delay, long id) {
        this.id = id;
        this.timeReceived = OffsetDateTime.now();
        this.delay = delay;
    }

    public OffsetDateTime getTime() {
        return timeReceived;
    }

    public long getDelay() {
        return delay;
    }

    public long getId() {
        return id;
    }
}
