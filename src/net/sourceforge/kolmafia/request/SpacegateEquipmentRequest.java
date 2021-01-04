/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public class SpacegateEquipmentRequest
	extends CreateItemRequest
{
	public SpacegateEquipmentRequest( final Concoction conc )
	{
		super( "choice.php", conc );
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

		if ( creation.equals( "filter helmet" ) )
		{
			output = "choice.php?whichchoice=1233&option=1";
		}
		else if ( creation.equals( "exo-servo leg braces" ) )
		{
			output = "choice.php?whichchoice=1233&option=2";
		}
		else if ( creation.equals( "rad cloak" ) )
		{
			output = "choice.php?whichchoice=1233&option=3";
		}
		else if ( creation.equals( "gate transceiver" ) )
		{
			output = "choice.php?whichchoice=1233&option=4";
		}
		else if ( creation.equals( "high-friction boots" ) )
		{
			output = "choice.php?whichchoice=1233&option=5";
		}
		else if ( creation.equals( "geological sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=6";
		}
		else if ( creation.equals( "botanical sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=7";
		}
		else if ( creation.equals( "zoological sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=8";
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot create " + creation );
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + creation + "..." );

		while ( this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue() )
		{
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );
			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=spacegate&action=sg_requisition" ) );
			RequestThread.postRequest( new GenericRequest( output ) );
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
			ConcoctionDatabase.refreshConcoctionsNow();
		}
	}
}
