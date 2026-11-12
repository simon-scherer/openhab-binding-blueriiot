package org.openhab.core.thing;
public interface Bridge extends Thing { java.util.List<Thing> getThings(); org.openhab.core.thing.binding.BridgeHandler getHandler(); }
