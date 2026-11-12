package org.openhab.core.thing.binding;

/** Compile-time stub. */
public interface ThingHandlerService {
    void setThingHandler(ThingHandler handler);

    ThingHandler getThingHandler();

    default void initialize() {
    }

    default void dispose() {
    }
}
