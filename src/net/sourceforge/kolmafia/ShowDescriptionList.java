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

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.net.URLEncoder;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;
import net.sourceforge.kolmafia.StoreManager.SoldItem;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

public class ShowDescriptionList extends JList implements KoLConstants
{
	public int lastSelectIndex;
	public JPopupMenu contextMenu;
	public ListElementFilter filter;

	private LockableListModel displayModel, originalModel;
	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	public ShowDescriptionList( LockableListModel displayModel )
	{	this( displayModel, null, 4 );
	}

	public ShowDescriptionList( LockableListModel displayModel, int visibleRowCount )
	{	this( displayModel, null, visibleRowCount );
	}

	public ShowDescriptionList( LockableListModel displayModel, ListElementFilter filter )
	{	this( displayModel, filter, 4 );
	}

	public ShowDescriptionList( LockableListModel displayModel, ListElementFilter filter, int visibleRowCount )
	{
		this.contextMenu = new JPopupMenu();

		boolean isMoodList = displayModel == MoodSettings.getTriggers();
		boolean isEncyclopedia = !displayModel.isEmpty() && displayModel.get(0) instanceof Entry;

		if ( !isMoodList )
		{
			if ( displayModel.size() == 0 || !isEncyclopedia )
				this.contextMenu.add( new DescriptionMenuItem() );

			this.contextMenu.add( new WikiLookupMenuItem() );
		}

		if ( displayModel == activeEffects )
		{
			this.contextMenu.add( new ShrugOffMenuItem() );
			this.contextMenu.add( new BoostEffectMenuItem() );
		}

		if ( displayModel == usableSkills || displayModel == availableSkills )
		{
			this.contextMenu.add( new CastSkillMenuItem() );
			this.contextMenu.add( new BoostSkillMenuItem() );
		}

		if ( displayModel == tally )
		{
			this.contextMenu.add( new ZeroTallyMenuItem() );
			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new AddToJunkListMenuItem() );
			this.contextMenu.add( new AddToSingletonListMenuItem() );
			this.contextMenu.add( new AddToMementoListMenuItem() );

			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new AutoSellMenuItem() );
			this.contextMenu.add( new ConsumeMenuItem() );
			this.contextMenu.add( new PulverizeMenuItem() );
		}
		else if ( displayModel == inventory || displayModel == closet || isEncyclopedia )
		{
			this.contextMenu.add( new AddToJunkListMenuItem() );
			this.contextMenu.add( new AddToMementoListMenuItem() );
			this.contextMenu.add( new AddToSingletonListMenuItem() );
		}
		else if ( displayModel == junkList )
		{
			this.contextMenu.add( new AddToSingletonListMenuItem() );
		}
		else if ( isMoodList )
		{
			addKeyListener( new RemoveTriggerListener() );
		}

		this.addMouseListener( new PopupListener() );

		this.originalModel = displayModel;
		this.displayModel = filter == null ? displayModel.getMirrorImage() : displayModel.getMirrorImage( filter );
		this.setModel( this.displayModel );

		this.setVisibleRowCount( visibleRowCount );
		this.setCellRenderer( AdventureResult.getDefaultRenderer() );
		this.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" );
	}

	public LockableListModel getOriginalModel()
	{	return originalModel;
	}

	/**
	 * Shows and hides the applicable context menu item.  Actually
	 * all it does is show it -- the VM will handle hiding it.
	 */

	private class PopupListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{	this.maybeShowPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{	this.maybeShowPopup( e );
		}

		private void maybeShowPopup( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				int index = ShowDescriptionList.this.locationToIndex( e.getPoint() );
				ShowDescriptionList.this.lastSelectIndex = index;

				if ( !ShowDescriptionList.this.isSelectedIndex( index ) )
				{
					ShowDescriptionList.this.clearSelection();
					ShowDescriptionList.this.addSelectionInterval( index, index );
				}

				ShowDescriptionList.this.contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
			else
			{
				ShowDescriptionList.this.lastSelectIndex = -1;
			}
		}
	}

	public static final void showGameDescription( Object item )
	{
		if ( item instanceof AdventureResult )
		{
			if ( ((AdventureResult)item).isItem() )
				StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((AdventureResult)item).getItemId() ) );
			if ( ((AdventureResult)item).isStatusEffect() )
				StaticEntity.openRequestFrame( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getDescriptionId( StatusEffectDatabase.getEffectId( ((AdventureResult)item).getName() ) ) );
		}
		else if ( item instanceof ItemCreationRequest )
		{
			StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((ItemCreationRequest)item).getItemId() ) );
		}
		else if ( item instanceof Concoction )
		{
			StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((Concoction)item).getItemId() ) );
		}
		else if ( item instanceof UseSkillRequest )
		{
			StaticEntity.openRequestFrame( "desc_skill.php?whichskill=" + ((UseSkillRequest)item).getSkillId() );
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

	public static final void showWikiDescription( Object item )
	{
		if ( item == null )
			return;

		String name = null;
		boolean isEffect = item instanceof AdventureResult && ((AdventureResult)item).isStatusEffect();
		boolean isSkill = item instanceof UseSkillRequest;

		if ( item instanceof AdventureResult )
			name = ((AdventureResult)item).getName();
		else if ( isSkill )
			name = ((UseSkillRequest) item).getSkillName();
		else if ( item instanceof ItemCreationRequest )
			name = ((ItemCreationRequest)item).getName();
		else if ( item instanceof Concoction )
			name = ((Concoction)item).getName();
		else if ( item instanceof SoldItem )
			name = ((SoldItem) item).getItemName();
		else if ( item instanceof String )
			name = (String) item;
		else if ( item instanceof Entry )
			name = (String) ((Entry)item).getValue();

		if ( name == null )
			return;

		if ( isEffect && ClassSkillsDatabase.contains( name ) )
			name = name + " (effect)";
		else if ( isSkill && StatusEffectDatabase.contains( name ) )
			name = name + " (skill)";

		name = StaticEntity.globalStringReplace( name, " ", "_" );
		name = Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );

		StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/" + name );
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
			this.index = ShowDescriptionList.this.lastSelectIndex == -1 ? ShowDescriptionList.this.getSelectedIndex() : ShowDescriptionList.this.lastSelectIndex;
			this.item = displayModel.getElementAt( this.index );

			if ( this.item == null )
				return;

			ShowDescriptionList.this.ensureIndexIsVisible( this.index );
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
		{	showGameDescription( this.item );
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
		{	showWikiDescription( this.item );
		}
	}

	private class CastSkillMenuItem extends ContextMenuItem
	{
		public CastSkillMenuItem()
		{	super( "Cast the skill once" );
		}

		public void executeAction()
		{
			Object [] skills = ShowDescriptionList.this.getSelectedValues();
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
			Object [] skills = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			if ( KoLSettings.getUserProperty( "currentMood" ).equals( "apathetic" ) )
				KoLSettings.setUserProperty( "currentMood", "default" );

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
			Object [] effects = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			if ( KoLSettings.getUserProperty( "currentMood" ).equals( "apathetic" ) )
				KoLSettings.setUserProperty( "currentMood", "default" );

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
			Object [] effects = ShowDescriptionList.this.getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[i] ) );
		}
	}

	private class RemoveTriggerListener extends KeyAdapter
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.isConsumed() )
				return;

			if ( e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE )
				return;

			Object [] items = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			MoodSettings.removeTriggers( items );
			MoodSettings.saveSettings();
			e.consume();
		}
	}

	private class AddToJunkListMenuItem extends ContextMenuItem
	{
		public AddToJunkListMenuItem()
		{	super( "Add to junk list" );
		}

		public void executeAction()
		{
			Object [] items = ShowDescriptionList.this.getSelectedValues();
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

				if ( data == null )
					continue;

				if ( !junkList.contains( data ) )
					junkList.add( data );
			}
		}
	}

	private class AddToSingletonListMenuItem extends ContextMenuItem
	{
		public AddToSingletonListMenuItem()
		{	super( "Add to singleton list" );
		}

		public void executeAction()
		{
			Object [] items = ShowDescriptionList.this.getSelectedValues();
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

				if ( data == null )
					continue;

				if ( !junkList.contains( data ) )
					junkList.add( data );
				if ( !singletonList.contains( data ) )
					singletonList.add( data );
			}
		}
	}

	private class AddToMementoListMenuItem extends ContextMenuItem
	{
		public AddToMementoListMenuItem()
		{	super( "Add to memento list" );
		}

		public void executeAction()
		{
			Object [] items = ShowDescriptionList.this.getSelectedValues();
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

			KoLSettings.setUserProperty( "mementoListActive", "true" );
		}
	}

	private class ZeroTallyMenuItem extends ContextMenuItem
	{
		public ZeroTallyMenuItem()
		{	super( "Zero out entries" );
		}

		public void executeAction()
		{
			Object [] items = ShowDescriptionList.this.getSelectedValues();
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
			if ( !KoLFrame.confirm( "Are you sure you would like to sell the selected items?" ) )
				return;

			RequestThread.postRequest( new AutoSellRequest( ShowDescriptionList.this.getSelectedValues(), AutoSellRequest.AUTOSELL ) );
		}
	}

	private class ConsumeMenuItem extends ContextMenuItem
	{
		public ConsumeMenuItem()
		{	super( "Consume selected" );
		}

		public void executeAction()
		{
			if ( !KoLFrame.confirm( "Are you sure you want to use the selected items?" ) )
				return;

			Object [] items = ShowDescriptionList.this.getSelectedValues();

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
			if ( !KoLFrame.confirm( "The items you've selected will be smashed to pieces.  Are you sure?" ) )
				return;

			Object [] items = ShowDescriptionList.this.getSelectedValues();

			RequestThread.openRequestSequence();
			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[i] ) );
			RequestThread.closeRequestSequence();
		}
	}
}
