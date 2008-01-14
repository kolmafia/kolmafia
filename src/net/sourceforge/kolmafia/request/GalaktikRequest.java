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

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

public class GalaktikRequest
	extends GenericRequest
{
	public static final String HP = "curehp";
	public static final String MP = "curemp";

	private int restoreAmount;

	public GalaktikRequest( final String type )
	{
		this( type, 0 );
	}

	public GalaktikRequest( final String type, final int restoreAmount )
	{
		super( "galaktik.php" );

		this.addFormField( "pwd" );
		this.addFormField( "action", type );

		if ( restoreAmount > 0 )
		{
			this.restoreAmount = restoreAmount;
		}
		else if ( type.equals( GalaktikRequest.HP ) )
		{
			this.restoreAmount =
				Math.max( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() + restoreAmount, 0 );
		}
		else if ( type.equals( GalaktikRequest.MP ) )
		{
			this.restoreAmount =
				Math.max( KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() + restoreAmount, 0 );
		}
		else
		{
			this.restoreAmount = 0;
		}

		this.addFormField( "quantity", String.valueOf( this.restoreAmount ) );
	}

	public void run()
	{
		if ( this.restoreAmount == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE, "You don't need that cure." );
			return;
		}

		int originalAmount = KoLCharacter.getAvailableMeat();
		KoLmafia.updateDisplay( "Visiting Doc Galaktik..." );

		super.run();

		int finalCost = KoLCharacter.getAvailableMeat() - originalAmount;
		if ( finalCost != 0 )
		{
			AdventureResult.addResultToList( KoLConstants.tally, new AdventureResult( AdventureResult.MEAT, finalCost ) );
		}

	}

	public static final LockableListModel retrieveCures()
	{
		LockableListModel cures = new LockableListModel();

		if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
		{
			cures.add( "Restore all HP with Curative Nostrum" );
		}

		if ( KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
		{
			cures.add( "Restore all MP with Fizzy Invigorating Tonic" );
		}

		return cures;
	}

	public void processResults()
	{
		if ( this.responseText.indexOf( "You can't afford that" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't afford that cure." );
			return;
		}

		if ( !this.containsUpdate )
		{
			CharPaneRequest.getInstance().run();
		}

		KoLmafia.updateDisplay( "Cure purchased." );
	}
}
