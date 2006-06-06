/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;

public class GalaktikRequest extends KoLRequest
{
	public static final int HP = 1;
	public static final int MP = 2;

	private int price;
	private int type;

	public GalaktikRequest( KoLmafia client )
	{
		super( client, "galaktik.php" );
		this.price = -1;
	}

	public GalaktikRequest( KoLmafia client, int type )
	{
		super( client, "galaktik.php" );

		this.type = type;
		switch ( type )
		{
		case HP:
			addFormField( "action", "curehp" );
			addFormField( "pwd" );
			this.price =  ( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() ) * 10;
			break;

		case MP:
			addFormField( "action", "curemp" );
			addFormField( "pwd" );
			this.price =  ( KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() ) * 20;
			break;

		default:
			this.price = 0;
			break;
		}
	}

	public void run()
	{
		if ( price == 0 )
		{
			KoLmafia.updateDisplay( CONTINUE_STATE, "You don't need that cure." );
			return;
		}

		if ( price > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You need " + ( price - KoLCharacter.getAvailableMeat() ) + " more meat." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting Doc Galaktik..." );
		super.run();
	}

	public static List retrieveCures( KoLmafia client )
	{
		LockableListModel cures = new LockableListModel();

		int currentHP = KoLCharacter.getCurrentHP();
		int maxHP = KoLCharacter.getMaximumHP();

		if ( currentHP < maxHP )
			cures.add( "Restore all HP for " + ( maxHP - currentHP ) * 10 + " Meat" );

		int currentMP = KoLCharacter.getCurrentMP();
		int maxMP = KoLCharacter.getMaximumMP();

		if ( currentMP < maxMP )
			cures.add( "Restore all MP for " + ( maxMP - currentMP ) * 20 + " Meat" );

		return cures;
	}

	protected void processResults()
	{
		if ( responseText.indexOf( "You can't afford that" ) != -1 )
		{
			// This will only happen if we didn't track HP/MP
			// correctly.

			KoLmafia.updateDisplay( ERROR_STATE, "You can't afford that cure." );
			return;
		}

		client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - price ) );

		if ( type == HP )
			client.processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );
		else
			client.processResult( new AdventureResult( AdventureResult.MP, KoLCharacter.getMaximumMP() ) );

		super.processResults();
		KoLmafia.updateDisplay( "Cure purchased." );
	}

	public String getCommandForm( int iterations )
	{	return "galaktik " + (type == HP ? "hp" : "mp");
	}
}
