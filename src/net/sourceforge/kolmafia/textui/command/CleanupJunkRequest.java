/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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

import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class CleanupJunkRequest
	extends AbstractCommand
{
	{
		this.usage = " - use, pulverize, or autosell your junk items.";
	}

	@Override
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

				if ( canUntinker && (ConcoctionDatabase.getMixingMethod( currentItem ) & KoLConstants.CT_MASK) == KoLConstants.COMBINE )
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
				case 621: // Warm Subject gift certificate
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
					RequestThread.postRequest( UseItemRequest.getInstance( currentItem.getInstance( itemCount ) ) );
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
			boolean hasMalusAccess = KoLCharacter.isMuscleClass() && !KoLCharacter.isAvatarOfBoris();

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
			RequestThread.postRequest( new AutoSellRequest( sellList.toArray() ) );
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
				RequestThread.postRequest( new AutoSellRequest( sellList.toArray() ) );
			}
		}
	}
}
