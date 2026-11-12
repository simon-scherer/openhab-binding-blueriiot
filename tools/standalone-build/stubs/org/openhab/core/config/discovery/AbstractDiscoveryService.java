package org.openhab.core.config.discovery;

/** Compile-time stub. */
public abstract class AbstractDiscoveryService {
    protected abstract void startScan();

    protected void thingDiscovered(final DiscoveryResult discoveryResult) {
    }
}
