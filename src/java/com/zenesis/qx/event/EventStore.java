package com.zenesis.qx.event;

import com.zenesis.qx.event.EventManager.BoundListeners;

/**
 * Implements an event store, ie the backing for an Eventable implementation
 *  
 * @author john
 *
 */
public class EventStore implements Eventable {
    
    private final Object keyObject;
    private final BoundListeners listeners = new BoundListeners();
    private int eventsDisabled;

    public EventStore(Object keyObject) {
        super();
        this.keyObject = keyObject;
    }

    @Override
    public void disableEvents() {
        eventsDisabled++;
    }

    @Override
    public void enableEvents() {
        if (eventsDisabled < 1)
            throw new IllegalArgumentException("Cannot enable events more than they have been enabled");
        eventsDisabled--;
    }

    @Override
    public boolean eventsEnabled() {
        return eventsDisabled != 0;
    }

    @Override
    public boolean addListener(String eventName, EventListener listener) throws IllegalArgumentException {
        return listeners.addListener(eventName, listener);
    }

    @Override
    public boolean removeListener(String eventName, EventListener listener) {
        return listeners.removeListener(eventName, listener);
    }

    @Override
    public boolean hasListener(String eventName, EventListener listener) {
        return listeners.hasListener(eventName, listener);
    }

    @Override
    public void fireEvent(String eventName) {
        listeners.fireDataEvent(new Event(keyObject, keyObject, eventName, null));
    }

    @Override
    public void fireDataEvent(String eventName, Object data) {
        listeners.fireDataEvent(new Event(keyObject, keyObject, eventName, data));
    }
    
   

}
