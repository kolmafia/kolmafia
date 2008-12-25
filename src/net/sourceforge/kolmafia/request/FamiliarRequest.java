/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarRequest
	extends GenericRequest
{
	private static final Pattern UNEQUIP_PATTERN = Pattern.compile( "famid=(\\d+)" );
	private static final Pattern EQUIP_PATTERN = Pattern.compile( "newfam=(\\d+)" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichfam=(\\d+).*whichitem=(\\d+)" );

	private final FamiliarData changeTo;
	private final AdventureResult item;
	private final boolean locking;

	public FamiliarRequest()
	{
		super( "familiar.php" );
		this.changeTo = null;
		this.item = null;
		this.locking = false;
	}

	public FamiliarRequest( final FamiliarData changeTo )
	{
		super( "familiar.php" );

		this.changeTo = changeTo == null ? FamiliarData.NO_FAMILIAR : changeTo;
		this.item = null;
		this.locking = false;

		if ( this.changeTo == FamiliarData.NO_FAMILIAR )
		{
			this.addFormField( "action", "putback" );
		}
		else
		{
			this.addFormField( "action", "newfam" );
			this.addFormField( "newfam", String.valueOf( this.changeTo.getId() ) );
		}
	}

	public FamiliarRequest( final FamiliarData familiar, final AdventureResult item )
	{
		super( "familiar.php" );

		this.changeTo = familiar;
		this.item = item;
		this.locking = false;

		this.addFormField( "action", "equip" );
		this.addFormField( "whichfam", String.valueOf( familiar.getId() ) );
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	public FamiliarRequest( boolean locking )
	{
		super( "familiar.php" );
		this.addFormField( "action", "lockequip" );
		this.changeTo = null;
		this.item = null;
		this.locking = true;
	}

	public String getFamiliarChange()
	{
		return this.changeTo == null ? null : this.changeTo.toString();
	}

	protected boolean retryOnTimeout()
	{
		return !this.locking;
	}

	public void run()
	{
		if ( this.item != null )
		{
			KoLmafia.updateDisplay( "Equipping " + this.changeTo.getName() + " the " + this.changeTo.getRace() + " with " + this.item.getName() + "..." );
			super.run();
			return;
		}

		if ( this.locking )
		{
			String verb = EquipmentManager.familiarItemLocked() ? "Unlocking" : "Locking";
			KoLmafia.updateDisplay( verb + " familiar item..." );
			super.run();
			return;
		}

		if ( this.changeTo == null )
		{
			KoLmafia.updateDisplay( "Retrieving familiar data..." );
			super.run();
			return;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar.getId() == this.changeTo.getId() )
		{
			return;
		}

		if ( familiar != FamiliarData.NO_FAMILIAR )
		{
			KoLmafia.updateDisplay( "Putting " + familiar.getName() + " the " + familiar.getRace() + " back into terrarium..." );
		}

		if ( this.changeTo != FamiliarData.NO_FAMILIAR )
		{
			KoLmafia.updateDisplay( "Taking " + this.changeTo.getName() + " the " + this.changeTo.getRace() + " out of terrarium..." );
		}

		super.run();

		EquipmentManager.updateEquipmentList( EquipmentManager.FAMILIAR );

		// If we didn't have a familiar before or don't have one now,
		// leave equipment alone.

		if ( familiar == FamiliarData.NO_FAMILIAR || this.changeTo == FamiliarData.NO_FAMILIAR)
		{
			return;
		}

		// If we are switching to certain specialized familiars, don't
		// steal any equipment from the old familiar

		switch ( this.changeTo.getId() )
		{
		case 54:	// Comma Chameleon
		case 59:	// Reassembled Blackbird
		case 82:	// Mad Hatrack
		case 92:	// Disembodied Hand
			return;
		}

		AdventureResult item = familiar.getItem();

		// If the old familiar wasn't wearing equipment, nothing to
		// steal.

		if ( item == EquipmentRequest.UNEQUIP )
		{
			return;
		}

		// If KoL itself switched equipment because it was locked,
		// remove it from the old familiar and add it to the new one.

		if ( item == EquipmentManager.lockedFamiliarItem() )
		{
			FamiliarRequest.unequipFamiliar( familiar.getId() );
			if ( this.changeTo.canEquip( item ) )
			{
				FamiliarRequest.equipFamiliar( this.changeTo.getId(), item.getItemId() );
			}
			return;
		}

		// If the new familiar already has an item, leave it alone.

		if ( !this.changeTo.getItem().equals( EquipmentRequest.UNEQUIP ) )
		{
			return;
		}

		// If the new familiar can't equip the old item, don't steal

		if ( !this.changeTo.canEquip( item ) )
		{
			return;
		}

		// It's probably worthwhile to transfer the item from the old
		// familiar to the new one. Do so.

		KoLmafia.updateDisplay( familiar.getItem().getName() + " is better than " + this.changeTo.getItem().getName() + ".  Switching items..." );
		( new EquipmentRequest( item, EquipmentManager.FAMILIAR ) ).run();
	}

	public void processResults()
	{
		FamiliarData.registerFamiliarData( this.responseText );

		if ( this.item != null )
		{
			KoLmafia.updateDisplay( "Familiar equipped." );
		}
		else if ( this.locking )
		{
                        String locked = EquipmentManager.familiarItemLocked() ? "locked" : "unlocked";
			KoLmafia.updateDisplay( "Familiar item " + locked + "." );
		}
		else if ( this.changeTo == null )
		{
			KoLmafia.updateDisplay( "Familiar data retrieved." );
		}

		if ( KoLCharacter.getFamiliar() == null || KoLCharacter.getFamiliar() == FamiliarData.NO_FAMILIAR )
		{
			EquipmentManager.setEquipment( EquipmentManager.FAMILIAR, EquipmentRequest.UNEQUIP );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "familiar.php?" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=putback" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar none" );
			return true;
		}

		if ( urlString.indexOf( "action=equip" ) != -1 )
		{
			Matcher familiarMatcher = FamiliarRequest.ITEM_PATTERN.matcher( urlString );
			if ( !familiarMatcher.find() )
			{
				return true;
			}

			int familiarId = StringUtilities.parseInt( familiarMatcher.group(1) );
			int itemId = StringUtilities.parseInt( familiarMatcher.group(2) );
			FamiliarRequest.equipFamiliar( familiarId, itemId );

			return true;
		}

		if ( urlString.indexOf( "action=unequip" ) != -1 )
		{
			Matcher familiarMatcher = FamiliarRequest.UNEQUIP_PATTERN.matcher( urlString );
			if ( !familiarMatcher.find() )
			{
				return true;
			}

			int familiarId = StringUtilities.parseInt( familiarMatcher.group(1) );
			FamiliarRequest.unequipFamiliar( familiarId );

			return true;
		}

		if ( urlString.indexOf( "action=lockequip" ) != -1 )
		{
			if ( EquipmentManager.familiarItemLockable() )
			{
				String verb = EquipmentManager.familiarItemLocked() ? "unlock" : "lock";
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "familiar " + verb );
				if ( urlString.indexOf( "frominv=" ) != -1 )
				{	// If the lock icon is clicked from the Equipment page (rather than
					// the Familiars page), the results are shown via a redirect that we
					// don't follow.  Must change the lock state here, instead.
					EquipmentManager.lockFamiliarItem(
						!EquipmentManager.familiarItemLocked() );
				}
			}
			return true;
		}

		Matcher familiarMatcher = FamiliarRequest.EQUIP_PATTERN.matcher( urlString );
		if ( !familiarMatcher.find() )
		{
			return true;
		}

		int id = StringUtilities.parseInt( familiarMatcher.group( 1 ) );
		FamiliarData changeTo = KoLCharacter.findFamiliar( id );

		// If we don't actually have the new familiar, nothing to do.

		if ( changeTo == null )
		{
			return true;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();

		// Special handling for the blackbird. If the blackbird is
		// equipped, then cache your earlier familiar so that as soon
		// as you use the map, KoLmafia knows to change it back.

		if ( id == 59 )
		{
			Preferences.setString( "preBlackbirdFamiliar", familiar.getRace() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "familiar " + changeTo.toString() );

		// If we have a familiar item locked and the new familiar can
		// equip it, it will be automatically transferred, so move it
		// from the old familiar to the new familiar now

		AdventureResult lockedItem = EquipmentManager.lockedFamiliarItem();

		if ( lockedItem != EquipmentRequest.UNEQUIP )
		{
                        if ( changeTo.canEquip( lockedItem ) )
                        {
                                FamiliarRequest.unequipFamiliar( familiar.getId() );
                                FamiliarRequest.equipFamiliar( changeTo.getId(), lockedItem.getItemId() );
                                EquipmentManager.lockFamiliarItem( changeTo );
                        }
                        else
                        {
                                EquipmentManager.lockFamiliarItem( false );
                        }
		}

		// If we're not going to see the familiar page, change to the
		// new familiar here.

		if ( urlString.indexOf( "ajax=" ) != -1 )
		{
			KoLCharacter.setFamiliar( changeTo );
		}

		return true;
	}

	private static final void equipFamiliar( final int familiarId, final int itemId )
	{
		FamiliarData familiar = KoLCharacter.findFamiliar( familiarId );

		if ( familiar != null )
		{
			FamiliarRequest.equipFamiliar( familiar, itemId );
		}
	}

	private static final void equipFamiliar( FamiliarData familiar, final int itemId )
	{
		AdventureResult item = ItemPool.get( itemId, 1 );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Equip " + familiar.getRace() + " with " + item.getName() );

		familiar.setItem( item );
	}

	public static final void equipCurrentFamiliar( final int itemId )
	{
		FamiliarRequest.equipFamiliar( KoLCharacter.getFamiliar(), itemId );
	}

	private static final void unequipFamiliar( final int familiarId )
	{
		FamiliarData familiar = KoLCharacter.findFamiliar( familiarId );
		if ( familiar != null )
		{
			FamiliarRequest.unequipFamiliar( familiar );
		}
	}

	private static final void unequipFamiliar( FamiliarData familiar )
	{
		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Unequip " + familiar.getRace() );
		familiar.setItem( EquipmentRequest.UNEQUIP );
	}

	public static final void unequipCurrentFamiliar()
	{
		FamiliarRequest.unequipFamiliar( KoLCharacter.getFamiliar() );
	}
}
