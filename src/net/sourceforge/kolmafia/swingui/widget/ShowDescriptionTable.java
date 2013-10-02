/**
 * Copyright (c) 2005-2013, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.ColumnControlButton;
import org.jdesktop.swingx.table.TableColumnExt;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.ProfileFrame;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

import com.jgoodies.binding.adapter.AbstractTableAdapter;

/*
 ShowDescriptionTable is a variant of ShowDescriptionList that extends a JXTable instead of a JList.
 It is meant so that you can simply instantiate ShowDescriptionTable instead of ShowDescriptionList,
 and all the "List-specific" methods will be provided in adapter methods.
 */

public class ShowDescriptionTable
	extends JXTable
{
	public int lastSelectIndex;
	public JPopupMenu contextMenu;
	public ListElementFilter filter;

	private final LockableListModel displayModel, originalModel;
	private boolean[] flags;

	private AdaptedTableModel adaptedModel;

	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	private final Comparator<String[]> arrayComparator = new Comparator<String[]>()
	{
		public int compare( String[] o1, String[] o2 )
		{
			if ( o1.length != 2 || o2.length != 2 )
			{
				return 0;
			}
			Integer i1 = Integer.valueOf( o1[1] );
			Integer i2 = Integer.valueOf( o2[1] );
			return i2.compareTo( i1 ); // descending order needed
		}
	};

	public ShowDescriptionTable( final LockableListModel displayModel )
	{
		this( displayModel, null, 4 );
	}

	public ShowDescriptionTable( final LockableListModel displayModel, boolean[] flags )
	{
		this( displayModel, null, 4, 3, flags );
	}

	public ShowDescriptionTable( final LockableListModel displayModel, final int visibleRowCount )
	{
		this( displayModel, null, visibleRowCount );
	}
	
	public ShowDescriptionTable( final LockableListModel displayModel, final int visibleRowCount, final int visibleColumnCount )
	{
		this( displayModel, null, visibleRowCount, visibleColumnCount, new boolean[]
		{
			false, false
		} );
	}

	public ShowDescriptionTable( final LockableListModel displayModel, final ListElementFilter filter )
	{
		this( displayModel, filter, 4 );
	}

	public ShowDescriptionTable( final LockableListModel displayModel, final ListElementFilter filter,
			final int visibleRowCount )
	{
		this( displayModel, filter, 4, 3, new boolean[]
		{
			false, false
		} );
	}

	public ShowDescriptionTable( final LockableListModel displayModel, final ListElementFilter filter,
			final int visibleRowCount, final int visibleColumnCount, final boolean[] flags )
	{
		this.contextMenu = new JPopupMenu();
		this.flags = flags;

		boolean isMoodList = displayModel == MoodManager.getTriggers();
		boolean isEncyclopedia = !displayModel.isEmpty() && displayModel.get( 0 ) instanceof Entry;

		if ( !isMoodList )
		{
			if ( displayModel.size() == 0 || !isEncyclopedia )
			{
				this.contextMenu.add( new ContextMenuItem( "Game description",
					new DescriptionRunnable() ) );
			}

			this.contextMenu.add( new ContextMenuItem( "Wiki description", new WikiLookupRunnable() ) );
		}

		if ( displayModel == KoLConstants.activeEffects )
		{
			this.contextMenu.add( new ContextMenuItem( "Remove this effect", new ShrugOffRunnable() ) );
			this.contextMenu
				.add( new ContextMenuItem( "Add to current mood", new AddToMoodEffectRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Extend this effect", new ExtendEffectRunnable() ) );
		}

		if ( displayModel == KoLConstants.usableSkills || displayModel == KoLConstants.availableSkills )
		{
			this.contextMenu.add( new ContextMenuItem( "Cast the skill once", new CastSkillRunnable() ) );
			this.contextMenu
				.add( new ContextMenuItem( "Add to current mood", new AddToMoodSkillRunnable() ) );
		}

		if ( displayModel == KoLConstants.tally )
		{
			this.contextMenu.add( new ContextMenuItem( "Zero out entries", new ZeroTallyRunnable() ) );
			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new ContextMenuItem( "Add to junk list", new AddToJunkListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to singleton list",
				new AddToSingletonListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to memento list",
				new AddToMementoListRunnable() ) );

			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new ContextMenuItem( "Autosell selected", new AutoSellRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add selected to mall", new AutoMallRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Consume selected", new ConsumeRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Pulverize selected", new PulverizeRunnable() ) );
		}
		else if ( displayModel == KoLConstants.inventory || displayModel == KoLConstants.closet
			|| isEncyclopedia )
		{
			this.contextMenu.add( new ContextMenuItem( "Add to junk list", new AddToJunkListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to singleton list",
				new AddToSingletonListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to memento list",
				new AddToMementoListRunnable() ) );
		}
		else if ( isMoodList )
		{
			this.contextMenu.add( new ContextMenuItem( "Force execution", new ForceExecuteRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Remove selected", new RemoveTriggerRunnable() ) );

			this.addKeyListener( new RemoveTriggerListener() );
		}

		this.addMouseListener( new PopupListener() );

		// Add functionality for scrolling to an entry when the user types in a partial name.
		// This is provided natively by JList, but needs to be added here because tables do not natively provide
		// this behavior (for obvious reasons).
		this.addKeyListener( new KeyAdapter()
		{
			private long saved_ms = 0;
			private int TIMETOWAIT = 700;
			private String searchField = "";

			@Override
			public void keyTyped( KeyEvent e )
			{
				if ( e.isMetaDown() )
				{
					// Workaround mac-specific issue: when meta (command) is pressed, keyTyped events still fire.
					// This is undesirable since meta-a is supposed to select all.  Notably, command-a on windows
					// does not encounter this problem.
					return;
				}
				// the search "resets" after a brief pause.  700ms feels about right.
				if ( System.currentTimeMillis() - this.saved_ms < TIMETOWAIT )
				{
					this.searchField = this.searchField + e.getKeyChar();
				}
				else
				{
					this.searchField = "" + e.getKeyChar();
				}
				ShowDescriptionTable.this.fireSearch( searchField );
				this.saved_ms = System.currentTimeMillis();
			}
		} );

		this.originalModel = displayModel;
		this.displayModel = filter == null ? displayModel.getMirrorImage() : displayModel
			.getMirrorImage( filter );

		String[] colNames = TableCellFactory.getColumnNames( this.originalModel, flags );
		this.adaptedModel = new AdaptedTableModel( this.displayModel, colNames );

		// this.getTableHeader().setReorderingAllowed( false );
		this.setShowGrid( false );
		this.setModel( this.adaptedModel );

		// form of.. magic numbers
		this.getColumnModel().getColumn( 0 ).setPreferredWidth( 220 );

		// Make the viewport behave
		this.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
		this.setVisibleRowCount( visibleRowCount );

		// Enable column visibility control
		this.setColumnControlVisible( true );
		ColumnControlButton btn = new ColumnControlButton( this )
		{
			// Disable the "extra" visibility features which are added by default.
			// We have no need of installing horizontal scrollbars, packing columns, etc.
			@Override
			protected void addAdditionalActionItems()
			{
			}
		};
		this.setColumnControl( btn );

		// Set columns > visibleColumnCount to not visible.
		List<TableColumn> allColumns = getColumns( true );
		for ( TableColumn t : allColumns )
		{
			TableColumnExt ext = (TableColumnExt) t;
			ext.setComparator( new RenderedComparator( this.originalModel, ext.getModelIndex(), flags ) );
			if ( ext.getModelIndex() >= visibleColumnCount )
			{
				ext.setVisible( false );
			}
		}

		this.setDefaultRenderer( String.class, new DescriptionTableRenderer( this.originalModel, flags ) );
		this.setDefaultRenderer( Integer.class, new DescriptionTableRenderer( this.originalModel, flags ) );
		this.setDefaultRenderer( JButton.class, new DescriptionTableRenderer( this.originalModel, flags ) );
		
		this.setIntercellSpacing( new Dimension( 0, 0 ) );
		
		// install a handler to provide saner clipboard behavior
		this.setTransferHandler( new ClipboardHandler() );
	}

	protected void fireSearch( String searchField )
	{
		if ( convertColumnIndexToModel( this.getSelectedColumn() ) != 0 )
		{
			return;
		}
		for ( int i = 0; i < this.getRowCount(); i++ )
		{
			String val = this.getValueAt( i, convertColumnIndexToModel( 0 ) ).toString().toLowerCase();
			if ( val.startsWith( searchField.toLowerCase() ) )
			{
				ShowDescriptionTable.this.setRowSelectionInterval( i, i );
				ShowDescriptionTable.this.scrollCellToVisible( i, i );
				break;
			}
		}
	}

	public void setColumnClasses( Class< ? >[] classDefs )
	{
		String[] colNames = TableCellFactory.getColumnNames( this.originalModel, this.flags );
		this.adaptedModel = new AdaptedTableModel( this.displayModel, colNames, classDefs );

		this.setModel( this.adaptedModel );
	}

	private class RenderedComparator
		implements Comparator<Object>
	{
		private int column;
		private boolean[] flags;
		private LockableListModel model;

		public RenderedComparator( LockableListModel originalModel, int column, boolean[] flags )
		{
			this.model = originalModel;
			this.column = column;
			this.flags = flags;
		}

		public int compare( Object o1, Object o2 )
		{
			Object o1val = TableCellFactory.get( this.column, this.model, o1, this.flags, false, true );
			Object o2val = TableCellFactory.get( this.column, this.model, o2, this.flags, false, true );
			if ( o1val instanceof Integer )
			{
				if ( o2val == null )
				{
					return 1;
				}
				Integer a1 = (Integer) o1val;
				Integer a2 = (Integer) o2val;
				return a1.compareTo( a2 );
			}
			if ( o2val instanceof Integer )
			{
				// o1val was null
				return -1;
			}
			if ( o1val instanceof String )
			{
				if ( o2val == null )
				{
					return 1;
				}
				String a1 = (String) o1val;
				String a2 = (String) o2val;
				return a1.compareToIgnoreCase( a2 );
			}
			if ( o2val instanceof String )
			{
				// o1val was null
				return -1;
			}
			return 0;
		}
	}

	public LockableListModel getOriginalModel()
	{
		return this.originalModel;
	}

	public LockableListModel getDisplayModel()
	{
		return displayModel;
	}

	// This is the adapted model object. ListModel -> Wrapper -> TableModel
	public class AdaptedTableModel
		extends AbstractTableAdapter
	{
		protected LockableListModel model;
		private Class<?>[] classDefs;

		public AdaptedTableModel( LockableListModel listModel, String[] columnNames )
		{
			super( listModel, columnNames );
			this.model = listModel;
		}

		public AdaptedTableModel( LockableListModel listModel, String[] columnNames, Class< ? >[] classDefs )
		{
			super( listModel, columnNames );
			this.model = listModel;
			this.classDefs = classDefs;
		}

		public Object getValueAt( int rowIndex, int columnIndex )
		{
			return getRow( rowIndex );
		}

		public Object getValueAt( int rowIndex )
		{
			return getRow( rowIndex );
		}

		public LockableListModel getModel()
		{
			return this.model;
		}

		@Override
		public Class<?> getColumnClass( int col )
		{
			if ( this.classDefs != null )
			{
				return classDefs[ col ];
			}
			if ( col == 0 )
			{
				// Item name should be the only column that we need to do a String compareTo(). It
				// should always be first.
				return String.class;
			}
			return Integer.class;
		}
	}

	public class DescriptionTableRenderer
		extends DefaultTableCellRenderer
	{
		protected LockableListModel model;
		private boolean[] flags;

		public DescriptionTableRenderer( LockableListModel originalModel, boolean[] flags )
		{
			this.model = originalModel;
			this.flags = flags;
		}

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int col )
		{
			Object it =
				TableCellFactory.get( convertColumnIndexToModel( col ), this.model, value, this.flags, isSelected );
			if ( it instanceof JButton )
			{
				this.setToolTipText( ( (JButton) it ).getToolTipText() );
				return (Component) it;
			}
			this.setValue( it );
			this.setToolTipText( TableCellFactory.getTooltipText( value , flags ) );
			
			if ( isSelected )
			{
				this.setBackground( SystemColor.textHighlight );
				this.setForeground( SystemColor.textHighlightText );
			}

			return this;
		}
	}

	private class PopupListener
		extends MouseAdapter
	{
		@Override
		public void mousePressed( final MouseEvent e )
		{
			this.maybeShowPopup( e );
		}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			this.maybeShowPopup( e );
		}

		private void maybeShowPopup( final MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				int index = ShowDescriptionTable.this.rowAtPoint( ( e.getPoint() ) );
				if ( index == -1 )
				{
					return;
				}
				ShowDescriptionTable.this.lastSelectIndex = index;

				if ( !ShowDescriptionTable.this.isRowSelected( ( index ) ) )
				{
					ShowDescriptionTable.this.clearSelection();
					ShowDescriptionTable.this.setRowSelectionInterval( index, index );
				}

				ShowDescriptionTable.this.contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
		}
	}

	public class ClipboardHandler
		extends TransferHandler
	{
		@Override
		protected Transferable createTransferable( JComponent c )
		{
			JTable t = (JTable) c;
			return new Selection( t );
		}

		@Override
		public int getSourceActions( JComponent c )
		{
			return COPY;
		}

		class Selection
			implements Transferable
		{
			private final JTable delegate;
			private final List<DataFlavor> flavors = new ArrayList<DataFlavor>();

			public Selection( JTable t )
			{
				this.flavors.add( DataFlavor.stringFlavor );
				this.delegate = t;
			}

			public DataFlavor[] getTransferDataFlavors()
			{
				return flavors.toArray( new DataFlavor[ 0 ] );
			}

			public boolean isDataFlavorSupported( DataFlavor flavor )
			{
				return flavors.contains( flavor );
			}

			public Object getTransferData( DataFlavor flavor )
				throws UnsupportedFlavorException, IOException
			{
				int row = delegate.getSelectedRow();
				return delegate.getValueAt( row, 0 ).toString();
			}
		}
	}

	public static final void showGameDescription( Object item )
	{
		if ( item instanceof Boost )
		{
			item = ( (Boost) item ).getItem();
		}

		if ( item instanceof AdventureResult )
		{
			if ( ( (AdventureResult) item ).isItem() )
			{
				StaticEntity.openDescriptionFrame( "desc_item.php?whichitem="
					+ ItemDatabase.getDescriptionId( ( (AdventureResult) item ).getItemId() ) );
			}
			if ( ( (AdventureResult) item ).isStatusEffect() )
			{
				StaticEntity.openDescriptionFrame( "desc_effect.php?whicheffect="
					+ EffectDatabase.getDescriptionId( EffectDatabase
						.getEffectId( ( (AdventureResult) item ).getName() ) ) );
			}
		}
		else if ( item instanceof Concoction )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem="
				+ ItemDatabase.getDescriptionId( ( (Concoction) item ).getName() ) );
		}
		else if ( item instanceof CreateItemRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem="
				+ ItemDatabase.getDescriptionId( ( (CreateItemRequest) item ).getItemId() ) );
		}
		else if ( item instanceof PurchaseRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem="
				+ ItemDatabase.getDescriptionId( ( (PurchaseRequest) item ).getItemId() ) );
		}
		else if ( item instanceof UseSkillRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_skill.php?whichskill="
				+ ( (UseSkillRequest) item ).getSkillId() + "&self=true" );
		}
		else if ( item instanceof SoldItem )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (SoldItem) item ).getItemId() ) );
		}
		else if ( item instanceof String )
		{
			Matcher playerMatcher = ShowDescriptionTable.PLAYERID_MATCHER.matcher( (String) item );
			if ( playerMatcher.find() )
			{
				Object[] parameters = new Object[]
				{
					"#" + playerMatcher.group( 1 )
				};
				SwingUtilities.invokeLater( new CreateFrameRunnable( ProfileFrame.class, parameters ) );
			}
		}
	}

	public static final String getWikiLocation( Object item )
	{
		if ( item == null )
		{
			return null;
		}

		String name = null;

		boolean isItem = false;
		boolean isEffect = false;
		boolean isSkill = false;

		if ( item instanceof Boost )
		{
			item = ( (Boost) item ).getItem();
		}

		if ( item instanceof AdventureResult )
		{
			AdventureResult result = (AdventureResult) item;
			name = result.getName();

			isItem = result.isItem();
			isEffect = result.isStatusEffect();
		}
		else if ( isSkill )
		{
			name = ( (UseSkillRequest) item ).getSkillName();
			isEffect = true;
		}
		else if ( item instanceof Concoction )
		{
			name = ( (Concoction) item ).getName();
			isItem = true;
		}
		else if ( item instanceof CreateItemRequest )
		{
			name = ( (CreateItemRequest) item ).getName();
			isItem = true;
		}
		else if ( item instanceof PurchaseRequest )
		{
			name = ( (PurchaseRequest) item ).getItemName();
			isItem = true;
		}
		else if ( item instanceof SoldItem )
		{
			name = ( (SoldItem) item ).getItemName();
			isItem = true;
		}
		else if ( item instanceof String )
		{
			name = (String) item;
		}
		else if ( item instanceof Entry )
		{
			name = (String) ( (Entry) item ).getValue();
		}

		if ( name == null )
		{
			return null;
		}

		boolean inItemTable = ItemDatabase.contains( name );
		boolean inEffectTable = EffectDatabase.contains( name );
		boolean inSkillTable = SkillDatabase.contains( name );

		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods != null )
		{
			String wikiname = mods.getString( "Wiki Name" );
			if ( wikiname != null && wikiname.length() > 0 )
			{
				name = wikiname;
			}
		}

		if ( isItem && ( inEffectTable || inSkillTable ) )
		{
			name = name + " (item)";
		}
		else if ( isEffect && ( inItemTable || inSkillTable ) )
		{
			name = name + " (effect)";
		}
		else if ( isSkill && ( inItemTable || inEffectTable ) )
		{
			name = name + " (skill)";
		}

		name = StringUtilities.globalStringReplace( name, " ", "_" );
		name = Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
		return "http://kol.coldfront.net/thekolwiki/index.php/" + StringUtilities.getURLEncode( name );
	}

	public static final void showWikiDescription( final Object item )
	{
		String location = ShowDescriptionTable.getWikiLocation( item );

		if ( location != null )
		{
			RelayLoader.openSystemBrowser( location );
		}
	}

	private class ContextMenuItem
		extends ThreadedMenuItem
	{
		public ContextMenuItem( final String title, final ThreadedListener action )
		{
			super( title, action );
		}
	}

	private abstract class ContextMenuListener
		extends ThreadedListener
	{
		public int index;
		public Object item;

		@Override
		protected void execute()
		{
			this.index = ShowDescriptionTable.this.lastSelectIndex == -1 ? ShowDescriptionTable.this
				.getSelectedRow() : ShowDescriptionTable.this.lastSelectIndex;

			this.item = ShowDescriptionTable.this.displayModel.getElementAt( ShowDescriptionTable.this
				.convertRowIndexToModel( this.index ) );

			if ( this.item == null )
			{
				return;
			}

			// ShowDescriptionTable.this.ensureIndexIsVisible( this.index );

			this.executeAction();
		}

		protected abstract void executeAction();
	}

	/**
	 * Utility class which shows the description of the item which is currently selected.
	 */

	private class DescriptionRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			ShowDescriptionTable.showGameDescription( this.item );
		}
	}

	/**
	 * Utility class which shows the description of the item which is currently selected, as it appears on the wiki.
	 */

	private class WikiLookupRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			ShowDescriptionTable.showWikiDescription( this.item );
		}
	}

	public void removeTriggers()
	{
		Object[] items = ShowDescriptionTable.this.getSelectedValues();
		ShowDescriptionTable.this.clearSelection();

		MoodManager.removeTriggers( items );
		MoodManager.saveSettings();
	}

	public Object[] getSelectedValues()
	{
		/*
		 * Since this function exists for lists but not for tables, provide this as a pseudo-adapter function.
		 * Note that we have to get the MODEL index from the sorter object, as there is possibly a
		 * mapping of viewIndex -> modelIndex caused by sorting.
		 */

		int[] selectedRows = this.getSelectedRows();

		Object[] selectedValues = new Object[ selectedRows.length ];

		for ( int i = 0; i < selectedRows.length; ++i )
		{
			selectedValues[ i ] = this.displayModel.getElementAt( this
				.convertRowIndexToModel( selectedRows[ i ] ) );
		}
		return selectedValues;
	}

	private class ForceExecuteRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( ( (MoodTrigger) items[ i ] ).getAction() );
			}
		}
	}

	private class RemoveTriggerRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			ShowDescriptionTable.this.removeTriggers();
		}
	}

	private class RemoveTriggerListener
		extends KeyAdapter
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
			if ( e.isConsumed() )
			{
				return;
			}

			if ( e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE )
			{
				return;
			}

			ShowDescriptionTable.this.removeTriggers();
			e.consume();
		}
	}

	private class CastSkillRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] skills = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			UseSkillRequest request;

			for ( int i = 0; i < skills.length; ++i )
			{
				request = (UseSkillRequest) skills[ i ];

				request.setTarget( null );
				request.setBuffCount( 1 );

				RequestThread.postRequest( request );
			}
		}
	}

	private class AddToMoodSkillRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] skills = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			if ( Preferences.getString( "currentMood" ).equals( "apathetic" ) )
			{
				Preferences.setString( "currentMood", "default" );
			}

			String name, action;

			for ( int i = 0; i < skills.length; ++i )
			{
				name = UneffectRequest.skillToEffect( ( (UseSkillRequest) skills[ i ] ).getSkillName() );

				action = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodManager.addTrigger( "lose_effect", name, action );
				}
			}
			MoodManager.saveSettings();
		}
	}

	private class AddToMoodEffectRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] effects = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			if ( Preferences.getString( "currentMood" ).equals( "apathetic" ) )
			{
				Preferences.setString( "currentMood", "default" );
			}

			String name, action;

			for ( int i = 0; i < effects.length; ++i )
			{
				name = ( (AdventureResult) effects[ i ] ).getName();

				action = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodManager.addTrigger( "lose_effect", name, action );
					continue;
				}

				action = MoodManager.getDefaultAction( "gain_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodManager.addTrigger( "gain_effect", name, action );
				}
			}
			MoodManager.saveSettings();
		}
	}

	private class ExtendEffectRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] effects = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			String name, action;

			for ( int i = 0; i < effects.length; ++i )
			{
				name = ( (AdventureResult) effects[ i ] ).getName();

				action = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					CommandDisplayFrame.executeCommand( action );
				}
			}
		}
	}

	private class ShrugOffRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] effects = ShowDescriptionTable.this.getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
			{
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[ i ] ) );
			}
		}
	}

	private class AddToJunkListRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult
					&& ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry
					&& ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1,
						false );
				}

				if ( data == null )
				{
					continue;
				}

				if ( !KoLConstants.junkList.contains( data ) )
				{
					KoLConstants.junkList.add( data );
				}
			}
		}
	}

	private class AddToSingletonListRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult
					&& ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry
					&& ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1,
						false );
				}

				if ( data == null )
				{
					continue;
				}

				if ( !KoLConstants.junkList.contains( data ) )
				{
					KoLConstants.junkList.add( data );
				}
				if ( !KoLConstants.singletonList.contains( data ) )
				{
					KoLConstants.singletonList.add( data );
				}
			}
		}
	}

	private class AddToMementoListRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionTable.this.getSelectedValues();
			ShowDescriptionTable.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult
					&& ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry
					&& ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1,
						false );
				}

				if ( data != null && !KoLConstants.mementoList.contains( data ) )
				{
					KoLConstants.mementoList.add( data );
				}
			}

			Preferences.setBoolean( "mementoListActive", true );
		}
	}

	private class ZeroTallyRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionTable.this.getSelectedValues();
			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult.addResultToList( KoLConstants.tally,
					( (AdventureResult) items[ i ] ).getNegation() );
			}
		}
	}

	private class AutoSellRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "Are you sure you would like to sell the selected items?" ) )
			{
				return;
			}

			RequestThread
				.postRequest( new AutoSellRequest( ShowDescriptionTable.this.getSelectedValues() ) );
		}
	}

	private class AutoMallRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities
				.confirm( "Are you sure you would like to add the selected items to your store?" ) )
			{
				return;
			}

			RequestThread
				.postRequest( new AutoMallRequest( ShowDescriptionTable.this.getSelectedValues() ) );
		}
	}

	private class ConsumeRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "Are you sure you want to use the selected items?" ) )
			{
				return;
			}

			Object[] items = ShowDescriptionTable.this.getSelectedValues();

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( (AdventureResult) items[ i ] ) );
			}
		}
	}

	private class PulverizeRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities
				.confirm( "The items you've selected will be smashed to pieces.  Are you sure?" ) )
			{
				return;
			}

			Object[] items = ShowDescriptionTable.this.getSelectedValues();

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[ i ] ) );
			}
		}
	}

	/*
	 * And now a bunch of adapter functions.
	 * These are methods that are provided in a list interface that are not provided in a table interface.
	 */

	public void ensureIndexIsVisible( int index )
	{
		this.scrollRowToVisible( convertRowIndexToView( index ) );
	}

	public int locationToIndex( Point point )
	{
		return convertRowIndexToModel( this.rowAtPoint( point ) );
	}

	public int getSelectedIndex()
	{
		return convertRowIndexToModel( this.getSelectedRow() );
	}

	public void setSelectedIndex( int i )
	{
		ShowDescriptionTable.this.setRowSelectionInterval( i, i );
	}

	public void setCellRenderer( DefaultListCellRenderer renderer )
	{
		// Blank method for now, no cellrenderer needed.
		// Eventually might want to do something with this?
	}

	public String collectHeaderStates()
	{
		// JViewPort, GenericScrollPane, JPanel, JPanel, JPanel, Inventory/Restore/etc...

		StringBuilder buffer = new StringBuilder();
		List<TableColumn> cols = this.getColumns( true );
		for ( int i = 0; i < cols.size(); ++i )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( "|" );
			}
			buffer.append( cols.get( i ).getHeaderValue() );
			buffer.append( ":" );
			buffer.append( convertColumnIndexToView( cols.get( i ).getModelIndex() ) );
		}
		buffer.append( ";" );
		return buffer.toString();
	}

	public void setHeaderStates( String rawPref )
	{
		List<TableColumn> cols = this.getColumns( true );
		ArrayList<String[]> sortCols = new ArrayList<String[]>();

		// rawPref is a pipe-delimited list of (header name):(view index)
		String[] split1 = rawPref.split( "\\|" );
		for ( String it : split1 )
		{
			String[] split2 = it.split( "\\:" );
			if ( split2.length != 2 )
			{
				// malformed, or the first element (panel name)
				continue;
			}
			else
			{
				String[] entry = new String[]
				{
					split2[ 0 ], split2[ 1 ]
				};

				sortCols.add( entry );
			}
		}

		Collections.sort( sortCols, arrayComparator );

		// Now, go through and set visibility. The comparator sorts things in descending order, so the first
		// column will be the highest index that's visible.

		for ( String[] it : sortCols )
		{
			if ( it.length != 2 )
			{
				// malformed, no idea how that happened. punt
				return;
			}
			for ( TableColumn t : cols )
			{
				// find the column that matches the name
				if ( t.getHeaderValue().toString().equals( it[ 0 ] ) )
				{
					// set it visible if its index isn't -1
					this.getColumnExt( t.getHeaderValue() ).setVisible(
						Integer.valueOf( it[ 1 ] ) >= 0 );
					break;
				}
			}
		}

		// All the columns have their visibility set now. Just move them into their proper order.

		for ( String[] it : sortCols )
		{
			if ( it.length != 2 )
			{
				// malformed, no idea how that happened. punt
				return;
			}
			if ( Integer.valueOf( it[ 1 ] ) < 0 )
			{
				// Once we hit the negative ones, they're all negative after that. We're done.
				break;
			}
			for ( TableColumn t : cols )
			{
				if ( t.getHeaderValue().toString().equals( it[ 0 ] ) )
				{
					this.moveColumn( convertColumnIndexToView( t.getModelIndex() ),
						Integer.valueOf( it[ 1 ] ) );
					break;
				}
			}
		}
	}
}
