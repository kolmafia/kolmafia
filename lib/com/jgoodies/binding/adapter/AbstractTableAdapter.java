/*
 * Copyright (c) 2002-2007 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.binding.adapter;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;

/**
 * An abstract implementation of the {@link javax.swing.table.TableModel} 
 * interface that converts a {@link javax.swing.ListModel} of row elements.<p>
 * 
 * This class provides default implementations for the <code>TableModel</code> 
 * methods <code>#getColumnCount()</code> and <code>#getColumnName(int)</code>.
 * To use these methods you must use the constructor that accepts an 
 * array of column names and this array must not be <code>null</code>.
 * If a subclass constructs itself with the column names set to <code>null</code>
 * it must override the methods <code>#getColumnCount()</code> and
 * <code>#getColumnName(int)</code>.<p>
 * 
 * <strong>Example:</strong> API users subclass <code>AbstractTableAdapter</code>
 * and just implement the method <code>TableModel#getValueAt(int, int)</code>.<p>
 * 
 * The following example implementation is based on a list of customer rows
 * and exposes the first and last name as well as the customer ages:<pre>
 * public class CustomerTableModel extends AbstractTableAdapter {
 * 
 *     private static final String[] COLUMN_NAMES = 
 *         { "Last Name", "First Name", "Age" };
 * 
 *     public CustomerTableModel(ListModel listModel) {
 *         super(listModel, COLUMN_NAMES);
 *     }
 * 
 *     public Object getValueAt(int rowIndex, int columnIndex) {
 *         Customer customer = (Customer) getRow(rowIndex);
 *         switch (columnIndex) {
 *             case 0 : return customer.getLastName();
 *             case 1 : return customer.getFirstName();
 *             case 2 : return customer.getAge();
 *             default: return null;
 *         }
 *     }
 *            
 * }
 * </pre>
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see javax.swing.ListModel
 * @see javax.swing.JTable
 */
public abstract class AbstractTableAdapter extends AbstractTableModel {

    /**
     * Refers to the <code>ListModel</code> that holds the table row elements
     * and reports changes in the structure and content. The elements of 
     * the list model can be requested using <code>#getRow(int)</code>.
     * A typical subclass will use the elements to implement the 
     * <code>TableModel</code> method <code>#getValueAt(int, int)</code>.
     * 
     * @see #getRow(int)
     * @see #getRowCount()
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    private final ListModel listModel;

    /**
     * Holds an optional array of column names that is used by the
     * default implementation of the <code>TableModel</code> methods
     * <code>#getColumnCount()</code> and <code>#getColumnName(int)</code>.
     * 
     * @see #getColumnCount()
     * @see #getColumnName(int)
     */
    private final String[]  columnNames;


    // Instance Creation ******************************************************

    /**
     * Constructs an AbstractTableAdapter on the given ListModel.
     * Subclasses that use this constructor must override the methods
     * <code>#getColumnCount()</code> and <code>#getColumnName(int)</code>.
     * 
     * @param listModel   the ListModel that holds the row elements
     * @throws NullPointerException if the list model is <code>null</code>
     */
    public AbstractTableAdapter(ListModel listModel) {
        this(listModel, null);
    }


    /**
     * Constructs an AbstractTableAdapter on the given ListModel using
     * the specified table column names. If the column names array is 
     * non-<code>null</code>, it is copied to avoid external mutation.<p>
     *  
     * Subclasses that invoke this constructor with a <code>null</code> column 
     * name array must override the methods <code>#getColumnCount()</code> and 
     * <code>#getColumnName(int)</code>.
     * 
     * @param listModel   the ListModel that holds the row elements
     * @param columnNames an optional array of column names
     * @throws NullPointerException if the list model is <code>null</code>
     */
    public AbstractTableAdapter(ListModel listModel, String[] columnNames) {
        this.listModel = listModel;
        if (listModel == null)
            throw new NullPointerException("The list model must not be null.");
        if (columnNames == null) {
            this.columnNames = null;
        } else {
            this.columnNames = new String[columnNames.length];
            System.arraycopy(columnNames, 0, this.columnNames, 0,
                    columnNames.length);
        }

        listModel.addListDataListener(createChangeHandler());
    }


    // TableModel Implementation **********************************************

    /**
     * Returns the number of columns in the model. A JTable uses 
     * this method to determine how many columns it should create and 
     * display by default.<p>
     * 
     * Subclasses must override this method if they don't provide an 
     * array of column names in the constructor.
     *
     * @return the number of columns in the model
     * @throws NullPointerException  if the optional column names array 
     *     has not been set in the constructor. In this case API users
     *     must override this method.
     * 
     * @see #getColumnName(int)
     * @see #getRowCount()
     */
    public int getColumnCount() {
        return columnNames.length;
    }


    /**
     * Returns the name of the column at the given column index.  
     * This is used to initialize the table's column header name.  
     * Note: this name does not need to be unique; two columns in a table 
     * can have the same name.<p>
     * 
     * Subclasses must override this method if they don't provide an 
     * array of column names in the constructor.
     *
     * @param columnIndex   the index of the column
     * @return  the name of the column
     * @throws NullPointerException  if the optional column names array 
     *     has not been set in the constructor. In this case API users
     *     must override this method.
     * 
     * @see #getColumnCount()
     * @see #getRowCount()
     */
    @Override
public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }


    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * 
     * @see #getRow(int)
     */
    public final int getRowCount() {
        return listModel.getSize();
    }


    // Misc *******************************************************************

    /**
     * Returns the row at the specified row index.
     * 
     * @param index   row index in the underlying list model
     * @return the row at the specified row index. 
     */
    protected final Object getRow(int index) {
        return listModel.getElementAt(index);
    }


    // Event Handling *********************************************************

    /**
     * Creates and returns a listener that handles changes 
     * in the underlying list model.
     * 
     * @return the listener that handles changes in the underlying ListModel
     */
    protected ListDataListener createChangeHandler() {
        return new ListDataChangeHandler();
    }

    /**
     * Listens to subject changes and fires a contents change event.
     */
    private final class ListDataChangeHandler implements ListDataListener {

        /** 
         * Sent after the indices in the index0,index1 
         * interval have been inserted in the data model.
         * The new interval includes both index0 and index1.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void intervalAdded(ListDataEvent evt) {
            fireTableRowsInserted(evt.getIndex0(), evt.getIndex1());
        }


        /**
         * Sent after the indices in the index0,index1 interval
         * have been removed from the data model.  The interval 
         * includes both index0 and index1.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void intervalRemoved(ListDataEvent evt) {
            fireTableRowsDeleted(evt.getIndex0(), evt.getIndex1());
        }


        /** 
         * Sent when the contents of the list has changed in a way 
         * that's too complex to characterize with the previous 
         * methods. For example, this is sent when an item has been
         * replaced. Index0 and index1 bracket the change.
         *
         * @param evt  a <code>ListDataEvent</code> encapsulating the
         *    event information
         */
        public void contentsChanged(ListDataEvent evt) {
            int firstRow = evt.getIndex0();
            int lastRow = evt.getIndex1();
            fireTableRowsUpdated(firstRow, lastRow);
        }

    }

}
