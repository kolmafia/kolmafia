/**
 * 
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class CleanupJunkRequest
	extends AbstractCommand
{
	{
		this.usage = " - use, pulverize, or autosell your junk items.";
	}

	public void run( final String cmd, final String parameters )
	{
		CleanupJunkRequest.cleanup();
	}

	public static void cleanup()
	{
		int itemCount;
		AdventureResult currentItem;

		Object[] items = KoLConstants.junkList.toArray();

		// Before doing anything else, go through the list of items
		// which are traditionally used and use them. Also, if the item
		// can be untinkered, it's usually more beneficial to untinker
		// first.

		boolean madeUntinkerRequest = false;
		boolean canUntinker = UntinkerRequest.canUntinker();

		RequestThread.openRequestSequence();
		ArrayList closetList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( !KoLConstants.singletonList.contains( items[ i ] ) || KoLConstants.closet.contains( items[ i ] ) )
			{
				continue;
			}

			if ( KoLConstants.inventory.contains( items[ i ] ) )
			{
				closetList.add( ( (AdventureResult) items[ i ] ).getInstance( 1 ) );
			}
		}

		if ( closetList.size() > 0 )
		{
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, closetList.toArray() ) );
		}

		do
		{
			madeUntinkerRequest = false;

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];
				itemCount = currentItem.getCount( KoLConstants.inventory );

				if ( itemCount == 0 )
				{
					continue;
				}

				if ( canUntinker && ConcoctionDatabase.getMixingMethod( currentItem ) == KoLConstants.COMBINE )
				{
					RequestThread.postRequest( new UntinkerRequest( currentItem.getItemId() ) );
					madeUntinkerRequest = true;
					continue;
				}

				switch ( currentItem.getItemId() )
				{
				case 184: // briefcase
				case 533: // Gnollish toolbox
				case 553: // 31337 scroll
				case 604: // Penultimate fantasy chest
				case 831: // small box
				case 832: // large box
				case 1768: // Gnomish toolbox
				case 1917: // old leather wallet
				case 1918: // old coin purse
				case 2057: // black pension check
				case 2058: // black picnic basket
				case 2511: // Frat Army FGF
				case 2512: // Hippy Army MPE
				case 2536: // canopic jar
				case 2612: // ancient vinyl coin purse
					RequestThread.postRequest( new UseItemRequest( currentItem.getInstance( itemCount ) ) );
					break;

				case 621: // Warm Subject gift certificate
					RequestThread.postRequest( new UseItemRequest( currentItem.getInstance( itemCount ) ) );
					break;
				}
			}
		}
		while ( madeUntinkerRequest );

		// Now you've got all the items used up, go ahead and prepare to
		// pulverize strong equipment.

		int itemPower;

		if ( KoLCharacter.hasSkill( "Pulverize" ) )
		{
			boolean hasMalusAccess = KoLCharacter.isMuscleClass();

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];

				if ( KoLConstants.mementoList.contains( currentItem ) )
				{
					continue;
				}

				if ( currentItem.getName().startsWith( "antique" ) )
				{
					continue;
				}

				itemCount = currentItem.getCount( KoLConstants.inventory );
				itemPower = EquipmentDatabase.getPower( currentItem.getItemId() );

				if ( itemCount > 0 && !NPCStoreDatabase.contains( currentItem.getName(), false ) )
				{
					switch ( ItemDatabase.getConsumptionType( currentItem.getItemId() ) )
					{
					case KoLConstants.EQUIP_HAT:
					case KoLConstants.EQUIP_PANTS:
					case KoLConstants.EQUIP_SHIRT:
					case KoLConstants.EQUIP_WEAPON:
					case KoLConstants.EQUIP_OFFHAND:

						if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) && itemPower >= 100 || hasMalusAccess && itemPower > 10 )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					case KoLConstants.EQUIP_FAMILIAR:
					case KoLConstants.EQUIP_ACCESSORY:

						if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					default:

						if ( currentItem.getName().endsWith( "powder" ) || currentItem.getName().endsWith( "nuggets" ) )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;
					}
				}
			}
		}

		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		ArrayList sellList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[ i ];

			if ( KoLConstants.mementoList.contains( currentItem ) )
			{
				continue;
			}

			if ( currentItem.getItemId() == ItemPool.MEAT_PASTE )
			{
				continue;
			}

			itemCount = currentItem.getCount( KoLConstants.inventory );
			if ( itemCount > 0 )
			{
				sellList.add( currentItem.getInstance( itemCount ) );
			}
		}

		if ( !sellList.isEmpty() )
		{
			RequestThread.postRequest( new SellStuffRequest( sellList.toArray(), SellStuffRequest.AUTOSELL ) );
			sellList.clear();
		}

		if ( !KoLCharacter.canInteract() )
		{
			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];

				if ( KoLConstants.mementoList.contains( currentItem ) )
				{
					continue;
				}

				if ( currentItem.getItemId() == ItemPool.MEAT_PASTE )
				{
					continue;
				}

				itemCount = currentItem.getCount( KoLConstants.inventory ) - 1;
				if ( itemCount > 0 )
				{
					sellList.add( currentItem.getInstance( itemCount ) );
				}
			}

			if ( !sellList.isEmpty() )
			{
				RequestThread.postRequest( new SellStuffRequest( sellList.toArray(), SellStuffRequest.AUTOSELL ) );
			}
		}

		RequestThread.closeRequestSequence();
	}
}