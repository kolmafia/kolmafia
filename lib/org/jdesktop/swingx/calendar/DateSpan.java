/*
 * $Id: DateSpan.java,v 1.3 2005/10/10 18:02:46 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.calendar;

import java.util.Date;

/**
 * An immutable representation of a time range.  The time range is
 * internally represented as two longs. The methods that take and return
 * <code>Date</code>s create the <code>Date</code>s as needed, so that
 * if you modify returned <code>Date</code>s you will <b>not</b> effect
 * the <code>DateSpan</code>.  The end points are inclusive.
 *
 * @version  $Revision: 1.3 $
 */
public class DateSpan {
    private long _start;
    private long _end;

    /**
     * Creates a <code>DateSpan</code> between the two end points.
     *
     * @param start Beginning date
     * @param end Ending date
     * @throws IllegalArgumentException if <code>start</code> is after
     *         <code>end</code>
     */
    public DateSpan(long start, long end) {
        _start = start;
        _end = end;
        if (_start > _end) {
            throw new IllegalArgumentException(
                             "Start date must be before end date");
        }
    }

    /**
     * Creates a <code>DateSpan</code> between the two end points.  This
     * is a conveniance constructor that is equivalent to
     * <code>new Date(start.getTime(), end.getTime());</code>.
     *
     * @param start Beginning date
     * @param end Ending date
     */
    public DateSpan(Date start, Date end) {
        this(start.getTime(), end.getTime());
    }

    /**
     * Returns the start of the date span.
     *
     * @return start of the  span.
     */
    public long getStart() {
        return _start;
    }

    /**
     * Returns the end of the date span.
     *
     * @return end of the span.
     */
    public long getEnd() {
        return _end;
    }

    /**
     * Returns the start of the date span as a <code>Date</code>.
     *
     * @return start of the  span.
     */
    public Date getStartAsDate() {
        return new Date(getStart());
    }

    /**
     * Returns the end of the date span as a <code>Date</code>.
     *
     * @return end of the span.
     */
    public Date getEndAsDate() {
        return new Date(getEnd());
    }

    /**
     * Returns true if this <code>DateSpan</code> contains the specified
     * <code>DateSpan</code>.
     *
     * @param span Date to check
     * @return true if this DateSpan contains <code>span</code>.
     */
    public boolean contains(DateSpan span) {
        return (contains(span.getStart()) && contains(span.getEnd()));
    }

    /**
     * Returns whether or not this <code>DateSpan</code> contains the specified
     * time.
     *
     * @param time time check
     * @return true if this DateSpan contains <code>time</code>.
     */
    public boolean contains(long time) {
        return (time >= getStart() && time <= getEnd());
    }

    /**
     * Returns whether or not this <code>DateSpan</code> contains the
     * specified date span.
     *
     * @param start Start of time span
     * @param end End of time
     * @return true if this <code>DateSpan</code> contains the specified
     *         date span.
     */
    public boolean contains(long start, long end) {
        return (start >= getStart() && end <= getEnd());
    }

    /**
     * Returns true if the this <code>DateSpan</code> intersects with the
     * specified time.
     *
     * @param start Start time
     * @param end End time
     * @return true if this <code>DateSpan</code> intersects with the specified
     * time.
     */
    public boolean intersects(long start, long end) {
        return (start <= getEnd() && end >= getStart());
    }

    /**
     * Returns true if the this <code>DateSpan</code> intersects with the
     * specified <code>DateSpan</code>.
     *
     * @param span DateSpan to compare to
     * @return true if this <code>DateSpan</code> intersects with the specified
     * time.
     */
    public boolean intersects(DateSpan span) {
        return intersects(span.getStart(), span.getEnd());
    }

    /**
     * Returns a new <code>DateSpan</code> that is the union of this
     * <code>DateSpan</code> and <code>span</code>.
     *
     * @param span DateSpan to add
     * @return union of this DateSpan and <code>span</code>
     */
    public DateSpan add(DateSpan span) {
        return add(span.getStart(), span.getEnd());
    }

    /**
     * Returns a new <code>DateSpan</code> that is the union of this
     * <code>DateSpan</code> and the passed in span.
     *
     * @param start Start of region to add
     * @param end End of region to end
     * @return union of this DateSpan and <code>start</code>, <code>end</code>
     */
    public DateSpan add(long start, long end) {
        return new DateSpan(Math.min(start, getStart()),
                            Math.max(end, getEnd()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DateSpan) {
            DateSpan ds = (DateSpan)o;
            return (_start == ds.getStart() && _end == ds.getEnd());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (int)(_start ^ (_start >>> 32));
        result = 37 * result + (int)(_end ^ (_end >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DateSpan [" + getStartAsDate() + "-" + getEndAsDate() + "]";
    }
}
