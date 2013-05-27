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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FoldItemCommand
	extends AbstractCommand
{
	public FoldItemCommand()
	{
		this.usage = "[?] <item> - produce item by using another form, repeated as needed.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		// Determine which item to create
		AdventureResult target = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
		if ( target == null )
		{
			return;
		}

		// If we already have the item in inventory, we're done
		if ( target.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		// Find the fold group containing this item
		String targetName = target.getName();
		String canon = StringUtilities.getCanonicalName( targetName );
		ArrayList group = ItemDatabase.getFoldGroup( targetName );
		if ( group == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That's not a transformable item!" );
			return;
		}

		// Confirm that we'll be able to make this item
		boolean canShirt = KoLCharacter.hasSkill( "Torso Awaregness" );
		if ( !canShirt && EquipmentDatabase.isShirt( target ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't make a shirt" );
			return;
		}

		boolean canStaff =
			KoLCharacter.hasSkill( "Spirit of Rigatoni" ) ||
			KoLCharacter.isJarlsberg() ||
			( KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) &&
			  KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPECIAL_SAUCE_GLOVE, 1 ) ) );
		if ( !canStaff && EquipmentDatabase.isChefStaff( target ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't make a chefstaff" );
			return;
		}

		// Locate the item in the fold group
		int count = group.size();
		int targetIndex = 0;
		for ( int i = 1; i < count; ++i )
		{
			String form = (String) group.get( i );
			if ( form.equals( canon ) )
			{
				targetIndex = i;
				break;
			}
		}

		// Sanity check
		if ( targetIndex == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Internal error: cannot find " + targetName + " in fold group" );
			return;
		}

		// Iterate backwards to find closest item to transform. Skip
		// index 0.
		int sourceIndex = ( targetIndex > 1 ) ? targetIndex - 1 : count - 1;
		AdventureResult source = null;
		AdventureResult worn = null;
		int wornIndex = 0;
		int slot = EquipmentManager.NONE;
		boolean multiple = false;

		while ( sourceIndex != targetIndex )
		{
			String form = (String) group.get( sourceIndex );
			AdventureResult item = new AdventureResult( form, 1, false );

			// If we have this item in inventory, use it
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				source = item;
				break;
			}
			
			// If we have this item equipped, remember where
			int where = KoLCharacter.equipmentSlot( item );
			if ( where != EquipmentManager.NONE )
			{
				if ( worn == null )
				{
					worn = item;
					wornIndex = sourceIndex;
					slot = where;
				}
				else
				{
					multiple = true;
				}
			}

			// Consider the next item. Skip index 0.
			sourceIndex = sourceIndex > 1 ? sourceIndex - 1 : count - 1;
		}

		// If a Boris's Helm is equipped, twist it regardless of whether or not
		// they have one in inventory, since this is probably what the user wants.
		if ( targetName.startsWith( "Boris's Helm" ) && slot != EquipmentManager.NONE )
		{
			StringBuilder buf = new StringBuilder();
			buf.append( "inventory.php?action=twisthorns&slot=" );
			buf.append( slot == EquipmentManager.HAT ? "hat" : "familiarequip" );
			buf.append( "&pwd=" );
			buf.append( GenericRequest.passwordHash );

			GenericRequest request = new GenericRequest( buf.toString(), false );
			RequestThread.postRequest( request );
			return;
		}

		// If a Jarlsberg's pan is equipped, shake it regardless of whether or not
		// they have one in inventory, since this is probably what the user wants.
		if ( targetName.startsWith( "Jarlsberg's pan" ) && slot != EquipmentManager.NONE )
		{
			GenericRequest request = new GenericRequest( "inventory.php?action=shakepan" );
			RequestThread.postRequest( request );
			return;
		}

		// track the equipment slot if Loathing Legion gear
		// is being folded without being unequipped
		int legionSlot = -1;
		
		// If nothing in inventory is foldable, consider equipment
		if ( source == null )
		{
			// Too many choices. Let player decide which one
			if ( multiple )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unequip the item you want to fold into that." );
				return;
			}
			if ( worn == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have anything transformable into that item!" );
				return;
			}
			// If we are switching a Loathing Legion item into another item that goes in the same slot
			// then we don't need to unequip it first
			if ( targetName.startsWith( "Loathing Legion" ) &&
				 ( ItemDatabase.getConsumptionType( target.getItemId() ) == ItemDatabase.getConsumptionType( worn.getItemId() ) ) )
			{
				legionSlot = KoLCharacter.equipmentSlot( worn );
			}
			else
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, slot ) );
			}
			source = worn;
			sourceIndex = wornIndex;
		}

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( source + " => " + target );
			return;
		}
		
		if ( targetName.startsWith( "Loathing Legion" ) )
		{
			StringBuilder buf = new StringBuilder();
			buf.append( "inv_use.php?pwd=" );
			buf.append( GenericRequest.passwordHash );
			buf.append( "&switch=1" );
			buf.append( "&whichitem=" );
			buf.append( Integer.toString( source.getItemId() ) );
			buf.append( "&fold=" );
			buf.append( StringUtilities.getURLEncode( targetName ) );
			if ( legionSlot != -1 )
			{
				buf.append( "&eq=" );
				buf.append( EquipmentRequest.phpSlotNames[ legionSlot ] );
			}

			GenericRequest request = new GenericRequest( buf.toString(), false );
			RequestThread.postRequest( request );
			if ( legionSlot != -1 )
			{
				EquipmentManager.setEquipment( legionSlot, target );
				KoLCharacter.recalculateAdjustments();
				KoLCharacter.updateStatus();
				RequestLogger.printLine( "Now wearing " + targetName );
			}
			return;
		}

		int damage = ( (Integer) group.get( 0 ) ).intValue();
		damage = damage == 0 ? 0 : KoLCharacter.getMaximumHP() * damage / 100 + 2;

		// Fold repeatedly until target is obtained
		while ( sourceIndex != targetIndex )
		{
			String form = (String) group.get( sourceIndex );
			AdventureResult item = new AdventureResult( form, 1, false );

			// Consider the next item. Skip index 0.
			sourceIndex = ( sourceIndex < count - 1 ) ? sourceIndex + 1 : 1;

			// If we don't have this item in inventory,  skip
			if ( item.getCount( KoLConstants.inventory ) == 0 )
			{
				continue;
			}

			int hp = KoLCharacter.getCurrentHP();
			if ( hp > 0 && hp < damage )
			{
				RecoveryManager.recoverHP( damage );
			}

			RequestThread.postRequest( UseItemRequest.getInstance( item ) );
		}
	}
}
