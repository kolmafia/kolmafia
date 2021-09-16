/*
 * $Id: DefaultDateSelectionModel.java,v 1.15 2008/10/14 22:31:37 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import org.jdesktop.swingx.util.Contract;

/**
 * 
 * @author Joshua Outwater
 */
public class DefaultDateSelectionModel extends AbstractDateSelectionModel {
    private SelectionMode selectionMode;
    private SortedSet<Date> selectedDates;
    private SortedSet<Date> unselectableDates;

    /**
     * 
     */
    public DefaultDateSelectionModel() {
        this(null);
    }

    /**
     * <p>
     * 
     * The selection mode defaults to SINGLE_SELECTION.
     */
    public DefaultDateSelectionModel(Locale locale) {
        super(locale);
        this.selectionMode = SelectionMode.SINGLE_SELECTION;
        this.selectedDates = new TreeSet<Date>();
        this.unselectableDates = new TreeSet<Date>();
    }
    /**
     * {@inheritDoc}
     */
    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    /**
     * {@inheritDoc}
     */
    public void setSelectionMode(final SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        clearSelection();
    }

    
//------------------- selection ops    
    /**
     * {@inheritDoc}
     */
    public void addSelectionInterval(Date startDate, Date endDate) {
        if (startDate.after(endDate)) {
            return;
        }
        boolean added = false;
        switch (selectionMode) {
            case SINGLE_SELECTION:
                if (isSelected(startDate)) return;
                clearSelectionImpl();
                added = addSelectionImpl(startDate, startDate);
                break;
            case SINGLE_INTERVAL_SELECTION:
                if (isIntervalSelected(startDate, endDate)) return;
                clearSelectionImpl();
                added = addSelectionImpl(startDate, endDate);
                break;
            case MULTIPLE_INTERVAL_SELECTION:
                if (isIntervalSelected(startDate, endDate)) return;
                added = addSelectionImpl(startDate, endDate);
                break;
            default:
                break;
        }
        if (added) {
            fireValueChanged(EventType.DATES_ADDED);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSelectionInterval(final Date startDate, Date endDate) {
        if (SelectionMode.SINGLE_SELECTION.equals(selectionMode)) {
           if (isSelected(startDate)) return;
           endDate = startDate;
        } else {
            if (isIntervalSelected(startDate, endDate)) return;
        }
        clearSelectionImpl();
        if (addSelectionImpl(startDate, endDate)) {
            fireValueChanged(EventType.DATES_SET);
        }
    }

    /**
     * Checks and returns if the single date interval bounded by startDate and endDate
     * is selected. This is useful only for SingleInterval mode.
     * 
     * @param startDate the start of the interval
     * @param endDate the end of the interval, must be >= startDate
     * @return true the interval is selected, false otherwise.
     */
    private boolean isIntervalSelected(Date startDate, Date endDate) {
        if (isSelectionEmpty()) return false;
        return selectedDates.first().equals(startDate) 
           && selectedDates.last().equals(endDate);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionInterval(final Date startDate, final Date endDate) {
        if (startDate.after(endDate)) {
            return;
        }

        long startDateMs = startDate.getTime();
        long endDateMs = endDate.getTime();
        ArrayList<Date> datesToRemove = new ArrayList<Date>();
        for (Date selectedDate : selectedDates) {
            long selectedDateMs = selectedDate.getTime();
            if (selectedDateMs >= startDateMs && selectedDateMs <= endDateMs) {
                datesToRemove.add(selectedDate);
            }
        }

        if (!datesToRemove.isEmpty()) {
            selectedDates.removeAll(datesToRemove);
            fireValueChanged(EventType.DATES_REMOVED);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearSelection() {
        if (isSelectionEmpty()) return;
        clearSelectionImpl();
        fireValueChanged(EventType.SELECTION_CLEARED);
    }

    private void clearSelectionImpl() {
        selectedDates.clear();
    }

    /**
     * {@inheritDoc}
     */
    public SortedSet<Date> getSelection() {
        return new TreeSet<Date>(selectedDates);
    }

    /**
     * {@inheritDoc}
     */
    public Date getFirstSelectionDate() {
        return isSelectionEmpty() ? null : selectedDates.first();
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastSelectionDate() {
        return isSelectionEmpty() ? null : selectedDates.last();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected(final Date date) {
        Contract.asNotNull(date, "date must not be null");
        return selectedDates.contains(date);
    }

    /**
     * {@inheritDoc}
     */
    public Date getNormalizedDate(Date date) {
        return new Date(date.getTime());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelectionEmpty() {
        return selectedDates.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public SortedSet<Date> getUnselectableDates() {
        return new TreeSet<Date>(unselectableDates);
    }

    /**
     * {@inheritDoc}
     */
    public void setUnselectableDates(SortedSet<Date> unselectableDates) {
        this.unselectableDates = unselectableDates;
        for (Date unselectableDate : this.unselectableDates) {
            removeSelectionInterval(unselectableDate, unselectableDate);
        }
        fireValueChanged(EventType.UNSELECTED_DATES_CHANGED);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUnselectableDate(Date date) {
        return upperBound != null && upperBound.getTime() < date.getTime() ||
                lowerBound != null && lowerBound.getTime() > date.getTime() ||
                unselectableDates != null && unselectableDates.contains(date);
    }


    private boolean addSelectionImpl(final Date startDate, final Date endDate) {
        boolean hasAdded = false;
        calendar.setTime(startDate);
        Date date = calendar.getTime();
        while (date.before(endDate) || date.equals(endDate)) {
            if (!isUnselectableDate(date)) {
                hasAdded = true;
                selectedDates.add(date);
            }
            calendar.add(Calendar.DATE, 1);
            date = calendar.getTime();
        }
        return hasAdded;
    }

    
}