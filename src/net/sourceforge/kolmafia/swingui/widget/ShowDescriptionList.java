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

import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CafeDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.MallSearchFrame;
import net.sourceforge.kolmafia.swingui.ProfileFrame;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

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
			if ( displayModel.isEmpty() || !isEncyclopedia )
			{
				this.contextMenu.add( new ContextMenuItem( "Game description", new DescriptionRunnable() ) );
			}

			this.contextMenu.add( new ContextMenuItem( "Wiki description", new WikiLookupRunnable() ) );
		}

		if ( displayModel == MallSearchFrame.results )
		{
			this.contextMenu.add( new JSeparator() );
			this.contextMenu.add( new ContextMenuItem( "Go To Store...", new StoreLookupRunnable() ) );
		}

		if ( displayModel == KoLConstants.activeEffects )
		{
			this.contextMenu.add( new ContextMenuItem( "Remove this effect", new ShrugOffRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to current mood", new AddToMoodEffectRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Extend this effect", new ExtendEffectRunnable() ) );
		}

		if ( displayModel == KoLConstants.usableSkills || displayModel == KoLConstants.availableSkills )
		{
			this.contextMenu.add( new ContextMenuItem( "Cast the skill once", new CastSkillRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to current mood", new AddToMoodSkillRunnable() ) );
		}

		if ( displayModel == KoLConstants.tally )
		{
			this.contextMenu.add( new ContextMenuItem( "Zero out entries", new ZeroTallyRunnable() ) );
			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new ContextMenuItem( "Add to junk list", new AddToJunkListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to singleton list", new AddToSingletonListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to memento list", new AddToMementoListRunnable() ) );

			this.contextMenu.add( new JSeparator() );

			this.contextMenu.add( new ContextMenuItem( "Autosell selected", new AutoSellRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add selected to mall", new AutoMallRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Consume selected", new ConsumeRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Pulverize selected", new PulverizeRunnable() ) );
		}
		else if ( displayModel == KoLConstants.inventory || displayModel == KoLConstants.closet || isEncyclopedia )
		{
			this.contextMenu.add( new ContextMenuItem( "Add to junk list", new AddToJunkListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to singleton list", new AddToSingletonListRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Add to memento list", new AddToMementoListRunnable() ) );
		}
		else if ( isMoodList )
		{
			this.contextMenu.add( new ContextMenuItem( "Force execution", new ForceExecuteRunnable() ) );
			this.contextMenu.add( new ContextMenuItem( "Remove selected", new RemoveTriggerRunnable() ) );

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

	public AdventureResult[] getSelectedItems()
	{
		// Obviously, this only works if the model contains AdventureResults
		Object[] values = this.getSelectedValuesList().toArray();
		AdventureResult[] result = new AdventureResult[ values.length ];
		for ( int i = 0; i < values.length; ++i )
		{
			result[ i ] = (AdventureResult)values[ i ];
		}
		return result;
	}

	public PurchaseRequest[] getSelectedPurchases()
	{
		// Obviously, this only works if the model contains PurchaseRequests
		Object[] values = this.getSelectedValuesList().toArray();
		PurchaseRequest[] result = new PurchaseRequest[ values.length ];
		for ( int i = 0; i < values.length; ++i )
		{
			result[ i ] = (PurchaseRequest)values[ i ];
		}
		return result;
	}

	/**
	 * Shows and hides the applicable context menu item. Actually all it does is show it -- the VM will handle hiding
	 * it.
	 */

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

	public static final void showGameDescription( Object item )
	{
		if ( item instanceof Boost )
		{
			item = ((Boost) item).getItem();
		}

		if ( item instanceof AdventureResult )
		{
			AdventureResult ar = (AdventureResult) item;
			if ( ar.isItem() )
			{
				int itemId = ar.getItemId();
				String descId =
					( itemId != -1 ) ?
					ItemDatabase.getDescriptionId( itemId ) :
					CafeDatabase.nameToDescId( ar.getName() );
				StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + descId );
			}
			if ( ar.isStatusEffect() )
			{
				String descId = EffectDatabase.getDescriptionId( EffectDatabase.getEffectId( ar.getName() ) );
				StaticEntity.openDescriptionFrame( "desc_effect.php?whicheffect=" + descId );
			}
		}
		else if ( item instanceof Concoction )
		{
			Concoction c = (Concoction) item;
			int itemId = c.getItemId();
			String descId =
				( itemId != -1 ) ?
				ItemDatabase.getDescriptionId( itemId ) :
				CafeDatabase.nameToDescId( c.getName() );
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + descId );
		}
		else if ( item instanceof QueuedConcoction )
		{
			QueuedConcoction c = (QueuedConcoction) item;
			int itemId = c.getItemId();
			String descId =
				( itemId != -1 ) ?
				ItemDatabase.getDescriptionId( itemId ) :
				CafeDatabase.nameToDescId( c.getName() );
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + descId );
		}
		else if ( item instanceof CreateItemRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (CreateItemRequest) item ).getItemId() ) );
		}
		else if ( item instanceof PurchaseRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId( ( (PurchaseRequest) item ).getItemId() ) );
		}
		else if ( item instanceof UseSkillRequest )
		{
			StaticEntity.openDescriptionFrame( "desc_skill.php?whichskill=" + ( (UseSkillRequest) item ).getSkillId() + "&self=true" );
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

	public static void showMallStore( Object item )
	{
		if ( item instanceof PurchaseRequest )
		{
			RelayLoader.openSystemBrowser( "mallstore.php?whichstore=" +
				( (PurchaseRequest) item ).getFormField( "whichstore" ) );
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
			this.index = ShowDescriptionList.this.lastSelectIndex == -1 ? ShowDescriptionList.this.getSelectedIndex() : ShowDescriptionList.this.lastSelectIndex;

			this.item = ShowDescriptionList.this.displayModel.getElementAt( this.index );

			if ( this.item == null )
			{
				return;
			}

			ShowDescriptionList.this.ensureIndexIsVisible( this.index );

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
			ShowDescriptionList.showGameDescription( this.item );
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
			WikiUtilities.showWikiDescription( this.item );
		}
	}

	public class StoreLookupRunnable
		extends ContextMenuListener
	{
		@Override
		protected void executeAction()
		{
			ShowDescriptionList.showMallStore( this.item );
		}
	}

	public void removeTriggers()
	{
		Object[] items = ShowDescriptionList.this.getSelectedValuesList().toArray();
		ShowDescriptionList.this.clearSelection();

		MoodManager.removeTriggers( items );
		MoodManager.saveSettings();
	}

	private class ForceExecuteRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] items = ShowDescriptionList.this.getSelectedValuesList().toArray();
			ShowDescriptionList.this.clearSelection();

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
			ShowDescriptionList.this.removeTriggers();
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

			ShowDescriptionList.this.removeTriggers();
			e.consume();
		}
	}

	private class CastSkillRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] skills = ShowDescriptionList.this.getSelectedValuesList().toArray();
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

	private class AddToMoodSkillRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] skills = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
			MoodManager.saveSettings();
		}
	}

	private class AddToMoodEffectRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] effects = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
			MoodManager.saveSettings();
		}
	}

	private class ExtendEffectRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			Object[] effects = ShowDescriptionList.this.getSelectedValuesList().toArray();
			ShowDescriptionList.this.clearSelection();

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
			Object[] effects = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
			Object[] items = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
					int itemId = ItemDatabase.getItemId( (String) items[ i ] );
					data = ItemPool.get( itemId );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					int itemId = ItemDatabase.getItemId( (String) ( (Entry) items[ i ] ).getValue() );
					data = ItemPool.get( itemId );
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
			Object[] items = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
					int itemId = ItemDatabase.getItemId( (String) items[ i ] );
					data = ItemPool.get( itemId );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					int itemId = ItemDatabase.getItemId( (String) ( (Entry) items[ i ] ).getValue() );
					data = ItemPool.get( itemId );
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
			Object[] items = ShowDescriptionList.this.getSelectedValuesList().toArray();
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
					int itemId = ItemDatabase.getItemId( (String) items[ i ] );
					data = ItemPool.get( itemId );
				}
				else if ( items[ i ] instanceof Entry && ItemDatabase.contains( (String) ( (Entry) items[ i ] ).getValue() ) )
				{
					int itemId = ItemDatabase.getItemId( (String) ( (Entry) items[ i ] ).getValue() );
					data = ItemPool.get( itemId );
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
			AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult.addResultToList( KoLConstants.tally, items[ i ].getNegation() );
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

			AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
			RequestThread.postRequest( new AutoSellRequest( items ) );
		}
	}

	private class AutoMallRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "Are you sure you would like to add the selected items to your store?" ) )
			{
				return;
			}

			AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
			RequestThread.postRequest( new AutoMallRequest( items ) );
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

			AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( items[ i ] ) );
			}
		}
	}

	private class PulverizeRunnable
		extends ContextMenuListener
	{
		@Override
		public void executeAction()
		{
			if ( !InputFieldUtilities.confirm( "The items you've selected will be smashed to pieces.  Are you sure?" ) )
			{
				return;
			}

			AdventureResult[] items = ShowDescriptionList.this.getSelectedItems();
			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( items[ i ] ) );
			}
		}
	}
}
