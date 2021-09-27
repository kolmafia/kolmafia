package net.sourceforge.kolmafia.session;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;

public abstract class FamiliarManager
{
	public static void equipAllFamiliars()
	{
		KoLmafia.updateDisplay( "Equipping familiars..." );

		FamiliarData current = KoLCharacter.getFamiliar();

		boolean useCloset = Preferences.getBoolean( "autoSatisfyWithCloset" );
		boolean useStorage = KoLCharacter.canInteract();

		AdventureResultArray closetItems = new AdventureResultArray();
		AdventureResultArray storageItems = new AdventureResultArray();
		ArrayList<GenericRequest> requests = new ArrayList<GenericRequest>();

		for ( FamiliarData familiar : KoLCharacter.getFamiliarList() )
		{
			int itemId = FamiliarDatabase.getFamiliarItemId( familiar.getId() );

			// If this familiar has no specific item of its own, skip it
			if (itemId == -1 )
			{
				continue;
			}

			// If this familiar is already wearing its item, skip it
			AdventureResult currentItem = familiar.getItem();
			if ( currentItem.getItemId() == itemId )
			{
				continue;
			}

			AdventureResult item = ItemPool.get( itemId, 1 );
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				// Use one from inventory
			}
			else if ( useCloset && item.getCount( KoLConstants.closet ) > 0 )
			{
				// Use one from the closet
				closetItems.add( item );
			}
			else if ( useStorage && item.getCount( KoLConstants.storage ) > 0 )
			{
				// Use one from storage
				storageItems.add( item );
			}
			else
			{
				continue;
			}

			GenericRequest req =
				familiar.equals( current ) ?
				new EquipmentRequest( item ) :
				new FamiliarRequest( familiar, item );

			requests.add( req );
		}

		// If nothing to do, do nothing!

		if ( requests.size() == 0 )
		{
			return;
		}

		// Pull all items that are in storage but not inventory or the closet
		if ( storageItems.size() > 0 )
		{
			RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, storageItems.toArray(), true ) );
		}

		// If you do a "pull all", some items can end up in the
		// closet. Can that happen with a normal "pull"?
		//
		// Just in case, if the item is now in the closet but not
		// inventory, add it to closetItems

		for ( AdventureResult item : storageItems )
		{
			if ( item.getCount( KoLConstants.inventory ) == 0 &&
			     item.getCount( KoLConstants.closet ) > 0 )
			{
				// Use one from the closet
				closetItems.add( item );
			}
		}

		// Move all items that are in the closet into inventory
		if ( closetItems.size() > 0 )
		{
			// *** We'd like to do this transfer without adding the
			// *** items to the session tally
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, closetItems.toArray() ) );
		}

		// Equip all familiars with equipment from inventory
		for ( GenericRequest request : requests )
		{
			RequestThread.postRequest( request );
		}

		// Leave original familiar as current familiar
		RequestThread.postRequest( new FamiliarRequest( current ) );

		KoLmafia.updateDisplay( "Familiars equipped." );
	}
}
