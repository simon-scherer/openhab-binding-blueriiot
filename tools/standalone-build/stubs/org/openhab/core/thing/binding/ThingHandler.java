package org.openhab.core.thing.binding;
import org.openhab.core.thing.*; import org.openhab.core.types.Command;
public interface ThingHandler { Thing getThing(); void initialize(); void dispose(); void handleCommand(ChannelUID c, Command cmd); void bridgeStatusChanged(ThingStatusInfo i); void handleRemoval();
 default java.util.Collection<Class<? extends ThingHandlerService>> getServices(){ return java.util.Set.of(); } }
