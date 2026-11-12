package org.openhab.core.library.types;

import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/** Compile-time stub. */
public enum OnOffType implements State, Command {
    ON,
    OFF;

    public static OnOffType from(boolean state) {
        return state ? ON : OFF;
    }

    public String format(String pattern) { return ""; }
    public String toFullString() { return name(); }
}
