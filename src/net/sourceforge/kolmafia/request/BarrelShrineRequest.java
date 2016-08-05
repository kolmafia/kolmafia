/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.preferences.Preferences;

public class BarrelShrineRequest
	extends CreateItemRequest
{
	public BarrelShrineRequest( final Concoction conc )
	{
		super( "choice.php", conc );
	}

	public static boolean availableBarrelItem( final String itemName )
	{
		if ( Preferences.getBoolean( "barrelShrineUnlocked" ) )
		{
			if ( itemName.equals( "barrel lid" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForProtection" ) )
			{
				return true;
			}
			if ( itemName.equals( "barrel hoop earring" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForGlamour" ) )
			{
				return true;
			}
			if ( itemName.equals( "bankruptcy barrel" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForVigor" ) )
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0 )
		{
			return;
		}

		String creation = this.getName();
		String output = null;

		if ( creation.equals( "barrel lid" ) )
		{
			output = "choice.php?whichchoice=1100&option=1";
		}
		else if ( creation.equals( "barrel hoop earring" ) )
		{
			output = "choice.php?whichchoice=1100&option=2";
		}
		else if ( creation.equals( "bankruptcy barrel" ) )
		{
			output = "choice.php?whichchoice=1100&option=3";
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot create " + creation );
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + creation + "..." );

		while ( this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue() )
		{
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );
			GenericRequest request = new GenericRequest( "da.php?barrelshrine=1" ) ;
			RequestThread.postRequest( request );
			request.constructURLString( output );
			RequestThread.postRequest( request );
			int createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;

			if ( createdQuantity == 0 )
			{
				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				}

				return;
			}

			KoLmafia.updateDisplay( "Successfully created " + creation + " (" + createdQuantity + ")" );
			this.quantityNeeded -= createdQuantity;
		}
	}
}
