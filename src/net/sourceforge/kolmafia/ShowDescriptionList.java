/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.Component;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.net.URLEncoder;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;
import net.sourceforge.kolmafia.StoreManager.SoldItem;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class ShowDescriptionList extends JList implements KoLConstants
{
	public int lastSelectIndex;
	public JPopupMenu contextMenu;
	public ListElementFilter filter;

	private LockableListModel listModel;
	private LockableListModel filterModel;

	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	public ShowDescriptionList( LockableListModel listModel )
	{	this( listModel, null, null, 4 );
	}

	public ShowDescriptionList( LockableListModel listModel, int visibleRowCount )
	{	this( listModel, null, null, visibleRowCount );
	}

	public ShowDescriptionList( LockableListModel listModel, LockableListModel filterModel, ListElementFilter filter )
	{	this( listModel, filterModel, filter, 4 );
	}

	public ShowDescriptionList( LockableListModel listModel, LockableListModel filterModel, ListElementFilter filter, int visibleRowCount )
	{
		contextMenu = new JPopupMenu();

		boolean isMoodList = listModel == MoodSettings.getTriggers();
		boolean isEncyclopedia = !listModel.isEmpty() && listModel.get(0) instanceof Entry;

		if ( !isMoodList )
		{
			if ( listModel.size() == 0 || !isEncyclopedia )
				contextMenu.add( new DescriptionMenuItem() );

			contextMenu.add( new WikiLookupMenuItem() );
		}

		if ( listModel == activeEffects )
		{
			contextMenu.add( new ShrugOffMenuItem() );
			contextMenu.add( new BoostEffectMenuItem() );
		}

		if ( listModel == usableSkills || listModel == availableSkills )
		{
			contextMenu.add( new CastSkillMenuItem() );
			contextMenu.add( new BoostSkillMenuItem() );
		}

		if ( listModel == junkItemList )
			contextMenu.add( new RemoveFromJunkListMenuItem() );

		if ( listModel == mementoList )
			contextMenu.add( new RemoveFromMementoListMenuItem() );

		if ( listModel == tally )
		{
			contextMenu.add( new ZeroTallyMenuItem() );
			contextMenu.add( new JSeparator() );
			contextMenu.add( new AutoSellMenuItem() );
			contextMenu.add( new ConsumeMenuItem() );
			contextMenu.add( new PulverizeMenuItem() );
		}
		else if ( listModel == inventory || listModel == closet || isEncyclopedia )
		{
			contextMenu.add( new AddToJunkListMenuItem() );
			contextMenu.add( new AddToMementoListMenuItem() );
		}
		else if ( listModel == StoreManager.getSortedSoldItemList() )
		{
			contextMenu.add( new AddToJunkListMenuItem() );
		}
		else if ( isMoodList )
		{
			contextMenu.add( new EditTriggerMenuItem() );
			contextMenu.add( new RemoveTriggerMenuItem() );
		}

		addMouseListener( new PopupListener() );
		addMouseListener( new ShowDescriptionAdapter() );

		this.listModel = filter == null ? listModel.getMirrorImage() : listModel.getMirrorImage( filter );
		setModel( this.listModel );

		setVisibleRowCount( visibleRowCount );
		setCellRenderer( AdventureResult.getDefaultRenderer() );
		setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" );
	}

	public void applyFilter( ListElementFilter filter )
	{
		this.filter = filter;
		listModel.applyListFilter( filter );
	}

	/**
	 * Shows and hides the applicable context menu item.  Actually
	 * all it does is show it -- the VM will handle hiding it.
	 */

	private class PopupListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{	maybeShowPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{	maybeShowPopup( e );
		}

		private void maybeShowPopup( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				int index = locationToIndex( e.getPoint() );
				lastSelectIndex = index;

				if ( !isSelectedIndex( index ) )
				{
					clearSelection();
					addSelectionInterval( index, index );
				}

				contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
			else
			{
				lastSelectIndex = -1;
			}
		}
    }

	public static void showGameDescription( Object item )
	{
		if ( item instanceof AdventureResult )
		{
			if ( ((AdventureResult)item).isItem() )
				FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((AdventureResult)item).getItemId() ) );
			if ( ((AdventureResult)item).isStatusEffect() )
				FightFrame.showLocation( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getEffectId( ((AdventureResult)item).getName() ) );
		}
		else if ( item instanceof ItemCreationRequest )
		{
			FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((ItemCreationRequest)item).getItemId() ) );
		}
		else if ( item instanceof Concoction )
		{
			FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((Concoction)item).getItemId() ) );
		}
		else if ( item instanceof UseSkillRequest )
		{
			FightFrame.showLocation( "desc_skill.php?whichskill=" + ((UseSkillRequest)item).getSkillId() );
		}
		else if ( item instanceof String )
		{
			Matcher playerMatcher = PLAYERID_MATCHER.matcher( (String) item );
			if ( playerMatcher.find() )
			{
				Object [] parameters = new Object [] { "#" + playerMatcher.group(1) };
				SwingUtilities.invokeLater( new CreateFrameRunnable( ProfileFrame.class, parameters ) );
			}
		}
	}

	public static void showWikiDescription( Object item )
	{
		String name = null;

		if ( item instanceof AdventureResult )
			name = ((AdventureResult)item).getName();
		else if ( item instanceof ItemCreationRequest )
			name = ((ItemCreationRequest)item).getName();
		else if ( item instanceof Concoction )
			name = ((AdventureResult)item).getName();
		else if ( item instanceof SoldItem )
			name = ((SoldItem) item).getItemName();
		else if ( item instanceof UseSkillRequest )
			name = ((UseSkillRequest) item).getSkillName();
		else if ( item instanceof String )
			name = (String) item;
		else if ( item instanceof Entry )
			name = (String) ((Entry)item).getValue();

		if ( name == null )
			return;

		try
		{
			name = URLEncoder.encode( name, "UTF-8" );
			StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Shows the description of the item which was recently
	 * double-clicked.
	 */

	private class ShowDescriptionAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			if ( e.getClickCount() == 2 )
			{
				int index = locationToIndex( e.getPoint() );
				Object item = getModel().getElementAt( index );

				if ( item == null )
					return;

				ensureIndexIsVisible( index );
				showGameDescription( item );
			}
		}
	}

	private abstract class ContextMenuItem extends ThreadedMenuItem
	{
		public int index;
		public Object item;

		public ContextMenuItem( String title )
		{	super( title );
		}

		public void run()
		{
			this.index = lastSelectIndex == -1 ? getSelectedIndex() : lastSelectIndex;
			this.item = ShowDescriptionList.this.getModel().getElementAt( index );

			if ( item == null )
				return;

			ensureIndexIsVisible( index );
			this.executeAction();
		}

		public abstract void executeAction();
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected.
	 */

	private class DescriptionMenuItem extends ContextMenuItem
	{
		public DescriptionMenuItem()
		{	super( "Game description" );
		}

		public void executeAction()
		{	showGameDescription( item );
		}
	}

	/**
	 * Utility class which shows the description of the item
	 * which is currently selected, as it appears on the wiki.
	 */

	private class WikiLookupMenuItem extends ContextMenuItem
	{
		public WikiLookupMenuItem()
		{	super( "Wiki description" );
		}

		public void executeAction()
		{	showWikiDescription( item );
		}
	}

	private class CastSkillMenuItem extends ContextMenuItem
	{
		public CastSkillMenuItem()
		{	super( "Cast the skill once" );
		}

		public void executeAction()
		{
			Object [] skills = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			UseSkillRequest request;

			for ( int i = 0; i < skills.length; ++i )
			{
				request = (UseSkillRequest) skills[i];

				request.setTarget( null );
				request.setBuffCount( 1 );

				RequestThread.postRequest( request );
			}
		}
	}

	private class BoostSkillMenuItem extends ContextMenuItem
	{
		public BoostSkillMenuItem()
		{	super( "Add to current mood" );
		}

		public void executeAction()
		{
			Object [] skills = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
				StaticEntity.setProperty( "currentMood", "default" );

			String name, action;

			for ( int i = 0; i < skills.length; ++i )
			{
				name = UneffectRequest.skillToEffect( ((UseSkillRequest) skills[i]).getSkillName() );

				action = MoodSettings.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
					MoodSettings.addTrigger( "lose_effect", name, action );
			}
		}
	}

	private class BoostEffectMenuItem extends ContextMenuItem
	{
		public BoostEffectMenuItem()
		{	super( "Add to current mood" );
		}

		public void executeAction()
		{
			Object [] effects = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
				StaticEntity.setProperty( "currentMood", "default" );

			String name, action;

			for ( int i = 0; i < effects.length; ++i )
			{
				name = ((AdventureResult) effects[i]).getName();

				action = MoodSettings.getDefaultAction( "lose_effect", name );
				if ( !action.equals( "" ) )
				{
					MoodSettings.addTrigger( "lose_effect", name, action );
					continue;
				}

				action = MoodSettings.getDefaultAction( "gain_effect", name );
				if ( !action.equals( "" ) )
					MoodSettings.addTrigger( "gain_effect", name, action );
			}
		}
	}

	private class ShrugOffMenuItem extends ContextMenuItem
	{
		public ShrugOffMenuItem()
		{	super( "Remove this effect" );
		}

		public void executeAction()
		{
			Object [] effects = getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[i] ) );
		}
	}

	private class EditTriggerMenuItem extends ContextMenuItem
	{
		public EditTriggerMenuItem()
		{	super( "Modify cast count" );
		}

		public void executeAction()
		{
			String desiredLevel = JOptionPane.showInputDialog( null, "Number of casts?", "1" );
			if ( desiredLevel == null )
				return;

			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			MoodSettings.addTriggers( items, StaticEntity.parseInt( desiredLevel ) );
			MoodSettings.saveSettings();
		}
	}

	private class RemoveTriggerMenuItem extends ContextMenuItem
	{
		public RemoveTriggerMenuItem()
		{	super( "Remove from mood" );
		}

		public void executeAction()
		{
			String desiredLevel = JOptionPane.showInputDialog( null, "Number of casts?", "1" );
			if ( desiredLevel == null )
				return;

			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			MoodSettings.removeTriggers( items );
			MoodSettings.saveSettings();
		}
	}

	private class AddToJunkListMenuItem extends ContextMenuItem
	{
		public AddToJunkListMenuItem()
		{	super( "Add to junk list" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[i] instanceof ItemCreationRequest )
					data = ((ItemCreationRequest)items[i]).createdItem;
				else if ( items[i] instanceof AdventureResult && ((AdventureResult)items[i]).isItem() )
					data = (AdventureResult) items[i];
				else if ( item instanceof SoldItem )
					data = new AdventureResult( ((SoldItem) item).getItemId(), 1 );
				else if ( items[i] instanceof String && TradeableItemDatabase.contains( (String) items[i] ) )
					data = new AdventureResult( (String) items[i], 1, false );
				else if ( items[i] instanceof Entry && TradeableItemDatabase.contains( (String) ((Entry)items[i]).getValue() ) )
					data = new AdventureResult( (String) ((Entry)items[i]).getValue(), 1, false );

				if ( data != null && !junkItemList.contains( data ) )
					junkItemList.add( data );
			}

			KoLSettings.saveFlaggedItemList();
			listModel.applyListFilter( filter );
		}
	}

	private class AddToMementoListMenuItem extends ContextMenuItem
	{
		public AddToMementoListMenuItem()
		{	super( "Add to memento list" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[i] instanceof ItemCreationRequest )
					data = ((ItemCreationRequest)items[i]).createdItem;
				else if ( items[i] instanceof AdventureResult && ((AdventureResult)items[i]).isItem() )
					data = (AdventureResult) items[i];
				else if ( items[i] instanceof String && TradeableItemDatabase.contains( (String) items[i] ) )
					data = new AdventureResult( (String) items[i], 1, false );
				else if ( items[i] instanceof Entry && TradeableItemDatabase.contains( (String) ((Entry)items[i]).getValue() ) )
					data = new AdventureResult( (String) ((Entry)items[i]).getValue(), 1, false );

				if ( data != null && !mementoList.contains( data ) )
					mementoList.add( data );
			}

			KoLSettings.saveFlaggedItemList();
		}
	}

	private class ZeroTallyMenuItem extends ContextMenuItem
	{
		public ZeroTallyMenuItem()
		{	super( "Zero out entries" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			for ( int i = 0; i < items.length; ++i )
				AdventureResult.addResultToList( tally, ((AdventureResult)items[i]).getNegation() );
		}
	}

	private class AutoSellMenuItem extends ContextMenuItem
	{
		public AutoSellMenuItem()
		{	super( "Autosell selected" );
		}

		public void executeAction()
		{
			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null, "Are you sure you would like to sell the selected items?", "Sell request nag screen!", JOptionPane.YES_NO_OPTION ) )
				return;

			RequestThread.postRequest( new AutoSellRequest( getSelectedValues(), AutoSellRequest.AUTOSELL ) );
		}
	}

	private class ConsumeMenuItem extends ContextMenuItem
	{
		public ConsumeMenuItem()
		{	super( "Consume selected" );
		}

		public void executeAction()
		{
			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null, "Are you sure you want to use the selected items?", "Use request nag screen!", JOptionPane.YES_NO_OPTION ) )
				return;

			Object [] items = getSelectedValues();

			RequestThread.openRequestSequence();
			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
			RequestThread.closeRequestSequence();
		}
	}

	private class PulverizeMenuItem extends ContextMenuItem
	{
		public PulverizeMenuItem()
		{	super( "Pulverize selected" );
		}

		public void executeAction()
		{
			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null, "The items you've selected will be smashed to pieces.  Are you sure?", "Pulverize request nag screen!", JOptionPane.YES_NO_OPTION ) )
				return;

			Object [] items = getSelectedValues();

			RequestThread.openRequestSequence();
			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[i] ) );
			RequestThread.closeRequestSequence();
		}
	}

	private class RemoveFromJunkListMenuItem extends ContextMenuItem
	{
		public RemoveFromJunkListMenuItem()
		{	super( "This is not junk" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
				junkItemList.remove( items[i] );

			KoLSettings.saveFlaggedItemList();
		}
	}

	private class RemoveFromMementoListMenuItem extends ContextMenuItem
	{
		public RemoveFromMementoListMenuItem()
		{	super( "This is not sacred" );
		}

		public void executeAction()
		{
			Object [] items = getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
				mementoList.remove( items[i] );

			KoLSettings.saveFlaggedItemList();
		}
	}
}
