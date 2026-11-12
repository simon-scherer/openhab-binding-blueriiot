package org.openhab.core.library.types;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/** Compile-time stub. */
public class QuantityType<T extends Quantity<T>> extends Number implements State, Command {
    private static final long serialVersionUID = 1L;

    public QuantityType(Number value, Unit<T> unit) {
    }

    public int intValue() { return 0; }
    public long longValue() { return 0; }
    public float floatValue() { return 0; }
    public double doubleValue() { return 0; }
    public String format(String pattern) { return ""; }
    public String toFullString() { return ""; }
}
