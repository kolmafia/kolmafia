/*
 * $Id: JXList.java,v 1.83 2009/05/07 09:21:23 kleopatra Exp $
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

package org.jdesktop.swingx;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.DefaultSelectionMapper;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PipelineEvent;
import org.jdesktop.swingx.decorator.PipelineListener;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.renderer.AbstractRenderer;
import org.jdesktop.swingx.renderer.DefaultListRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.rollover.ListRolloverController;
import org.jdesktop.swingx.rollover.ListRolloverProducer;
import org.jdesktop.swingx.rollover.RolloverProducer;
import org.jdesktop.swingx.rollover.RolloverRenderer;
import org.jdesktop.swingx.search.ListSearchable;
import org.jdesktop.swingx.search.SearchFactory;
import org.jdesktop.swingx.search.Searchable;

/**
 * Enhanced List component with support for general SwingX sorting/filtering,
 * rendering, highlighting, rollover and search functionality. List specific
 * enhancements include ?? PENDING JW ...
 * 
 * <h2>Sorting and Filtering</h2>
 * 
 * JXList supports sorting and filtering. 
 * 
 * It provides api to apply a specific sort order, to toggle the sort order and to reset a sort.
 * Sort sequence can be configured by setting a custom comparator.
 * 
 * <pre><code>
 * list.setFilterEnabled(true);
 * list.setComparator(myComparator);
 * list.setSortOrder(SortOrder.DESCENDING);
 * list.toggleSortOder();
 * list.resetSortOrder();
 * </code></pre>
 * 
 * <p>
 * Rows can be filtered from a JXList using a Filter class and a
 * FilterPipeline. One assigns a FilterPipeline to the table using
 * {@link #setFilters(FilterPipeline)}. Filtering hides, but does not delete nor
 * permanently remove rows from a JXList. 
 * 
 * <p>
 * JXList provides api to access items of the underlying model in view coordinates
 * and to convert from/to model coordinates.
 * 
 * <b>Note</b>: List sorting/filtering is disabled by
 * default because it has side-effects which might break "normal" expectations
 * when using a JList: if enabled all row coordinates (including those returned
 * by the selection) are in view coordinates. Furthermore, the model returned
 * from getModel() is a wrapper around the actual data. 
 * 
 * <b>Note:</b> SwingX sorting/filtering is incompatible with core sorting/filtering in 
 * JDK 6+. Will be replaced by core functionality after switching the target jdk
 * version from 5 to 6.
 * 
 * 
 * <h2>Rendering and Highlighting</h2>
 * 
 * As all SwingX collection views, a JXList is a HighlighterClient (PENDING JW:
 * formally define and implement, like in AbstractTestHighlighter), that is it
 * provides consistent api to add and remove Highlighters which can visually
 * decorate the rendering component.
 * <p>
 * 
 * <pre><code>
 * 
 * JXList list = new JXList(new Contributors());
 * // implement a custom string representation, concated from first-, lastName
 * StringValue sv = new StringValue() {
 *     public String getString(Object value) {
 *        if (value instanceof Contributor) {
 *           Contributor contributor = (Contributor) value;
 *           return contributor.lastName() + ", " + contributor.firstName(); 
 *        }
 *        return StringValues.TO_STRING(value);
 *     }
 * };
 * list.setCellRenderer(new DefaultListRenderer(sv); 
 * // highlight condition: gold merits
 * HighlightPredicate predicate = new HighlightPredicate() {
 *    public boolean isHighlighted(Component renderer,
 *                     ComponentAdapter adapter) {
 *       if (!(value instanceof Contributor)) return false;              
 *       return ((Contributor) value).hasGold();
 *    }
 * };
 * // highlight with foreground color 
 * list.addHighlighter(new PainterHighlighter(predicate, goldStarPainter);      
 * 
 * </code></pre>
 * 
 * <i>Note:</i> to support the highlighting this implementation wraps the
 * ListCellRenderer set by client code with a DelegatingRenderer which applies
 * the Highlighter after delegating the default configuration to the wrappee. As
 * a side-effect, getCellRenderer does return the wrapper instead of the custom
 * renderer. To access the latter, client code must call getWrappedCellRenderer.
 * <p>
 * 
 * <h2>Rollover</h2>
 * 
 * As all SwingX collection views, a JXList supports per-cell rollover. If
 * enabled, the component fires rollover events on enter/exit of a cell which by
 * default is promoted to the renderer if it implements RolloverRenderer, that
 * is simulates live behaviour. The rollover events can be used by client code
 * as well, f.i. to decorate the rollover row using a Highlighter.
 * 
 * <pre><code>
 * 
 * JXList list = new JXList();
 * list.setRolloverEnabled(true);
 * list.setCellRenderer(new DefaultListRenderer());
 * list.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, 
 *      null, Color.RED);      
 * 
 * </code></pre>
 * 
 * 
 * <h2>Search</h2>
 * 
 * As all SwingX collection views, a JXList is searchable. A search action is
 * registered in its ActionMap under the key "find". The default behaviour is to
 * ask the SearchFactory to open a search component on this component. The
 * default keybinding is retrieved from the SearchFactory, typically ctrl-f (or
 * cmd-f for Mac). Client code can register custom actions and/or bindings as
 * appropriate.
 * <p>
 * 
 * JXList provides api to vend a renderer-controlled String representation of
 * cell content. This allows the Searchable and Highlighters to use WYSIWYM
 * (What-You-See-Is-What-You-Match), that is pattern matching against the actual
 * string as seen by the user.
 * 
 * 
 * @author Ramesh Gupta
 * @author Jeanette Winzenburg
 */
public class JXList extends JList {
    @SuppressWarnings("all")
    private static final Logger LOG = Logger.getLogger(JXList.class.getName());
    public static final String EXECUTE_BUTTON_ACTIONCOMMAND = "executeButtonAction";

    /** The pipeline holding the filters. */
    protected FilterPipeline filters;

    /**
     * The pipeline holding the highlighters.
     */
    protected CompoundHighlighter compoundHighlighter;

    /** listening to changeEvents from compoundHighlighter. */
    private ChangeListener highlighterChangeListener;

    /** The ComponentAdapter for model data access. */
    protected ComponentAdapter dataAdapter;

    /**
     * Mouse/Motion/Listener keeping track of mouse moved in cell coordinates.
     */
    private RolloverProducer rolloverProducer;

    /**
     * RolloverController: listens to cell over events and repaints
     * entered/exited rows.
     */
    private ListRolloverController<JXList> linkController;

    /** A wrapper around the default renderer enabling decoration. */
    private DelegatingRenderer delegatingRenderer;

    private WrappingListModel wrappingModel;

    private PipelineListener pipelineListener;

    private boolean filterEnabled;

    private SelectionMapper selectionMapper;

    private Searchable searchable;

    private Comparator<?> comparator;

    /**
    * Constructs a <code>JXList</code> with an empty model and filters disabled.
    *
    */                                           
    public JXList() {
        this(false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in the
     * specified, non-<code>null</code> model and filters disabled.
     *
     * @param dataModel   the data model for this list
     * @exception IllegalArgumentException   if <code>dataModel</code>
     *                                           is <code>null</code>
     */                                           
    public JXList(ListModel dataModel) {
        this(dataModel, false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified array and filters disabled.
     *
     * @param  listData  the array of Objects to be loaded into the data model
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Object[] listData) {
        this(listData, false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified <code>Vector</code> and filtes disabled.
     *
     * @param  listData  the <code>Vector</code> to be loaded into the
     *          data model
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Vector<?> listData) {
        this(listData, false);
    }


    /**
     * Constructs a <code>JXList</code> with an empty model and
     * filterEnabled property.
     * 
     * @param filterEnabled <code>boolean</code> to determine if 
     *  filtering/sorting is enabled
     */
    public JXList(boolean filterEnabled) {
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> with the specified model and
     * filterEnabled property.
     * 
     * @param dataModel   the data model for this list
     * @param filterEnabled <code>boolean</code> to determine if 
     *          filtering/sorting is enabled
     * @throws IllegalArgumentException   if <code>dataModel</code>
     *                                          is <code>null</code>
     */
    public JXList(ListModel dataModel, boolean filterEnabled) {
        super(dataModel);
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified array and filterEnabled property.
     *
     * @param  listData  the array of Objects to be loaded into the data model
     * @param filterEnabled <code>boolean</code> to determine if filtering/sorting
     *   is enabled
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Object[] listData, boolean filterEnabled) {
        super(listData);
        if (listData == null) 
           throw new IllegalArgumentException("listData must not be null");
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified <code>Vector</code> and filtersEnabled property.
     *
     * @param  listData  the <code>Vector</code> to be loaded into the
     *          data model
     * @param filterEnabled <code>boolean</code> to determine if filtering/sorting
     *   is enabled
     * @throws IllegalArgumentException if <code>listData</code> is <code>null</code>
     */
    public JXList(Vector<?> listData, boolean filterEnabled) {
        super(listData);
        if (listData == null) 
           throw new IllegalArgumentException("listData must not be null");
        init(filterEnabled);
    }


    private void init(boolean filterEnabled) {
        setFilterEnabled(filterEnabled);
        
        Action findAction = createFindAction();
        getActionMap().put("find", findAction);
        
        KeyStroke findStroke = SearchFactory.getInstance().getSearchAccelerator();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(findStroke, "find");
        
    }

    private Action createFindAction() {
        return new UIAction("find") {
            public void actionPerformed(ActionEvent e) {
                doFind();
            }
        };
    }

    /** 
     * Starts a search on this List's visible items. This implementation asks the
     * SearchFactory to open a find widget on itself.
     */
    protected void doFind() {
        SearchFactory.getInstance().showFindInput(this, getSearchable());
    }

    /**
     * Returns a Searchable for this component, guaranteed to be not null. This 
     * implementation lazily creates a ListSearchable if necessary.
     *  
     * @return a not-null Searchable for this list.
     * 
     * @see #setSearchable(Searchable)
     * @see org.jdesktop.swingx.search.ListSearchable
     */
    public Searchable getSearchable() {
        if (searchable == null) {
            searchable = new ListSearchable(this);
        }
        return searchable;
    }

    /**
     * Sets the Searchable for this component. If null, a default 
     * Searchable will be created and used.
     * 
     * @param searchable the Searchable to use for this component, may be null to indicate
     *   using the list's default searchable.
     * @see #getSearchable()
     */
    public void setSearchable(Searchable searchable) {
        this.searchable = searchable;
    }
    
//--------------------- Rollover support
    
    /**
     * Sets the property to enable/disable rollover support. If enabled, the list
     * fires property changes on per-cell mouse rollover state, i.e. 
     * when the mouse enters/leaves a list cell. <p>
     * 
     * This can be enabled to show "live" rollover behaviour, f.i. the cursor over a cell 
     * rendered by a JXHyperlink.<p>
     * 
     * Default value is disabled.
     * 
     * @param rolloverEnabled a boolean indicating whether or not the rollover
     *   functionality should be enabled.
     * 
     * @see #isRolloverEnabled()
     * @see #getLinkController()
     * @see #createRolloverProducer()
     * @see org.jdesktop.swingx.rollover.RolloverRenderer  
     *    
     */
    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = isRolloverEnabled();
        if (rolloverEnabled == old)
            return;
        if (rolloverEnabled) {
            rolloverProducer = createRolloverProducer();
            addMouseListener(rolloverProducer);
            addMouseMotionListener(rolloverProducer);
            getLinkController().install(this);
        } else {
            removeMouseListener(rolloverProducer);
            removeMouseMotionListener(rolloverProducer);
            rolloverProducer = null;
            getLinkController().release();
        }
        firePropertyChange("rolloverEnabled", old, isRolloverEnabled());
    }

    /**
     * Returns a boolean indicating whether or not rollover support is enabled. 
     *
     * @return a boolean indicating whether or not rollover support is enabled. 
     * 
     * @see #setRolloverEnabled(boolean)
     */
    public boolean isRolloverEnabled() {
        return rolloverProducer != null;
    }
    
    /**
     * Returns the RolloverController for this component. Lazyly creates the 
     * controller if necessary, that is the return value is guaranteed to be 
     * not null. <p>
     * 
     * PENDING JW: rename to getRolloverController
     * 
     * @return the RolloverController for this tree, guaranteed to be not null.
     * 
     * @see #setRolloverEnabled(boolean)
     * @see #createLinkController()
     * @see org.jdesktop.swingx.rollover.RolloverController
     */
    protected ListRolloverController<JXList> getLinkController() {
        if (linkController == null) {
            linkController = createLinkController();
        }
        return linkController;
    }

    /**
     * Creates and returns a RolloverController appropriate for this component.
     * 
     * @return a RolloverController appropriate for this component.
     * 
     * @see #getLinkController()
     * @see org.jdesktop.swingx.rollover.RolloverController
     */
    protected ListRolloverController<JXList> createLinkController() {
        return new ListRolloverController<JXList>();
    }


    /**
     * Creates and returns the RolloverProducer to use with this tree.
     * <p>
     * 
     * @return <code>RolloverProducer</code> to use with this tree
     * 
     * @see #setRolloverEnabled(boolean)
     */
    protected RolloverProducer createRolloverProducer() {
        return new ListRolloverProducer();
    }

    //--------------------- public sort api
//    /** 
//     * Returns the sortable property.
//     * Here: same as filterEnabled.
//     * @return true if the table is sortable. 
//     */
//    public boolean isSortable() {
//        return isFilterEnabled();
//    }
    /**
     * Removes the interactive sorter.
     * 
     */
    public void resetSortOrder() {
        SortController controller = getSortController();
        if (controller != null) {
            controller.setSortKeys(null);
        }
    }

    /**
     * 
     * Toggles the sort order of the items.
     * <p>
     * The exact behaviour is defined by the SortController's
     * toggleSortOrder implementation. Typically a unsorted 
     * column is sorted in ascending order, a sorted column's
     * order is reversed. 
     * <p>
     * PENDING: where to get the comparator from?
     * <p>
     * 
     * 
     */
    public void toggleSortOrder() {
        SortController controller = getSortController();
        if (controller != null) {
            controller.toggleSortOrder(0, getComparator());
        }
    }

    /**
     * Sorts the list using SortOrder. 
     * 
     * 
     * Respects the JXList's sortable and comparator 
     * properties: routes the comparator to the SortController
     * and does nothing if !isFilterEnabled(). 
     * <p>
     * 
     * @param sortOrder the sort order to use. If null or SortOrder.UNSORTED, 
     *   this method has the same effect as resetSortOrder();
     *    
     */
    public void setSortOrder(SortOrder sortOrder) {
        if ((sortOrder == null) || !sortOrder.isSorted()) {
            resetSortOrder();
            return;
        }
        SortController sortController = getSortController();
        if (sortController != null) {
            SortKey sortKey = new SortKey(sortOrder, 
                    0, getComparator());    
            sortController.setSortKeys(Collections.singletonList(sortKey));
        }
    }


    /**
     * Returns the SortOrder. 
     * 
     * @return the interactive sorter's SortOrder  
     *  or SortOrder.UNSORTED 
     */
    public SortOrder getSortOrder() {
        SortController sortController = getSortController();
        if (sortController == null) return SortOrder.UNSORTED;
        SortKey sortKey = SortKey.getFirstSortKeyForColumn(sortController.getSortKeys(), 
                0);
        return sortKey != null ? sortKey.getSortOrder() : SortOrder.UNSORTED;
    }

    /**
     * 
     * @return the comparator used.
     * @see #setComparator(Comparator)
     */
    public Comparator<?> getComparator() {
        return comparator;
    }
    
    /**
     * Sets the comparator used. As a side-effect, the 
     * current sort might be updated. The exact behaviour
     * is defined in #updateSortAfterComparatorChange. 
     * 
     * @param comparator the comparator to use.
     */
    public void setComparator(Comparator<?> comparator) {
        Comparator<?> old = getComparator();
        this.comparator = comparator;
        updateSortAfterComparatorChange();
        firePropertyChange("comparator", old, getComparator());
    }
    
    /**
     * Updates sort after comparator has changed. 
     * Here: sets the current sortOrder with the new comparator.
     *
     */
    protected void updateSortAfterComparatorChange() {
        setSortOrder(getSortOrder());
        
    }

    /**
     * returns the currently active SortController. Will be null if
     * !isFilterEnabled().
     * @return the currently active <code>SortController</code> may be null
     */
    protected SortController getSortController() {
//      // this check is for the sake of the very first call after instantiation
        // doesn't apply for JXList? need to test for filterEnabled?
        //if (filters == null) return null;
        if (!isFilterEnabled()) return null;
        return getFilters().getSortController();
    }
    
    
    // ---------------------------- filters

    /**
     * returns the element at the given index. The index is in view coordinates
     * which might differ from model coordinates if filtering is enabled and
     * filters/sorters are active.
     * 
     * @param viewIndex the index in view coordinates
     * @return the element at the index
     * @throws IndexOutOfBoundsException if viewIndex < 0 or viewIndex >=
     *         getElementCount()
     */
    public Object getElementAt(int viewIndex) {
        return getModel().getElementAt(viewIndex);
    }

    /**
     * Returns the number of elements in this list in view 
     * coordinates. If filters are active this number might be
     * less than the number of elements in the underlying model.
     * 
     * @return number of elements in this list in view coordinates
     */
    public int getElementCount() {
        return getModel().getSize();
    }

    /**
     * Convert row index from view coordinates to model coordinates accounting
     * for the presence of sorters and filters.
     * 
     * @param viewIndex index in view coordinates
     * @return index in model coordinates
     * @throws IndexOutOfBoundsException if viewIndex < 0 or viewIndex >= getElementCount() 
     */
    public int convertIndexToModel(int viewIndex) {
        return isFilterEnabled() ? getFilters().convertRowIndexToModel(
                viewIndex) : viewIndex;
    }

    /**
     * Convert index from model coordinates to view coordinates accounting
     * for the presence of sorters and filters.
     * 
     * PENDING Filter guards against out of range - should not? 
     * 
     * @param modelIndex index in model coordinates
     * @return index in view coordinates if the model index maps to a view coordinate
     *          or -1 if not contained in the view.
     * 
     */
    public int convertIndexToView(int modelIndex) {
        return isFilterEnabled() ? getFilters().convertRowIndexToView(
                modelIndex) : modelIndex;
    }

    /**
     * returns the underlying model. If !isFilterEnabled this will be the same
     * as getModel().
     * 
     * @return the underlying model
     */
    public ListModel getWrappedModel() {
        return isFilterEnabled() ? wrappingModel.getModel() : getModel();
    }

    /**
     * Enables/disables filtering support. If enabled all row indices -
     * including the selection - are in view coordinates and getModel returns a
     * wrapper around the underlying model.
     * 
     * Note: as an implementation side-effect calling this method clears the
     * selection (done in super.setModel).
     * 
     * PENDING: cleanup state transitions!! - currently this can be safely
     * applied once only to enable. Internal state is inconsistent if trying to
     * disable again. As a temporary emergency measure, this will throw a 
     * IllegalStateException. 
     * 
     * see Issue #2-swinglabs.
     * 
     * @param enabled
     * @throws IllegalStateException if trying to disable again.
     */
    public void setFilterEnabled(boolean enabled) {
        boolean old = isFilterEnabled();
        if (old == enabled)
            return;
        if (old) 
            throw new IllegalStateException("must not reset filterEnabled");
        // JW: filterEnabled must be set before calling super.setModel!
        filterEnabled = enabled;
        wrappingModel = new WrappingListModel(getModel());
        super.setModel(wrappingModel);
        firePropertyChange("filterEnabled", old, isFilterEnabled());
    }

    /**
     * 
     * @return a <boolean> indicating if filtering is enabled.
     * @see #setFilterEnabled(boolean)
     */
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update selectionMapper
     */
    @Override 
    public void setSelectionModel(ListSelectionModel newModel) {
        super.setSelectionModel(newModel);
        getSelectionMapper().setViewSelectionModel(getSelectionModel());
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Sets the underlying data model. Note that if isFilterEnabled you must
     * call getWrappedModel to access the model given here. In this case
     * getModel returns a wrapper around the data!
     * 
     * @param model the data model for this list.
     * 
     */
    @Override
    public void setModel(ListModel model) {
        if (isFilterEnabled()) {
            wrappingModel.setModel(model);
        } else {
            super.setModel(model);
        }
    }

    /**
     * widened access for testing...
     * @return the selection mapper
     */
    protected SelectionMapper getSelectionMapper() {
        if (selectionMapper == null) {
            selectionMapper = new DefaultSelectionMapper(filters, getSelectionModel());
        }
        return selectionMapper;
    }

    /**
     * Returns the FilterPipeline assigned to this list, or null if filtering not
     * enabled.
     * 
     * @return the <code>FilterPipeline</code> assigned to this list, or
     *   null if !isFiltersEnabled().
     */
    public FilterPipeline getFilters() {
        if ((filters == null) && isFilterEnabled()) {
            setFilters(null);
        }
        return filters;
    }

    /** Sets the FilterPipeline for filtering the items of this list, maybe null
     *  to remove all previously applied filters. 
     *  
     *  Note: the current "interactive" sortState is preserved (by 
     *  internally copying the old sortKeys to the new pipeline, if any). 
     *  
     *  PRE: isFilterEnabled()
     * 
     * @param pipeline the <code>FilterPipeline</code> to use, null removes
     *   all filters.
     * @throws IllegalStateException if !isFilterEnabled()
     */
    public void setFilters(FilterPipeline pipeline) {
        if (!isFilterEnabled()) throw
            new IllegalStateException("filters not enabled - not allowed to set filters");

        FilterPipeline old = filters;
        List<? extends SortKey> sortKeys = null;
        if (old != null) {
            old.removePipelineListener(pipelineListener);
            sortKeys = old.getSortController().getSortKeys();
        }
        if (pipeline == null) {
            pipeline = new FilterPipeline();
        }
        filters = pipeline;
        filters.getSortController().setSortKeys(sortKeys);
        // JW: first assign to prevent (short?) illegal internal state
        // #173-swingx
        use(filters);
        getSelectionMapper().setFilters(filters);

    }

    /**
     * setModel() and setFilters() may be called in either order.
     * 
     * @param pipeline
     */
    private void use(FilterPipeline pipeline) {
        if (pipeline != null) {
            // check JW: adding listener multiple times (after setModel)?
            if (initialUse(pipeline)) {
                pipeline.addPipelineListener(getFilterPipelineListener());
                pipeline.assign(getComponentAdapter());
            } else {
                pipeline.flush();
            }
        }
    }

    /**
     * @return true is not yet used in this JXTable, false otherwise
     */
    private boolean initialUse(FilterPipeline pipeline) {
        if (pipelineListener == null)
            return true;
        PipelineListener[] l = pipeline.getPipelineListeners();
        for (int i = 0; i < l.length; i++) {
            if (pipelineListener.equals(l[i]))
                return false;
        }
        return true;
    }

    /** returns the listener for changes in filters. */
    protected PipelineListener getFilterPipelineListener() {
        if (pipelineListener == null) {
            pipelineListener = createPipelineListener();
        }
        return pipelineListener;
    }

    /** creates the listener for changes in filters. */
    protected PipelineListener createPipelineListener() {
        return new PipelineListener() {
            public void contentsChanged(PipelineEvent e) {
                updateOnFilterContentChanged();
            }
        };
    }

    /**
     * method called on change notification from filterpipeline.
     */
    protected void updateOnFilterContentChanged() {
        // make the wrapper listen to the pipeline?
        if (wrappingModel != null) {
            wrappingModel.updateOnFilterContentChanged();
        }
        revalidate();
        repaint();
    }

    private class WrappingListModel extends AbstractListModel {

        private ListModel delegate;

        private ListDataListener listDataListener;
        private Point OUTSIDE = new Point(-1, -1);
        protected boolean ignoreFilterContentChanged;

        public WrappingListModel(ListModel model) {
            setModel(model);
        }

        public void updateOnFilterContentChanged() {
            if (ignoreFilterContentChanged) return;
            fireContentsChanged(this, -1, -1);

        }

        public void setModel(ListModel model) {
            ListModel old = this.getModel();
            if (old != null) {
                old.removeListDataListener(listDataListener);
            }
            this.delegate = model;
            delegate.addListDataListener(getListDataListener());
            // sequence of method calls? 
            // fire contentsChanged after internal cleanup?
            fireContentsChanged(this, -1, -1);
            // fix #477-swingx
            getSelectionMapper().clearModelSelection();
            getFilters().flush();
        }

        private ListDataListener getListDataListener() {
            if (listDataListener == null) {
                listDataListener = createListDataListener();
            }
            return listDataListener;
        }

        private ListDataListener createListDataListener() {
            return new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    boolean wasEnabled = getSelectionMapper().isEnabled();
                    getSelectionMapper().setEnabled(false);
                    try {
                        updateModelSelection(e);
                        ignoreFilterContentChanged = true;
                        getFilters().flush();
                        ignoreFilterContentChanged = false;
                        // do the mapping after the flush and refire
                        refireMappedEvent(getMappedEvent(e));
                    } finally {
                        // for mutations, super and UI must be done with updating their internals
                        // before it's safe to synch the view selection
                        getSelectionMapper().setEnabled(wasEnabled);
                    }
                }

                public void intervalRemoved(ListDataEvent e) {
                    boolean wasEnabled = getSelectionMapper().isEnabled();
                    getSelectionMapper().setEnabled(false);
                    try {
                        updateModelSelection(e);
                        // do the mapping before flushing
                        // otherwise we may get indexOOBs
                        ListDataEvent mappedEvent = getMappedEvent(e);
                        ignoreFilterContentChanged = true;
                        getFilters().flush();
                        ignoreFilterContentChanged = false;
                        refireMappedEvent(mappedEvent);
                    } finally {
                        // for mutations, super and UI must be done with updating their internals
                        // before it's safe to synch the view selection
                        getSelectionMapper().setEnabled(wasEnabled);
                    }
                }

                public void contentsChanged(ListDataEvent e) {
                    updateInternals(e);
                    refireContentsChanged(e);
                }
            };
        }

        /**
         * Refires the received event. Tries its best to map to the new
         * coordinates. At this point, the internals (selection, filter) are
         * updated, so it's safe to use the conversion methods.
         * 
         * @param e the ListDataEvent received from the wrapped model.
         */
        private void refireContentsChanged(ListDataEvent e) {
            // quick check for single item removal
            if ((e.getIndex0() >= 0) 
                && (e.getIndex0() == e.getIndex1())) {
                // single outside - no notification
                int viewIndex = convertIndexToView(e.getIndex0());
                if (viewIndex == -1) return;
                fireContentsChanged(this, viewIndex, viewIndex);
            } else if (e.getIndex0() >= 0) {
                // PENDING JW: narrow the interval bounds
                fireContentsChanged(this, 0, getSize());
            } else {
                fireContentsChanged(this, -1, -1);
            }
        }

        /**
         * @param mappedEvent
         */
        protected void refireMappedEvent(ListDataEvent mappedEvent) {
            if (mappedEvent == null) return;
            if (mappedEvent.getType() == ListDataEvent.INTERVAL_REMOVED) {
                fireIntervalRemoved(this, mappedEvent.getIndex0(), mappedEvent.getIndex1());
            } else if (mappedEvent.getType() == ListDataEvent.INTERVAL_ADDED) {
                fireIntervalAdded(this, mappedEvent.getIndex0(), mappedEvent.getIndex1());
            } else {
                fireContentsChanged(this, mappedEvent.getIndex0(), mappedEvent.getIndex1());
            }
        }


        private ListDataEvent getMappedEvent(ListDataEvent e) {
            // quick check for single item removal
            if ((e.getIndex0() != - 1) 
                && (e.getIndex0() == e.getIndex1())) {
                int viewIndex = convertIndexToView(e.getIndex0());
                // single outside - no notification
                if (viewIndex == -1) return null;
                return new ListDataEvent(this, e.getType(), viewIndex, viewIndex);
            }
            Point mappedRange = getContinousMappedRange(e);
            if (mappedRange == null) {
             // cant help - no support for discontiouns interval remove notification
                return new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
            } else if (OUTSIDE == mappedRange) {
                return null;
                // do nothing, everything is outside
            }
            // could map to a continous interval
            return new ListDataEvent(this, e.getType(), mappedRange.x, mappedRange.y);
        }


        
        
        protected Point getContinousMappedRange(ListDataEvent e) {
            List<Integer> mapped = new ArrayList<Integer>();
            for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                int viewIndex = convertIndexToView(i);
                if (viewIndex >= 0) {
                    mapped.add(viewIndex);
                }
            }
            if (mapped.size() == 0) return OUTSIDE;
            if (mapped.size() == 1) return new Point(mapped.get(0), mapped.get(0));
            Collections.sort(mapped);
            for (int i = 0; i < mapped.size() - 2; i++) {
                if (mapped.get(i+1) - mapped.get(i) != 1) return null;
            }
            return new Point(mapped.get(0), mapped.get(mapped.size() - 1));
        }

        private void updateInternals(ListDataEvent e) {
            boolean wasEnabled = getSelectionMapper().isEnabled();
            getSelectionMapper().setEnabled(false);
            try {
                updateModelSelection(e);
            } finally {
                getSelectionMapper().setEnabled(wasEnabled);
            }
            ignoreFilterContentChanged = true;
            getFilters().flush();
            ignoreFilterContentChanged = false;
        }

        /**
         * Adjusts the model coordinates of the selection as appropriate
         * for the given event.
         * 
         * @param e the ListDataEvent to adjust from.
         */
        protected void updateModelSelection(ListDataEvent e) {
            if (e.getType() == ListDataEvent.INTERVAL_REMOVED) {
                getSelectionMapper()
                        .removeIndexInterval(e.getIndex0(), e.getIndex1());
            } else if (e.getType() == ListDataEvent.INTERVAL_ADDED) {

                int minIndex = Math.min(e.getIndex0(), e.getIndex1());
                int maxIndex = Math.max(e.getIndex0(), e.getIndex1());
                int length = maxIndex - minIndex + 1;
                getSelectionMapper().insertIndexInterval(minIndex, length, true);
            } else if (e.getIndex0() == -1) {
                getSelectionMapper().clearModelSelection();
            }

        }

        public ListModel getModel() {
            return delegate;
        }

        public int getSize() {
            return getFilters().getOutputSize();
        }

        public Object getElementAt(int index) {
            return getFilters().getValueAt(index, 0);
        }



    }

    // ---------------------------- uniform data model

    /**
     * @return the unconfigured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new ListAdapter(this);
        }
        return dataAdapter;
    }

    /**
     * Convenience to access a configured ComponentAdapter.
     * Note: the column index of the configured adapter is always 0.
     * 
     * @param index the row index in view coordinates, must be valid.
     * @return the configured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter(int index) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.column = 0;
        adapter.row = index;
        return adapter;
    }
    
    /**
     * A component adapter targeted at a JXList.
     */
    protected static class ListAdapter extends ComponentAdapter {
        private final JXList list;

        /**
         * Constructs a <code>ListAdapter</code> for the specified target
         * JXList.
         * 
         * @param component  the target list.
         */
        public ListAdapter(JXList component) {
            super(component);
            list = component;
        }

        /**
         * Typesafe accessor for the target component.
         * 
         * @return the target component as a {@link org.jdesktop.swingx.JXList}
         */
        public JXList getList() {
            return list;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasFocus() {
            /** TODO: Think through printing implications */
            return list.isFocusOwner() && (row == list.getLeadSelectionIndex());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return list.getWrappedModel().getSize();
        }

        /**
         * {@inheritDoc} <p>
         * Overridden to return value at implicit view coordinates.
         */
        @Override
        public Object getValue() {
            return list.getElementAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int row, int column) {
            return list.getWrappedModel().getElementAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getFilteredValueAt(int row, int column) {
            return list.getElementAt(row);
        }

        
        /**
         * {@inheritDoc}
         */
        @Override
        public String getFilteredStringAt(int row, int column) {
            return list.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString() {
            return list.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getStringAt(int row, int column) {
            // PENDING JW: here we are duplicating code from the list
            // that's because list api is in view-coordinates
            ListCellRenderer renderer = list.getDelegatingRenderer().getDelegateRenderer();
            if (renderer instanceof StringValue) {
                return ((StringValue) renderer).getString(getValueAt(row, column));
            }
            return StringValues.TO_STRING.getString(getValueAt(row, column));
        }
        
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(Object aValue, int row, int column) {
            throw new UnsupportedOperationException(
                    "The model is immutable.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEditable() {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected() {
            /** TODO: Think through printing implications */
            return list.isSelectedIndex(row);
        }

    }

    // ------------------------------ renderers


    
    /**
     * Sets the <code>Highlighter</code>s to the table, replacing any old settings.
     * None of the given Highlighters must be null.<p>
     * 
     * This is a bound property. <p> 
     * 
     * Note: as of version #1.257 the null constraint is enforced strictly. To remove
     * all highlighters use this method without param.
     * 
     * @param highlighters zero or more not null highlighters to use for renderer decoration.
     * @throws NullPointerException if array is null or array contains null values.
     * 
     * @see #getHighlighters()
     * @see #addHighlighter(Highlighter)
     * @see #removeHighlighter(Highlighter)
     * 
     */
    public void setHighlighters(Highlighter... highlighters) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().setHighlighters(highlighters);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    /**
     * Returns the <code>Highlighter</code>s used by this table.
     * Maybe empty, but guarantees to be never null.
     * 
     * @return the Highlighters used by this table, guaranteed to never null.
     * @see #setHighlighters(Highlighter[])
     */
    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter().getHighlighters();
    }
    /**
     * Appends a <code>Highlighter</code> to the end of the list of used
     * <code>Highlighter</code>s. The argument must not be null. 
     * <p>
     * 
     * @param highlighter the <code>Highlighter</code> to add, must not be null.
     * @throws NullPointerException if <code>Highlighter</code> is null.
     * 
     * @see #removeHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter[])
     */
    public void addHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().addHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    /**
     * Removes the given Highlighter. <p>
     * 
     * Does nothing if the Highlighter is not contained.
     * 
     * @param highlighter the Highlighter to remove.
     * @see #addHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter...)
     */
    public void removeHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().removeHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }
    
    /**
     * Returns the CompoundHighlighter assigned to the table, null if none.
     * PENDING: open up for subclasses again?.
     * 
     * @return the CompoundHighlighter assigned to the table.
     */
    protected CompoundHighlighter getCompoundHighlighter() {
        if (compoundHighlighter == null) {
            compoundHighlighter = new CompoundHighlighter();
            compoundHighlighter.addChangeListener(getHighlighterChangeListener());
        }
        return compoundHighlighter;
    }

    /**
     * Returns the <code>ChangeListener</code> to use with highlighters. Lazily 
     * creates the listener.
     * 
     * @return the ChangeListener for observing changes of highlighters, 
     *   guaranteed to be <code>not-null</code>
     */
    protected ChangeListener getHighlighterChangeListener() {
        if (highlighterChangeListener == null) {
            highlighterChangeListener = createHighlighterChangeListener();
        }
        return highlighterChangeListener;
    }

    /**
     * Creates and returns the ChangeListener observing Highlighters.
     * <p>
     * Here: repaints the table on receiving a stateChanged.
     * 
     * @return the ChangeListener defining the reaction to changes of
     *         highlighters.
     */
    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        };
    }

    
    /**
     * Returns the string representation of the cell value at the given position. 
     * 
     * @param row the row index of the cell in view coordinates
     * @return the string representation of the cell value as it will appear in the 
     *   table. 
     */
    public String getStringAt(int row) {
        ListCellRenderer renderer = getDelegatingRenderer().getDelegateRenderer();
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(getElementAt(row));
        }
        return StringValues.TO_STRING.getString(getElementAt(row));
    }

    private DelegatingRenderer getDelegatingRenderer() {
        if (delegatingRenderer == null) {
            // only called once... to get hold of the default?
            delegatingRenderer = new DelegatingRenderer();
        }
        return delegatingRenderer;
    }

    /**
     * Creates and returns the default cell renderer to use. Subclasses
     * may override to use a different type. Here: returns a <code>DefaultListRenderer</code>.
     * 
     * @return the default cell renderer to use with this list.
     */
    protected ListCellRenderer createDefaultCellRenderer() {
        return new DefaultListRenderer();
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to return the delegating renderer which is wrapped around the
     * original to support highlighting. The returned renderer is of type 
     * DelegatingRenderer and guaranteed to not-null<p>
     * 
     * @see #setCellRenderer(ListCellRenderer)
     * @see DelegatingRenderer
     */
    @Override
    public ListCellRenderer getCellRenderer() {
        return getDelegatingRenderer();
    }

    /**
     * Returns the renderer installed by client code or the default if none has
     * been set.
     * 
     * @return the wrapped renderer.
     * @see #setCellRenderer(ListCellRenderer)
     */
    public ListCellRenderer getWrappedCellRenderer() {
        return getDelegatingRenderer().getDelegateRenderer();
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to wrap the given renderer in a DelegatingRenderer to support
     * highlighting. <p>
     * 
     * Note: the wrapping implies that the renderer returned from the getCellRenderer
     * is <b>not</b> the renderer as given here, but the wrapper. To access the original,
     * use <code>getWrappedCellRenderer</code>.
     * 
     * @see #getWrappedCellRenderer()
     * @see #getCellRenderer()
     * 
     */
    @Override
    public void setCellRenderer(ListCellRenderer renderer) {
        // JW: Pending - probably fires propertyChangeEvent with wrong newValue?
        // how about fixedCellWidths?
        // need to test!!
        getDelegatingRenderer().setDelegateRenderer(renderer);
        super.setCellRenderer(delegatingRenderer);
    }

    /**
     * A decorator for the original ListCellRenderer. Needed to hook highlighters
     * after messaging the delegate.<p>
     * 
     * PENDING JW: formally implement UIDependent?
     */
    public class DelegatingRenderer implements ListCellRenderer, RolloverRenderer {
        /** the delegate. */
        private ListCellRenderer delegateRenderer;

        /**
         * Instantiates a DelegatingRenderer with list's default renderer as delegate.
         */
        public DelegatingRenderer() {
            this(null);
        }
        
        /**
         * Instantiates a DelegatingRenderer with the given delegate. If the
         * delegate is null, the default is created via the list's factory method.
         * 
         * @param delegate the delegate to use, if null the list's default is
         *   created and used.
         */
        public DelegatingRenderer(ListCellRenderer delegate) {
            setDelegateRenderer(delegate);
        }

        /**
         * Sets the delegate. If the
         * delegate is null, the default is created via the list's factory method.
         * 
         * @param delegate the delegate to use, if null the list's default is
         *   created and used.
         */
        public void setDelegateRenderer(ListCellRenderer delegate) {
            if (delegate == null) {
                delegate = createDefaultCellRenderer();
            }
            delegateRenderer = delegate;
        }

        /**
         * Returns the delegate.
         * 
         * @return the delegate renderer used by this renderer, guaranteed to
         *   not-null.
         */
        public ListCellRenderer getDelegateRenderer() {
            return delegateRenderer;
        }

        /**
         * Updates the ui of the delegate.
         */
         public void updateUI() {
             updateRendererUI(delegateRenderer);
         }

         /**
          * 
          * @param renderer the renderer to update the ui of.
          */
         private void updateRendererUI(ListCellRenderer renderer) {
             if (renderer == null) return;
             Component comp = null;
             if (renderer instanceof AbstractRenderer) {
                 comp = ((AbstractRenderer) renderer).getComponentProvider().getRendererComponent(null);
             } else if (renderer instanceof Component) {
                 comp = (Component) renderer;
             } else {
                 try {
                     comp = renderer.getListCellRendererComponent(
                             JXList.this, null, -1, false, false);
                } catch (Exception e) {
                    // nothing to do - renderer barked on off-range row
                }
             }
             if (comp != null) {
                 SwingUtilities.updateComponentTreeUI(comp);
             }

         }
         
         // --------- implement ListCellRenderer
        /**
         * {@inheritDoc} <p>
         * 
         * Overridden to apply the highlighters, if any, after calling the delegate.
         * The decorators are not applied if the row is invalid.
         */
       public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Component comp = delegateRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            if ((compoundHighlighter != null) && (index >= 0) && (index < getElementCount())) {
                comp = compoundHighlighter.highlight(comp, getComponentAdapter(index));
            }
            return comp;
        }


        // implement RolloverRenderer
        
        /**
         * {@inheritDoc}
         * 
         */
        public boolean isEnabled() {
            return (delegateRenderer instanceof RolloverRenderer) && 
               ((RolloverRenderer) delegateRenderer).isEnabled();
        }
        
        /**
         * {@inheritDoc}
         */
        public void doClick() {
            if (isEnabled()) {
                ((RolloverRenderer) delegateRenderer).doClick();
            }
        }
        
    }

    // --------------------------- updateUI

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update renderer and Highlighters.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        updateRendererUI();
        updateHighlighterUI();
    }

    private void updateRendererUI() {
        if (delegatingRenderer != null) {
            delegatingRenderer.updateUI();
        } else {
            ListCellRenderer renderer = getCellRenderer();
            if (renderer instanceof Component) {
                SwingUtilities.updateComponentTreeUI((Component) renderer);
            }
        }
    }

    /**
     * Updates highlighter after <code>updateUI</code> changes.
     * 
     * @see org.jdesktop.swingx.decorator.UIDependent
     */
    protected void updateHighlighterUI() {
        if (compoundHighlighter == null) return;
        compoundHighlighter.updateUI();
    }

}