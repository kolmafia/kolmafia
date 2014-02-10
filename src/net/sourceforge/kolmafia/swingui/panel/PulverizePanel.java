/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;

import net.sourceforge.kolmafia.request.PulverizeRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.textui.command.SendMessageCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class PulverizePanel
	extends ItemListManagePanel
{
	private JTable yields;

	public PulverizePanel()
	{
		super( KoLConstants.inventory );

		this.setButtons( true, new ActionListener[] {
				new EnqueueListener(),
				new DequeueListener(),
				new ClearListener(),
				new PulverizeListener(),
				new SmashbotListener(),
				new WadbotListener(),
		} );

		this.getElementList().setCellRenderer( ListCellRendererFactory.getPulverizationRenderer() );
		this.movers[ 2 ].setSelected( true );
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( this.buttons != null && !KoLCharacter.hasSkill( "Pulverize" ) )
		{
			this.buttons[ 3 ].setEnabled( false );
		}
	}

	private static class YieldsModel
		extends DefaultTableModel
	{
		private static String[][] contents = new String[][] { {
			"P", "2P", "3P",
			"<html><center>4P<br>or N</html>",
			"<html><center>N+3P<br>or 2N</html>",
			"3N",
			"<html><center>4N<br>or W</html>",
			"<html><center>W+3N<br>or 2W</html>",
			"3W",
			"1C"
		} };

		public YieldsModel()
		{
			super( contents, contents[0] );
		}

		@Override
		public boolean isCellEditable( int row, int column )
		{
			return false;
		}
	}
	@Override
	public void addFilters()
	{
		JPanel filterPanel = new JPanel();
		this.filters = new JCheckBox[ 8 ];

		this.filters[ 0 ] = new JCheckBox( "twinkly", true );
		this.filters[ 1 ] = new JCheckBox( "<html><font color=red>hot</html>", true );
		this.filters[ 2 ] = new JCheckBox( "<html><font color=blue>cold</html>", true );
		this.filters[ 3 ] = new JCheckBox( "<html><font color=green>stench</html>", true );
		this.filters[ 4 ] = new JCheckBox( "<html><font color=gray>spooky</html>", true );
		this.filters[ 5 ] = new JCheckBox( "<html><font color=purple>sleaze</html>", true );
		this.filters[ 6 ] = new JCheckBox( "Smiths", true );
		this.filters[ 7 ] = new JCheckBox( "other" );

		for ( int i = 0; i < 8; ++i )
		{
			filterPanel.add( this.filters[ i ] );
			this.listenToCheckBox( this.filters[ i ] );
		}

		this.northPanel.add( filterPanel, BorderLayout.NORTH );

		this.yields = new JTable( new YieldsModel() );
		this.yields.setTableHeader( null );
		this.yields.setShowVerticalLines( true );
		this.yields.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
		this.yields.setCellSelectionEnabled( true );
		this.yields.selectAll();
		DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
		tcr.setHorizontalAlignment( DefaultTableCellRenderer.CENTER );
		this.yields.setDefaultRenderer( Object.class, tcr );
		Dimension dims = tcr.getTableCellRendererComponent( this.yields,
			"<html>&nbsp;W+3N&nbsp;<br>&nbsp;N+3P&nbsp;</html>",
			false, false, 0, 0 ).getPreferredSize();
		this.yields.setRowHeight( dims.height );
		dims.width *= 9;
		this.yields.setPreferredScrollableViewportSize( dims );
		this.yields.setToolTipText( "Drag to select a range of yields. " +
			"P = powder, N = nugget, W = wad, C = cluster." );
		this.yields.getColumnModel().getSelectionModel().addListSelectionListener(
			(ListSelectionListener) this.filterfield );

		// If the yields list was added directly to northPanel, it would get horizontally
		// stretched, creating useless blank space inside the list frame.  Having an
		// intermediate JPanel, with the default FlowLayout, allows the list to take on its
		// natural width.
		JPanel panel = new JPanel();
		panel.add( new JScrollPane( this.yields,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ) );
		this.northPanel.add( panel, BorderLayout.CENTER );
		this.filterItems();
	}

	@Override
	public AutoFilterTextField getWordFilter()
	{
		return new EquipmentFilterField();
	}

	private class EquipmentFilterField
		extends AutoFilterTextField
		implements ListSelectionListener
	{
		boolean others = false;
		boolean smiths = false;
		int elemMask = 0;
		int yieldMask = 0;

		public EquipmentFilterField()
		{
			super( PulverizePanel.this.getElementList() );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			this.update();
		}

		@Override
		public synchronized void update()
		{
			this.smiths = PulverizePanel.this.filters[ 6 ].isSelected();
			this.others = PulverizePanel.this.filters[ 7 ].isSelected();
			this.elemMask =
				(PulverizePanel.this.filters[ 0 ].isSelected() ?
					EquipmentDatabase.ELEM_TWINKLY : 0) |
				(PulverizePanel.this.filters[ 1 ].isSelected() ?
					EquipmentDatabase.ELEM_HOT : 0) |
				(PulverizePanel.this.filters[ 2 ].isSelected() ?
					EquipmentDatabase.ELEM_COLD : 0) |
				(PulverizePanel.this.filters[ 3 ].isSelected() ?
					EquipmentDatabase.ELEM_STENCH : 0) |
				(PulverizePanel.this.filters[ 4 ].isSelected() ?
					EquipmentDatabase.ELEM_SPOOKY : 0) |
				(PulverizePanel.this.filters[ 5 ].isSelected() ?
					EquipmentDatabase.ELEM_SLEAZE : 0) |
				(this.others ? EquipmentDatabase.ELEM_OTHER : 0);
			this.yieldMask = 0;
			int[] indices = PulverizePanel.this.yields.getSelectedColumns();
			for ( int i = 0; i < indices.length; ++i )
			{
				this.yieldMask |= EquipmentDatabase.YIELD_1P << indices[ i ];
			}
			super.update();
		}

		@Override
		public boolean isVisible( final Object element )
		{
			if ( element == null || !(element instanceof AdventureResult) )
			{
				return false;
			}

			boolean isVisibleWithFilter = true;
			int itemId = ( (AdventureResult) element ).getItemId();
			if ( itemId == -1 )
			{
				return false;
			}

			int pulver = EquipmentDatabase.getPulverization( itemId );
			if ( pulver == -1 )
			{
				return false;
			}

			if ( pulver == ItemPool.HANDFUL_OF_SMITHEREENS )
			{
				isVisibleWithFilter = this.smiths;
			}
			else if ( pulver >= 0 )
			{
				isVisibleWithFilter = this.others;
			}
			else
			{
				isVisibleWithFilter = ( pulver & this.elemMask ) != 0 &&
					( pulver & this.yieldMask ) != 0;
			}

			return isVisibleWithFilter && super.isVisible( element );
		}
	}

	public class EnqueueListener
		extends TransferListener
	{
		public EnqueueListener()
		{
			super( "Smashing", false );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				if ( item.getCount() > 0 )
				{
					KoLConstants.pulverizeQueue.remove( item );
					KoLConstants.pulverizeQueue.add( item );
					LockableListModel inv = (LockableListModel)
						PulverizePanel.this.getElementList().getModel();
					int index = inv.indexOf( item );
					inv.fireContentsChanged( inv, index, index );
				}
			}
		}

		@Override
		public String toString()
		{
			return "add to queue";
		}
	}

	public class DequeueListener
		extends TransferListener
	{
		public DequeueListener()
		{
			super( "Keeping", false );
		}

		@Override
		protected void execute()
		{
			Object[] items = this.initialSetup( ItemManagePanel.TAKE_ALL );
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				if ( item.getCount() > 0 )
				{
					KoLConstants.pulverizeQueue.remove( item );
					LockableListModel inv = (LockableListModel)
						PulverizePanel.this.getElementList().getModel();
					int index = inv.indexOf( item );
					inv.fireContentsChanged( inv, index, index );
				}
			}
		}

		@Override
		public String toString()
		{
			return "remove from queue";
		}
	}

	public class ClearListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			KoLConstants.pulverizeQueue.clear();
			LockableListModel inv = (LockableListModel)
				PulverizePanel.this.getElementList().getModel();
			inv.fireContentsChanged( inv, 0, inv.size() - 1 );
		}

		@Override
		public String toString()
		{
			return "clear queue";
		}
	}

	public class PulverizeListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( KoLConstants.pulverizeQueue.isEmpty() )
			{
				(new EnqueueListener()).run();

				if ( KoLConstants.pulverizeQueue.isEmpty() )
				{
					InputFieldUtilities.alert( "No items selected or in queue!" );
					return;
				}
			}

			AdventureResult[] items =
				new AdventureResult[ KoLConstants.pulverizeQueue.size() ];
			KoLConstants.pulverizeQueue.toArray( items );
			KoLConstants.pulverizeQueue.clear();
			LockableListModel inv = (LockableListModel)
				PulverizePanel.this.getElementList().getModel();
			inv.fireContentsChanged( inv, 0, inv.size() - 1 );
			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( items[ i ] ) );
			}
		}

		@Override
		public String toString()
		{
			return "pulverize";
		}
	}

	private static class MsgOption
	{
		private String asString;
		private String asMessage;

		public MsgOption( String asString, String asMessage )
		{
			this.asString = asString;
			this.asMessage = asMessage;
		}

		@Override
		public String toString()
		{
			return this.asString;
		}

		public String toMessage()
		{
			return this.asMessage;
		}
	}

	public class SmashbotListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( KoLConstants.pulverizeQueue.isEmpty() )
			{
				InputFieldUtilities.alert( "No items in queue!" );
				return;
			}

			String message;
			if ( KoLmafia.isPlayerOnline( "smashbot" ) )
			{	// bot online
				if ( KoLCharacter.canInteract() )
				{
					message = "Smashbot is online, and ready to SMASH!";
				}
				else
				{
					InputFieldUtilities.alert( "Smashbot is online, but won't play with you in hardcore or ronin!" );
					return;
				}
			}
			else
			{	// bot offline
				if ( KoLCharacter.canInteract() )
				{
					message = "Smashbot is offline, so there will be a delay before you receive your smashed items.  Are you sure you want to continue?";
				}
				else
				{
					InputFieldUtilities.alert( "Smashbot won't play with you in hardcore or ronin.  Smashbot isn't online, anyway." );
					return;
				}
			}

			MsgOption selected = (MsgOption) InputFieldUtilities.input( message,
				new MsgOption[] {
					new MsgOption( "receive results as is", "" ),
					new MsgOption( "powders -> nuggets", "nuggets" ),
					new MsgOption( "also nuggets -> wads", "wads" ),
				}, null );
			if ( selected == null )
			{
				return;
			}

			AdventureResult[] items =
				new AdventureResult[ KoLConstants.pulverizeQueue.size() ];
			KoLConstants.pulverizeQueue.toArray( items );
			KoLConstants.pulverizeQueue.clear();
			LockableListModel inv = (LockableListModel)
				PulverizePanel.this.getElementList().getModel();
			inv.fireContentsChanged( inv, 0, inv.size() - 1 );
			SendMessageCommand.send( "smashbot", selected.toMessage(),
				items, false, true );
		}

		@Override
		public String toString()
		{
			return "send to smashbot";
		}
	}

	public class WadbotListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( KoLConstants.pulverizeQueue.isEmpty() )
			{
				InputFieldUtilities.alert( "No items in queue!" );
				return;
			}

			String message;
			if ( KoLmafia.isPlayerOnline( "wadbot" ) )
			{	// bot online
				if ( KoLCharacter.canInteract() )
				{
					message = "Wadbot is online, and ready to SMASH!";
				}
				else
				{
					message = "Wadbot is online, but you won't be able to receive your smashed items while you are under hardcore/Ronin restrictions.  Are you sure you want to continue?";
				}
			}
			else
			{	// bot offline
				if ( KoLCharacter.canInteract() )
				{
					message = "Wadbot is offline, so there will be a delay before you receive your smashed items.  Are you sure you want to continue?";
				}
				else
				{
					message = "You won't be able to receive your smashed items while you are under hardcore/Ronin restrictions.  Wadbot isn't online, anyway.  Proceed at your own risk.";
				}
			}

			MsgOption selected = (MsgOption) InputFieldUtilities.input( message,
				new MsgOption[] {
					new MsgOption( "receive results as is", "powders" ),
					new MsgOption( "powders -> nuggets", "nuggets" ),
					new MsgOption( "also nuggets -> wads", "wads" ),
				}, null );
			if ( selected == null )
			{
				return;
			}

			AdventureResult[] items =
				new AdventureResult[ KoLConstants.pulverizeQueue.size() ];
			KoLConstants.pulverizeQueue.toArray( items );
			KoLConstants.pulverizeQueue.clear();
			LockableListModel inv = (LockableListModel)
				PulverizePanel.this.getElementList().getModel();
			inv.fireContentsChanged( inv, 0, inv.size() - 1 );
			SendMessageCommand.send( "wadbot", selected.toMessage(),
				items, false, true );
		}

		@Override
		public String toString()
		{
			return "send to wadbot";
		}
	}
}
