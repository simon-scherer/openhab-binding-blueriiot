package org.openhab.core.config.discovery;

import org.openhab.core.thing.ThingUID;

/** Compile-time stub. */
public class DiscoveryResultBuilder {
    public static DiscoveryResultBuilder create(ThingUID thingUID) { return new DiscoveryResultBuilder(); }
    public DiscoveryResultBuilder withProperty(String key, Object value) { return this; }
    public DiscoveryResultBuilder withRepresentationProperty(String representationProperty) { return this; }
    public DiscoveryResultBuilder withBridge(ThingUID bridgeUID) { return this; }
    public DiscoveryResultBuilder withLabel(String label) { return this; }
    public DiscoveryResult build() { return new DiscoveryResult() { }; }
}
