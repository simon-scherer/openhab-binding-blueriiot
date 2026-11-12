package org.openhab.core.config.discovery;

import java.util.Set;

import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;

/** Compile-time stub. */
public abstract class AbstractThingHandlerDiscoveryService<T extends ThingHandler> extends AbstractDiscoveryService
        implements ThingHandlerService {
    protected T thingHandler;

    protected AbstractThingHandlerDiscoveryService(Class<T> thingClazz, Set<ThingTypeUID> supportedThingTypes,
            int timeout, boolean backgroundDiscoveryEnabledByDefault) {
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
    }

    @Override
    public T getThingHandler() {
        return thingHandler;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void dispose() {
    }
}
