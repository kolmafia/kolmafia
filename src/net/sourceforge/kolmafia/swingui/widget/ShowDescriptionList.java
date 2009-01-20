/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.MoodManager.MoodTrigger;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.swingui.ProfileFrame;
import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShowDescriptionList
	extends JList
{
	public int lastSelectIndex;
	public JPopupMenu contextMenu;
	public ListElementFilter filter;

	private final LockableListModel displayModel, originalModel;
	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );

	public ShowDescriptionList( final LockableListModel displayModel )
	{
		this( displayModel, null, 4 );
	}

	public ShowDescriptionList( final LockableListModel displayModel, final int visibleRowCount )
	{
		this( displayModel, null, visibleRowCount );
	}

	public ShowDescriptionList( final LockableListModel displayModel, final ListElementFilter filter )
	{
		this( displayModel, filter, 4 );
	}

	public ShowDescriptionList( final LockableListModel displayModel, final ListElementFilter filter,
		final int visibleRowCount )
	{
		this.contextMenu = new JPopupMenu();

		boolean isMoodList = displayModel == MoodManager.getTriggers();
		boolean isEncyclopedia = !displayModel.isEmpty() && displayModel.get( 0 ) instanceof Entry;

		if ( !isMoodList )
		{
			if ( displayModel.size() == 0 || !isEncyclopedia )
			{
				this.contextMenu.add( new DescriptionMenuItem() );
			}

			this.contextMenu.add( new WikiLookupMenuItem() );
		}

		if ( displayModel == KoLConstants.activeEffects )
		{
			this.contextMenu.add( new ShrugOffMenuItem() );
			this.contextMenu.add( new BoostEffectMenuItem() );
		}

		if ( displayModel == KoLConstants.usableSkills || displayModel == KoLConstants.availableSkills )
		{
			this.contextMenu.add( new CastSkillMenuItem() );
			this.contextMenu.add( new BoostSkillMenuItem() );
		}

		if ( displayModel == KoLConstants.tally )
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
		else if ( displayModel == KoLConstants.inventory || displayModel == KoLConstants.closet || isEncyclopedia )
		{
			this.contextMenu.add( new AddToJunkListMenuItem() );
			this.contextMenu.add( new AddToMementoListMenuItem() );
			this.contextMenu.add( new AddToSingletonListMenuItem() );
		}
		else if ( isMoodList )
		{
			this.contextMenu.add( new ForceExecuteMenuItem() );
			this.contextMenu.add( new RemoveTriggerMenuItem() );

			this.addKeyListener( new RemoveTriggerListener() );
		}

		this.addMouseListener( new PopupListener() );

		this.originalModel = displayModel;
		this.displayModel = filter == null ? displayModel.getMirrorImage() : displayModel.getMirrorImage( filter );
		this.setModel( this.displayModel );

		this.setVisibleRowCount( visibleRowCount );
		this.setCellRenderer( ListCellRendererFactory.getDefaultRenderer() );
		this.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" );
	}

	public LockableListModel getOriginalModel()
	{
		return this.originalModel;
	}

	/**
	 * Shows and hides the applicable context menu item. Actually all it does is show it -- the VM will handle hiding
	 * it.
	 */

	private class PopupListener
		extends MouseAdapter
	{
		public void mousePressed( final MouseEvent e )
		{
			this.maybeShowPopup( e );
		}

		public void mouseReleased( final MouseEvent e )
		{
			this.maybeShowPopup( e );
		}

		private void maybeShowPopup( final MouseEvent e )
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
		}
	}

	public static final void showGameDescription( final Object item )
	{
		if ( item instanceof AdventureResult )
		{
			if ( ( (AdventureResult) item ).isItem() )
			{
				StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (AdventureResult) item ).getItemId() ) );
			}
			if ( ( (AdventureResult) item ).isStatusEffect() )
			{
				StaticEntity.openRequestFrame( "desc_effect.php?whicheffect=" + EffectDatabase.getDescriptionId( EffectDatabase.getEffectId( ( (AdventureResult) item ).getName() ) ) );
			}
		}
		else if ( item instanceof Concoction )
		{
			StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (Concoction) item ).getItemId() ) );
		}
		else if ( item instanceof CreateItemRequest )
		{
			StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (CreateItemRequest) item ).getItemId() ) );
		}
		else if ( item instanceof MallPurchaseRequest )
		{
			StaticEntity.openRequestFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (MallPurchaseRequest) item ).getItemId() ) );
		}
		else if ( item instanceof UseSkillRequest )
		{
			StaticEntity.openRequestFrame( "desc_skill.php?whichskill=" + ( (UseSkillRequest) item ).getSkillId() );
		}
		else if ( item instanceof String )
		{
			Matcher playerMatcher = ShowDescriptionList.PLAYERID_MATCHER.matcher( (String) item );
			if ( playerMatcher.find() )
			{
				Object[] parameters = new Object[] { "#" + playerMatcher.group( 1 ) };
				SwingUtilities.invokeLater( new CreateFrameRunnable( ProfileFrame.class, parameters ) );
			}
		}
	}

	public static final String getWikiLocation( final Object item )
	{
		if ( item == null )
		{
			return null;
		}

		String name = null;
		
		boolean isItem = false;
		boolean isEffect = false;
		boolean isSkill = false;
				
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
		else if ( item instanceof MallPurchaseRequest )
		{
			name = ( (MallPurchaseRequest) item ).getItemName();
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
		return "http://kol.coldfront.net/thekolwiki/index.php/" + name;
	}

	public static final void showWikiDescription( final Object item )
	{
		String location = ShowDescriptionList.getWikiLocation( item );

		if ( location != null )
		{
			StaticEntity.openSystemBrowser( location );
		}
	}

	private abstract class ContextMenuItem
		extends ThreadedMenuItem
	{
		public int index;
		public Object item;

		public ContextMenuItem( final String title )
		{
			super( title );
		}

		public void run()
		{
			this.index =
				ShowDescriptionList.this.lastSelectIndex == -1 ? ShowDescriptionList.this.getSelectedIndex() : ShowDescriptionList.this.lastSelectIndex;

			this.item = ShowDescriptionList.this.displayModel.getElementAt( this.index );

			if ( this.item == null )
			{
				return;
			}

			ShowDescriptionList.this.ensureIndexIsVisible( this.index );
			this.executeAction();
		}

		public abstract void executeAction();
	}

	/**
	 * Utility class which shows the description of the item which is currently selected.
	 */

	private class DescriptionMenuItem
		extends ContextMenuItem
	{
		public DescriptionMenuItem()
		{
			super( "Game description" );
		}

		public void executeAction()
		{
			ShowDescriptionList.showGameDescription( this.item );
		}
	}

	/**
	 * Utility class which shows the description of the item which is currently selected, as it appears on the wiki.
	 */

	private class WikiLookupMenuItem
		extends ContextMenuItem
	{
		public WikiLookupMenuItem()
		{
			super( "Wiki description" );
		}

		public void executeAction()
		{
			ShowDescriptionList.showWikiDescription( this.item );
		}
	}

	public void removeTriggers()
	{
		Object[] items = ShowDescriptionList.this.getSelectedValues();
		ShowDescriptionList.this.clearSelection();

		MoodManager.removeTriggers( items );
		MoodManager.saveSettings();
	}

	private class ForceExecuteMenuItem
		extends ContextMenuItem
	{
		public ForceExecuteMenuItem()
		{
			super( "Force execution" );
		}

		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			for ( int i = 0; i < items.length; ++i )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( ( (MoodTrigger) items[ i ] ).getAction() );
			}
		}
	}

	private class RemoveTriggerMenuItem
		extends ContextMenuItem
	{
		public RemoveTriggerMenuItem()
		{
			super( "Remove selected" );
		}

		public void executeAction()
		{
			ShowDescriptionList.this.removeTriggers();
		}
	}

	private class RemoveTriggerListener
		extends KeyAdapter
	{
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

			ShowDescriptionList.this.removeTriggers();
			e.consume();
		}
	}

	private class CastSkillMenuItem
		extends ContextMenuItem
	{
		public CastSkillMenuItem()
		{
			super( "Cast the skill once" );
		}

		public void executeAction()
		{
			Object[] skills = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

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

	private class BoostSkillMenuItem
		extends ContextMenuItem
	{
		public BoostSkillMenuItem()
		{
			super( "Add to current mood" );
		}

		public void executeAction()
		{
			Object[] skills = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

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
		}
	}

	private class BoostEffectMenuItem
		extends ContextMenuItem
	{
		public BoostEffectMenuItem()
		{
			super( "Add to current mood" );
		}

		public void executeAction()
		{
			Object[] effects = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

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
		}
	}

	private class ShrugOffMenuItem
		extends ContextMenuItem
	{
		public ShrugOffMenuItem()
		{
			super( "Remove this effect" );
		}

		public void executeAction()
		{
			Object[] effects = ShowDescriptionList.this.getSelectedValues();
			for ( int i = 0; i < effects.length; ++i )
			{
				RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[ i ] ) );
			}
		}
	}

	private class AddToJunkListMenuItem
		extends ContextMenuItem
	{
		public AddToJunkListMenuItem()
		{
			super( "Add to junk list" );
		}

		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult && ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1, false );
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

	private class AddToSingletonListMenuItem
		extends ContextMenuItem
	{
		public AddToSingletonListMenuItem()
		{
			super( "Add to singleton list" );
		}

		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult && ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1, false );
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

	private class AddToMementoListMenuItem
		extends ContextMenuItem
	{
		public AddToMementoListMenuItem()
		{
			super( "Add to memento list" );
		}

		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValues();
			ShowDescriptionList.this.clearSelection();

			AdventureResult data;

			for ( int i = 0; i < items.length; ++i )
			{
				data = null;

				if ( items[ i ] instanceof CreateItemRequest )
				{
					data = ( (CreateItemRequest) items[ i ] ).createdItem;
				}
				else if ( items[ i ] instanceof AdventureResult && ( (AdventureResult) items[ i ] ).isItem() )
				{
					data = (AdventureResult) items[ i ];
				}
				else if ( items[ i ] instanceof String && ItemDatabase.contains( (String) items[ i ] ) )
				{
					data = new AdventureResult( (String) items[ i ], 1, false );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					data = new AdventureResult( (String) ( (Entry) items[ i ] ).getValue(), 1, false );
				}

				if ( data != null && !KoLConstants.mementoList.contains( data ) )
				{
					KoLConstants.mementoList.add( data );
				}
			}

			Preferences.setBoolean( "mementoListActive", true );
		}
	}

	private class ZeroTallyMenuItem
		extends ContextMenuItem
	{
		public ZeroTallyMenuItem()
		{
			super( "Zero out entries" );
		}

		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValues();
			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult.addResultToList( KoLConstants.tally, ( (AdventureResult) items[ i ] ).getNegation() );
			}
		}
	}

	private class AutoSellMenuItem
		extends ContextMenuItem
	{
		public AutoSellMenuItem()
		{
			super( "Autosell selected" );
		}

		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "Are you sure you would like to sell the selected items?" ) )
			{
				return;
			}

			RequestThread.postRequest( new SellStuffRequest(
				ShowDescriptionList.this.getSelectedValues(), SellStuffRequest.AUTOSELL ) );
		}
	}

	private class ConsumeMenuItem
		extends ContextMenuItem
	{
		public ConsumeMenuItem()
		{
			super( "Consume selected" );
		}

		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "Are you sure you want to use the selected items?" ) )
			{
				return;
			}

			Object[] items = ShowDescriptionList.this.getSelectedValues();

			RequestThread.openRequestSequence();
			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new UseItemRequest( (AdventureResult) items[ i ] ) );
			}
			RequestThread.closeRequestSequence();
		}
	}

	private class PulverizeMenuItem
		extends ContextMenuItem
	{
		public PulverizeMenuItem()
		{
			super( "Pulverize selected" );
		}

		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "The items you've selected will be smashed to pieces.  Are you sure?" ) )
			{
				return;
			}

			Object[] items = ShowDescriptionList.this.getSelectedValues();

			RequestThread.openRequestSequence();
			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[ i ] ) );
			}
			RequestThread.closeRequestSequence();
		}
	}
}
