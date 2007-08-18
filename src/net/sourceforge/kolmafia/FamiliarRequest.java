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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FamiliarRequest extends KoLRequest
{
	private static final Pattern UNEQUIP_PATTERN = Pattern.compile( "famid=(\\d+)" );
	private static final Pattern EQUIP_PATTERN = Pattern.compile( "newfam=(\\d+)" );
	private FamiliarData changeTo;

	public FamiliarRequest()
	{
		super( "familiar.php" );
		this.changeTo = null;
	}

	public FamiliarRequest( FamiliarData changeTo )
	{
		super( "familiar.php" );

		if ( changeTo == FamiliarData.NO_FAMILIAR )
		{
			this.addFormField( "action", "putback" );
		}
		else
		{
			this.addFormField( "action", "newfam" );
			this.addFormField( "newfam", String.valueOf( changeTo.getId() ) );
		}

		this.changeTo = changeTo;
	}

	public String getFamiliarChange()
	{	return this.changeTo == null ? null : this.changeTo.toString();
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void run()
	{
		FamiliarData familiar = KoLCharacter.getFamiliar();
		AdventureResult item = familiar != null ? familiar.getItem() : EquipmentRequest.UNEQUIP;

		if ( this.changeTo == null )
		{
			KoLmafia.updateDisplay( "Retrieving familiar data..." );
		}
		else
		{
			if ( familiar.getId() == this.changeTo.getId() )
				return;

			if ( familiar != FamiliarData.NO_FAMILIAR )
				KoLmafia.updateDisplay( "Putting " + familiar.getName() + " the " + familiar.getRace() + " back into terrarium..." );

			if ( this.changeTo != FamiliarData.NO_FAMILIAR )
				KoLmafia.updateDisplay( "Taking " + this.changeTo.getName() + " the " + this.changeTo.getRace() + " out of terrarium..." );
		}

		super.run();

		// If you're not equipping a familiar, or your old familiar wasn't
		// wearing something, or your new familiar can't equip the old item,
		// then do nothing further.

		if ( familiar == null || familiar == FamiliarData.NO_FAMILIAR )
			return;

		if ( this.changeTo == null || this.changeTo == FamiliarData.NO_FAMILIAR || this.changeTo.getId() == 59 )
			return;

		if ( item == EquipmentRequest.UNEQUIP || !this.changeTo.getItem().equals( EquipmentRequest.UNEQUIP ) || !this.changeTo.canEquip( item ) )
			return;

		// In all other cases, a switch is probably in order.  Go ahead and make
		// the item switch.

		KoLmafia.updateDisplay( familiar.getItem().getName() + " is better than " + this.changeTo.getItem().getName() + ".  Switching items..." );
		(new EquipmentRequest( item, KoLCharacter.FAMILIAR )).run();
	}

	public void processResults()
	{
		FamiliarData.registerFamiliarData( this.responseText );
		if ( this.changeTo == null )
			KoLmafia.updateDisplay( "Familiar data retrieved." );

		if ( KoLCharacter.getFamiliar() == null || KoLCharacter.getFamiliar() == FamiliarData.NO_FAMILIAR )
			KoLCharacter.setEquipment( KoLCharacter.FAMILIAR, EquipmentRequest.UNEQUIP );
	}

	public static final boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "familiar.php?" ) )
			return false;

		if ( urlString.indexOf( "action=putback" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar none" );
			return true;
		}

		if ( urlString.indexOf( "action=unequip" ) != -1 )
		{
			Matcher familiarMatcher = UNEQUIP_PATTERN.matcher( urlString );
			if ( !familiarMatcher.find() )
				return true;

			FamiliarData [] familiars = new FamiliarData[ KoLCharacter.getFamiliarList().size() ];
			KoLCharacter.getFamiliarList().toArray( familiars );

			int id = StaticEntity.parseInt( familiarMatcher.group(1) );
			for ( int i = 0; i < familiars.length; ++i )
			{
				if ( familiars[i].getId() == id )
				{
					AdventureResult item = familiars[i].getItem();
					if ( item != null )
					{
						familiars[i].setItem( EquipmentRequest.UNEQUIP );
						AdventureResult.addResultToList( inventory, item );
					}

					return true;
				}
			}

			return true;
		}

		Matcher familiarMatcher = EQUIP_PATTERN.matcher( urlString );
		if ( familiarMatcher.find() )
		{
			FamiliarData changeTo = new FamiliarData( StaticEntity.parseInt( familiarMatcher.group(1) ) );

			// Special handling for the blackbird.  If
			// the blackbird is equipped, then cache your
			// earlier familiar so that as soon as you use
			// the map, KoLmafia knows to change it back.

			if ( changeTo.getId() == 59 )
				KoLSettings.setUserProperty( "preBlackbirdFamiliar", KoLCharacter.getFamiliar().getRace() );

			int index = KoLCharacter.getFamiliarList().indexOf( changeTo );

			if ( index != -1 )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliarList().get(index).toString() );
				return true;
			}
		}

		return false;
	}
}
