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

	public FamiliarRequest()
	{
		super( "familiar.php" );
		this.changeTo = null;
		this.item = null;
	}

	public FamiliarRequest( final FamiliarData changeTo )
	{
		super( "familiar.php" );

		this.changeTo = changeTo == null ? FamiliarData.NO_FAMILIAR : changeTo;
		this.item = null;

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

		this.addFormField( "pwd" );
		this.addFormField( "action", "equip" );
		this.addFormField( "whichfam", String.valueOf( familiar.getId() ) );
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	public String getFamiliarChange()
	{
		return this.changeTo == null ? null : this.changeTo.toString();
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		if ( this.item != null )
		{
			KoLmafia.updateDisplay( "Equipping " + this.changeTo.getName() + " the " + this.changeTo.getRace() + " with " + this.item.getName() + "..." );
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

		// If you're not equipping a familiar, or your old familiar
		// wasn't wearing something, or your new familiar can't equip
		// the old item, then do nothing further.

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			return;
		}

		if ( this.changeTo == FamiliarData.NO_FAMILIAR || this.changeTo.getId() == 59 )
		{
			return;
		}

		AdventureResult item = familiar.getItem();

		if ( item == EquipmentRequest.UNEQUIP || !this.changeTo.getItem().equals( EquipmentRequest.UNEQUIP ) || !this.changeTo.canEquip( item ) )
		{
			return;
		}

		// In all other cases, a switch is probably in order.  Go ahead
		// and make the item switch.

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

		Matcher familiarMatcher = FamiliarRequest.EQUIP_PATTERN.matcher( urlString );
		if ( familiarMatcher.find() )
		{
			FamiliarData changeTo = new FamiliarData( StringUtilities.parseInt( familiarMatcher.group( 1 ) ) );

			// Special handling for the blackbird.  If
			// the blackbird is equipped, then cache your
			// earlier familiar so that as soon as you use
			// the map, KoLmafia knows to change it back.

			if ( changeTo.getId() == 59 )
			{
				Preferences.setString( "preBlackbirdFamiliar", KoLCharacter.getFamiliar().getRace() );
			}

			int index = KoLCharacter.getFamiliarList().indexOf( changeTo );

			if ( index != -1 )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliarList().get( index ).toString() );
				return true;
			}
		}

		return false;
	}

	private static final void equipFamiliar( final int familiarId, final int itemId )
	{
		FamiliarData[] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
		KoLCharacter.getFamiliarList().toArray( familiars );

		for ( int i = 0; i < familiars.length; ++i )
		{
			if ( familiars[ i ].getId() == familiarId )
			{
				FamiliarData familiar = familiars[ i ];
				AdventureResult item = ItemPool.get( itemId, 1 );

				familiar.setItem( item );

				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Equip " + familiar.getRace() + " with " + item.getName() );

				return;
			}
		}
	}

	private static final void unequipFamiliar( final int familiarId )
	{
		FamiliarData[] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
		KoLCharacter.getFamiliarList().toArray( familiars );

		for ( int i = 0; i < familiars.length; ++i )
		{
			if ( familiars[ i ].getId() == familiarId )
			{
				FamiliarData familiar = familiars[ i ];
				AdventureResult item = familiar.getItem();
				if ( item != EquipmentRequest.UNEQUIP )
				{
					familiar.setItem( EquipmentRequest.UNEQUIP );
					AdventureResult.addResultToList( KoLConstants.inventory, item );
				}

				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Unequip " + familiar.getRace() );

				return;
			}
		}
	}
}
