package org.openhab.core.library.types;
import org.openhab.core.types.*;
public class DateTimeType implements PrimitiveType, State, Command { public DateTimeType(java.time.ZonedDateTime z){} public DateTimeType(java.time.Instant i){} public String format(String p){return null;} }
