/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.Serializable;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import org.jdesktop.swingx.JXGlassBox;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.rollover.RolloverProducer;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;

import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemListManagePanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.swingui.table.IntegerRenderer;
import net.sourceforge.kolmafia.swingui.table.ListWrapperTableModel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StoreManageFrame
	extends GenericPanelFrame
{
	protected static StoreManageFrame INSTANCE = null;
	private static final JLabel searchLabel =
		JComponentUtilities.createLabel( "Mall Prices", SwingConstants.CENTER, Color.black, Color.white );
	private static final LockableListModel priceSummary = new LockableListModel();

	private JComboBox sellingList;
	protected StoreManageTable manageTable;
	private JList resultsDisplay;

	public StoreManageFrame()
	{
		super( "Store Manager" );

		this.tabs.add( "Price Setup", new StoreManagePanel() );
		this.tabs.add( "Additions", new StoreAddPanel() );
		this.tabs.add( "Removals", new StoreRemovePanel() );
		this.tabs.add( "Store Log", new StoreLogPanel() );

		this.setCenterComponent( this.tabs );

		StoreManageFrame.INSTANCE = this;

		StoreManageFrame.updateEarnings( StoreManager.getPotentialEarnings() );
	}

	public static final void cancelTableEditing()
	{
		if ( StoreManageFrame.INSTANCE != null )
		{
			InputFieldUtilities.cancelTableEditing( StoreManageFrame.INSTANCE.manageTable );
		}
	}

	public static final void updateEarnings( final long potentialEarnings )
	{
		if ( StoreManageFrame.INSTANCE == null || GenericFrame.appearsInTab( "StoreManageFrame" ) )
		{
			return;
		}

		StoreManageFrame.INSTANCE.setTitle( "Store Manager (potential earnings: " + KoLConstants.COMMA_FORMAT.format( potentialEarnings ) + " meat)" );
	}

	public static void showGlassBox( int rowIndex, int x, int y)
	{
		/*
		 * This is a fun way to display a little dialog with a checkbox and a textfield.
		 */
		final LimitGlassBox box = new LimitGlassBox( rowIndex );
		final JTextField text = new JTextField();
		final JCheckBox check = new JCheckBox();
		SpringLayout layout = new SpringLayout();
		int limit = (Integer) StoreManageFrame.INSTANCE.manageTable.getModel().getValueAt( rowIndex, 4 );
		JXPanel panel = new JXPanel( new FlowLayout() );

		box.setLayout( layout );

		text.setPreferredSize( new Dimension( 100, text.getPreferredSize().height ) );
		check.setOpaque( false );

		check.setSelected( limit > 0 );
		text.setText( String.valueOf( limit ) );
		text.setEnabled( limit > 0 );

		ActionListener bob = new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				text.setEnabled( check.isSelected() );
				if ( text.isEnabled() && text.getText().equals( "0" ) )
				{
					text.setText( "1" );
				}
				if ( !check.isSelected() )
				{
					box.setLimit( 0 );
					if ( text.getText().equals( "1" ) )
					{
						text.setText( "0" );
					}
				}
				else
				{
					box.setLimit( StringUtilities.parseInt( text.getText() ) );
				}
			}
		};
		check.addActionListener( bob );

		DocumentListener steve = new DocumentListener()
		{

			public void insertUpdate( DocumentEvent e )
			{
				this.changedUpdate( e );
			}

			public void removeUpdate( DocumentEvent e )
			{
				this.changedUpdate( e );
			}

			public void changedUpdate( DocumentEvent e )
			{
				try
				{
					long lim = StringUtilities.parseLong( e.getDocument().getText( 0, e.getDocument().getLength() ) );
					box.setLimit( lim );
				}
				catch ( BadLocationException e1 )
				{
					e1.printStackTrace();
				}
			}
		};
		text.getDocument().addDocumentListener( steve );

		panel.add( check );
		panel.add( text );
		panel.setBorder( BorderFactory.createRaisedBevelBorder() );

		box.add( panel );

		layout.putConstraint( SpringLayout.WEST, panel, x, SpringLayout.WEST, box );
		layout.putConstraint( SpringLayout.NORTH, panel, y, SpringLayout.NORTH, box );

		StoreManageFrame.INSTANCE.setGlassPane( box );

		box.setVisible( true );
	}

	private class StoreManagePanel
		extends GenericPanel
	{
		public StoreManagePanel()
		{
			super( "save prices", "refresh", true );

			StoreManageFrame.this.manageTable = new StoreManageTable();
			StoreManageFrame.this.manageTable.setEditable( true );
			GenericScrollPane manageScroller = new GenericScrollPane( StoreManageFrame.this.manageTable );

			JPanel managePanel = new JPanel( new BorderLayout() );
			managePanel.add( manageScroller, BorderLayout.CENTER );
			JComponentUtilities.setComponentSize( managePanel, 500, 400 );

			JPanel storePanel = new JPanel( new BorderLayout() );
			storePanel.add( managePanel, BorderLayout.CENTER );

			JPanel searchResults = new SearchResultsPanel();

			this.setContent( this.elements, true );
			this.eastContainer.add( searchResults, BorderLayout.CENTER );
			this.container.add( storePanel, BorderLayout.CENTER );
		}

		@Override
		public void actionConfirmed()
		{
			if ( !InputFieldUtilities.finalizeTable( StoreManageFrame.this.manageTable ) )
			{
				return;
			}

			KoLmafia.updateDisplay( "Compiling reprice data..." );
			int rowCount = StoreManageFrame.this.manageTable.getRowCount();

			int[] itemId = new int[ rowCount ];
			int[] prices = new int[ rowCount ];
			int[] limits = new int[ rowCount ];

			SoldItem[] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
			StoreManager.getSoldItemList().toArray( sold );

			for ( int i = 0; i < rowCount; ++i )
			{
				String item = (String) StoreManageFrame.this.manageTable.getValueAt( i, 0 );
				itemId[ i ] = ItemDatabase.getItemId( item );

				prices[ i ] = ( (Integer) StoreManageFrame.this.manageTable.getValueAt( i, 1 ) ).intValue();
				int cheapest = ( (Integer) StoreManageFrame.this.manageTable.getValueAt( i, 2 ) ).intValue();

				if ( cheapest >= 1000000 && prices[ i ] < cheapest * 0.15  )
				{
					String message = item + ": the price is less than 15% of the cheapest in the mall, continue?";
					if ( !InputFieldUtilities.confirm( message ) )
					{
						return;
					}
				}

				int ilim = (Integer) StoreManageFrame.this.manageTable.getValueAt( i, 4 );

				limits[ i ] = Math.max( ilim, 0 );
			}

			RequestThread.postRequest( new ManageStoreRequest( itemId, prices, limits ) );
		}

		@Override
		public void actionCancelled()
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}
	}

	public static final String UNDERCUT_MESSAGE =
		"KoLmafia will take items priced at 999,999,999 meat and undercut the current lowest price in the mall.  " +
		"Would you like KoLmafia to avoid 'minimum possible prices' (100 meat, or twice the autosell value of the item) when doing so?";


	protected class StoreManageTable
		extends ShowDescriptionTable
	{
		public StoreManageTable()
		{
			super( StoreManager.getSoldItemList(), 11, 7 );

			this.setColumnClasses( new Class[]
			{
				String.class,
				Integer.class,
				Integer.class,
				Integer.class,
				Boolean.class,
				JButton.class,
				JButton.class,
			} );

			this.setModel( new StoreManageTableModel() );

			doColumnSetup();
			setEditors();
			setRenderers();
			setHighlighters();
		}

		private void doColumnSetup()
		{
			this.getColumnModel().getColumn( 6 ).setPreferredWidth( 44 );
			this.getColumnModel().getColumn( 6 ).setResizable( false );
			this.getColumnModel().getColumn( 5 ).setPreferredWidth( 44 );
			this.getColumnModel().getColumn( 5 ).setResizable( false );
			this.getColumnModel().getColumn( 4 ).setPreferredWidth( 44 );
			this.getColumnModel().getColumn( 4 ).setResizable( false );
			this.getColumnModel().getColumn( 3 ).setResizable( false );
			this.getColumnModel().getColumn( 0 ).setPreferredWidth( 220 );
			this.getColumnModel().getColumn( 1 ).setPreferredWidth( 100 );
			this.getTableHeader().setReorderingAllowed( false );
			this.setAutoResizeMode( AUTO_RESIZE_NEXT_COLUMN );

			this.setIntercellSpacing( new Dimension(1,1) );
		}

		private void setEditors()
		{
			this.setDefaultEditor( JButton.class, new JButtonHackEditor() );
			this.setDefaultEditor( Boolean.class, new JButtonHackEditor() );
			this.setDefaultEditor( Integer.class, new PriceEditor() );
		}

		private void setRenderers()
		{
			this.setDefaultRenderer( Boolean.class, new BoolRenderer() );
			IntegerRenderer rend = new IntegerRenderer();
			rend.setHorizontalAlignment( JLabel.RIGHT );
			this.setDefaultRenderer( Integer.class, rend );
			this.setDefaultRenderer( String.class, new DefaultTableRenderer() );
		}

		private void setHighlighters()
		{
			Highlighter stripe = HighlighterFactory.createSimpleStriping(new Color( 128, 128, 128 , 64) );
			this.addHighlighter( stripe );

			HighlightPredicate mouseOver = new HighlightPredicate()
			{
				public boolean isHighlighted( Component renderer, ComponentAdapter adapter )
				{
					if ( !adapter.getComponent().isEnabled() )
						return false;
					Point p = (Point) adapter.getComponent().getClientProperty( RolloverProducer.ROLLOVER_KEY );
					return p != null && p.y == adapter.row && convertColumnIndexToModel( p.x ) == 1 && convertColumnIndexToModel( adapter.column ) == 1;
				}
			};

			HighlightPredicate valueChanged = new HighlightPredicate()
			{
				public boolean isHighlighted( Component renderer, ComponentAdapter adapter )
				{
					if ( !adapter.getComponent().isEnabled() )
						return false;
					if ( convertColumnIndexToModel( adapter.column ) == 1 )
					{
					int cellValue =
						(Integer) adapter.getValueAt(
							convertRowIndexToModel( adapter.row ), convertColumnIndexToModel( adapter.column ) );
					SoldItem it =
						( (StoreManageTableModel) StoreManageTable.this.getModel() ).getSoldItem( convertRowIndexToModel( adapter.row ) );
					return it != null && cellValue != it.getPrice();
					}
					if ( convertColumnIndexToModel( adapter.column ) == 4 )
					{
						int cellValue =
								(Integer) adapter.getValueAt(
									convertRowIndexToModel( adapter.row ), convertColumnIndexToModel( adapter.column ) );
							SoldItem it =
								( (StoreManageTableModel) StoreManageTable.this.getModel() ).getSoldItem( convertRowIndexToModel( adapter.row ) );
							return it != null && cellValue != it.getLimit();
					}
					return false;
				}
			};

			HighlightPredicate warning = new HighlightPredicate()
			{
				public boolean isHighlighted( Component renderer, ComponentAdapter adapter )
				{
					if ( !adapter.getComponent().isEnabled() )
						return false;
					if ( convertColumnIndexToModel( adapter.column ) != 1 )
						return false;
					int cellValue =
						(Integer) adapter.getValueAt(
							convertRowIndexToModel( adapter.row ), convertColumnIndexToModel( adapter.column ) );
					SoldItem it =
						( (StoreManageTableModel) StoreManageTable.this.getModel() ).getSoldItem( convertRowIndexToModel( adapter.row ) );
					return it != null && (cellValue < it.getLowest() * 0.15) && it.getLowest() > 50000;
				}
			};

			HighlightPredicate auto = new HighlightPredicate()
			{
				public boolean isHighlighted( Component renderer, ComponentAdapter adapter )
				{
					if ( !adapter.getComponent().isEnabled() )
						return false;
					if ( convertColumnIndexToModel( adapter.column ) != 0 )
						return false;
					int cellValue = (Integer) adapter.getValueAt( convertRowIndexToModel( adapter.row ), 1 );
					return (cellValue == 999999999);
				}
			};

			AbstractHighlighter bold = new AbstractHighlighter( auto )
			{
				private Font BOLD_FONT;
				@Override
				protected Component doHighlight( Component renderer, ComponentAdapter adapter )
				{
					if ( adapter.isSelected() )
					{
						renderer.setForeground( getSelectionForeground() );
					}
					else
					{
						renderer.setForeground( getForeground() );
					}
					if ( BOLD_FONT == null )
					{
						BOLD_FONT = renderer.getFont().deriveFont( Font.BOLD );
					}
					renderer.setFont( BOLD_FONT );
					return renderer;
				}
			};

			AbstractHighlighter underline = new AbstractHighlighter( mouseOver )
			{
				private Font UNDERLINE_FONT;
				@Override
				protected Component doHighlight( Component renderer, ComponentAdapter adapter )
				{
					if ( adapter.isSelected() )
					{
						renderer.setForeground( getSelectionForeground() );
					}
					else
					{
						renderer.setForeground( getForeground() );
					}
					if ( UNDERLINE_FONT == null )
					{
						Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
						fontAttributes.put( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
						UNDERLINE_FONT = renderer.getFont().deriveFont( fontAttributes );
					}
					renderer.setFont( UNDERLINE_FONT );
					return renderer;
				}
			};

			ColorHighlighter mouse = new ColorHighlighter( mouseOver );
			mouse.setForeground( Color.blue );

			ColorHighlighter change = new ColorHighlighter( valueChanged );
			change.setBackground( new Color( 0xB5EAAA ) );
			change.setSelectedBackground( new Color( 0x306754 ) );

			CompoundHighlighter comp = new CompoundHighlighter( underline, mouse );

			ColorHighlighter warn = new ColorHighlighter( warning );
			warn.setBackground( Color.red );
			warn.setSelectedBackground( Color.red );

			this.addHighlighter( bold );
			this.addHighlighter( change );
			this.addHighlighter( comp );
			this.addHighlighter( warn );
		}
		@Override
		public boolean isCellEditable( final int row, final int col )
		{
			int column = convertColumnIndexToModel( col );
			if ( column == 1 || column == 4 || column == 5 || column == 6 )
			{
				return true;
			}
			return false;
		}

		private class JButtonHackEditor
			implements TableCellEditor
		{
			/*
			 * This is an awful, horrible hack, but swing doesn't provide many options otherwise. Since JTables do not
			 * forward MouseEvents to embedded components, we have to hack around it - override the editor and take
			 * advantage of the fact that it knows what cell is trying to be edited. The other way to do this is to
			 * install a mouselistener on the table that converts all mouseevents to row/column coordinates and then
			 * passes a mouse event to that cell. I find that has way more overhead and is less robust than doing it
			 * this way.  Sigh.
			 */

			public Object getCellEditorValue()
			{
				return null;
			}

			public boolean isCellEditable( EventObject anEvent )
			{
				return true;
			}

			public boolean shouldSelectCell( EventObject anEvent )
			{
				return false;
			}

			public boolean stopCellEditing()
			{
				return false;
			}

			public void cancelCellEditing()
			{
			}

			public void addCellEditorListener( CellEditorListener l )
			{
			}

			public void removeCellEditorListener( CellEditorListener l )
			{
			}

			public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row,
				int column )
			{
				if ( value instanceof Boolean || value instanceof Integer )
				{
					Rectangle rect = table.getCellRect( row, column, false );
					// Look at this nonsense
					Point p = new Point( rect.x, rect.y );
					// I hate you Swing
					SwingUtilities.convertPointToScreen( p, table );
					// I mean seriously, WTF
					SwingUtilities.convertPointFromScreen( p, StoreManageFrame.INSTANCE.getContentPane() );
					StoreManageFrame.showGlassBox( convertRowIndexToModel( row ), p.x + rect.width / 2 - 15, p.y + 13 );
				}
				else if ( value instanceof JButton )
				{
					( (JButton) value ).doClick( 10 );
				}

				return null;
			}
		}
	}

	private class StoreManageTableModel
		extends ListWrapperTableModel
	{
		public StoreManageTableModel()
		{
			super(
				new String[] { "Item Name", "Price", "Lowest", "Qty", "Lim", " ", " " },
				new Class[] { String.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
				new boolean[] { false, true, false, false, true, false, false },
				StoreManager.getSoldItemList() );
		}

		@Override
		public Vector<Serializable> constructVector( final Object o )
		{
			Vector<Serializable> value = (Vector<Serializable>) o;
			if ( value.size() < 7 )
			{
				String itemName = (String) value.get( 0 );
				String displayName = StringUtilities.getDisplayName( itemName );
				value.set( 0, displayName );

				JButton removeSomeButton = new JButton( JComponentUtilities.getImage( "xgray.gif" ) );
				removeSomeButton.setToolTipText( "remove some of item from store" );
				removeSomeButton.addActionListener( new RemoveSomeListener( value ) );
				JComponentUtilities.setComponentSize( removeSomeButton, new Dimension( 20, 20 ) );
				value.add( removeSomeButton );

				JButton searchItemButton = new JButton( JComponentUtilities.getImage( "forward.gif" ) );
				searchItemButton.setToolTipText( "price analysis" );
				searchItemButton.addActionListener( new SearchItemListener( itemName ) );
				value.add( searchItemButton );
			}

			return value;
		}

		public SoldItem getSoldItem( int row )
		{
			return (SoldItem) this.listModel.get( row );
		}
	}

	private class SearchItemListener
		extends ThreadedListener
	{
		private final String itemName;

		public SearchItemListener( final String itemName )
		{
			this.itemName = itemName;
		}

		@Override
		protected void execute()
		{
			String searchName = this.itemName;
			if ( searchName == null )
			{
				AdventureResult item = (AdventureResult) StoreManageFrame.this.sellingList.getSelectedItem();
				if ( item == null )
				{
					return;
				}

				searchName = item.getName();
			}

			StoreManageFrame.searchLabel.setText( searchName );
			StoreManager.searchMall( "\"" + searchName + "\"", 10, StoreManageFrame.priceSummary );

			KoLmafia.updateDisplay( "Price analysis complete." );
		}
	}

	private class RemoveSomeListener
		extends ThreadedListener
	{
		private final Vector<Serializable> vector;
		private final int itemId;

		public RemoveSomeListener( Vector<Serializable> vector )
		{
			this.vector = vector;
			String itemName = (String) vector.get( 0 );
			this.itemId = ItemDatabase.getItemId( itemName );
		}

		@Override
		protected void execute()
		{
			int max = (Integer) this.vector.get( 3 );
			Integer val = InputFieldUtilities.getQuantity( "Remove how many?", max, max );
			if ( val != null )
			{
				int qty = val.intValue();
				if ( qty > 0 )
				{
					RequestThread.postRequest( new ManageStoreRequest( this.itemId, qty ) );
				}
			}
		}
	}

	private class StoreAddPanel
		extends JTabbedPane
	{
		public StoreAddPanel()
		{
			super( JTabbedPane.LEFT );
			this.addTab( "Inventory", new StoreAddFromInventoryPanel() );
			this.addTab( "Storage", new StoreAddFromStoragePanel() );
		}
	}

	private class StoreAddFromInventoryPanel
		extends ItemListManagePanel
	{
		public StoreAddFromInventoryPanel()
		{
			super( "mallsell", "autosell", (SortedListModel<AdventureResult>) KoLConstants.inventory );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
			this.filterItems();
		}

		@Override
		public void actionConfirmed()
		{
			AdventureResult[] items = this.getDesiredItems( "Mallsell" );
			if ( items == null )
			{
				return;
			}

			RequestThread.postRequest( new AutoMallRequest( items ) );
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		@Override
		public void actionCancelled()
		{
			AdventureResult[] items = this.getDesiredItems( "Autosell" );
			if ( items == null )
			{
				return;
			}
			RequestThread.postRequest( new AutoSellRequest( items ) );
		}
	}

	private class StoreAddFromStoragePanel
		extends ItemListManagePanel
	{
		public StoreAddFromStoragePanel()
		{
			super( "mallsell", null, (SortedListModel<AdventureResult>) KoLConstants.storage, true, true );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
			this.filterItems();
		}

		@Override
		public void actionConfirmed()
		{
			AdventureResult[] items = this.getDesiredItems( "Mallsell" );
			if ( items == null )
			{
				return;
			}

			RequestThread.postRequest( new ManageStoreRequest( items, true ) );
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		@Override
		public void actionCancelled()
		{
		}
	}

	private class StoreRemovePanel
		extends ItemListManagePanel
	{
		public StoreRemovePanel()
		{
			super( "take all", "take one", StoreManager.getSortedSoldItemList() );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
		}

		@Override
		public void actionConfirmed()
		{
			this.removeItems( true );
		}

		@Override
		public void actionCancelled()
		{
			this.removeItems( false );
		}

		public void removeItems( final boolean takeAll )
		{
			StoreManageFrame.cancelTableEditing();

			Object[] items = this.getElementList().getSelectedValuesList().toArray();

			for ( int i = 0; i < items.length; ++i )
			{
				SoldItem soldItem = ( (SoldItem) items[ i ] );
				int count = takeAll ? soldItem.getQuantity() : 1;
				RequestThread.postRequest( new ManageStoreRequest( soldItem.getItemId(), count ) );
			}

			RequestThread.postRequest( new ManageStoreRequest() );
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the results of the mall search request. Note that
	 * all of the tallying functionality is handled by the <code>LockableListModel</code> provided, so this functions
	 * as a container for that list model.
	 */

	private class SearchResultsPanel
		extends JPanel
	{
		public SearchResultsPanel()
		{
			super( new BorderLayout() );

			JPanel container = new JPanel( new BorderLayout() );
			container.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

			container.add( StoreManageFrame.searchLabel, BorderLayout.NORTH );
			JComponentUtilities.setComponentSize( StoreManageFrame.searchLabel, 150, 16 );

			StoreManageFrame.this.resultsDisplay = new JList( StoreManageFrame.priceSummary );
			StoreManageFrame.this.resultsDisplay.setPrototypeCellValue( "1234567890ABCDEF" );
			StoreManageFrame.this.resultsDisplay.setVisibleRowCount( 11 );
			StoreManageFrame.this.resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			GenericScrollPane scrollArea = new GenericScrollPane( StoreManageFrame.this.resultsDisplay );

			container.add( scrollArea, BorderLayout.CENTER );
			this.add( Box.createVerticalStrut( 20 ), BorderLayout.NORTH );
			this.add( container, BorderLayout.CENTER );
		}
	}

	private class StoreLogPanel
		extends ScrollablePanel
	{
		public StoreLogPanel()
		{
			super( "", "refresh", "resort", new JList( StoreManager.getStoreLog()), false );

			JPanel northPanel = new JPanel( new BorderLayout() );
			this.actualPanel.add( northPanel, BorderLayout.NORTH );

			AutoFilterTextField filterfield = new AutoFilterTextField( (JList) this.scrollComponent );
			this.centerPanel.add( filterfield, BorderLayout.NORTH );
		}

		@Override
		public void actionConfirmed()
		{
			StoreManager.getStoreLog().clear();
			RequestThread.postRequest( new ManageStoreRequest( true ) );
		}

		@Override
		public void actionCancelled()
		{
			StoreManager.sortStoreLog( true );
		}
	}

	public class BoolRenderer
		extends JCheckBox
		implements TableCellRenderer
	{
		public BoolRenderer()
		{
			setHorizontalAlignment( JLabel.CENTER );
		}

		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column )
		{
			if ( isSelected )
			{
				setForeground( table.getSelectionForeground() );
				//super.setBackground(table.getSelectionBackground());
				setBackground( table.getSelectionBackground() );
			}
			else
			{
				setForeground( table.getForeground() );
				setBackground( table.getBackground() );
			}
			setSelected( ( value != null && ( (Integer) value ) > 0 ) );
			return this;
		}
	}

	private static class PriceEditor
		extends DefaultCellEditor
	{
		private static final AutoHighlightTextField rightField = new AutoHighlightTextField()
		{
			@Override
			public int getHorizontalAlignment()
			{
				return JLabel.RIGHT;
			}
		};

		public PriceEditor()
		{
			this( rightField );
		}

		public PriceEditor( JTextField textField )
		{
			super( textField );
		}

		@Override
		public Object getCellEditorValue()
		{
			return StringUtilities.parseInt( super.getCellEditorValue().toString() );
		}

		@Override
		public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row,
			int column )
		{
			if ( value == null )
			{
				return super.getTableCellEditorComponent( table, value, isSelected, row, column );
			}
			( (JTextField) this.editorComponent ).setText( KoLConstants.COMMA_FORMAT.format( value ) );
			return this.editorComponent;
		}

	}

}

class LimitGlassBox
	extends JXGlassBox
{
	private long limit;
	private final int rowIndex;

	public LimitGlassBox( int rowIndex )
	{
		super(1.0f);
		this.rowIndex = rowIndex;
		this.limit = (Integer) StoreManageFrame.INSTANCE.manageTable.getModel().getValueAt( rowIndex, 4 );
	}

	public void setLimit( long lim )
	{
		this.limit = lim;
	}

	@Override
	public void dismiss()
	{
		StoreManageFrame.INSTANCE.manageTable.getModel().setValueAt( (int)limit, rowIndex, 4 );
		super.dismiss();
	}
}
