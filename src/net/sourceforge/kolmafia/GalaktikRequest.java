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

public class GalaktikRequest extends KoLRequest
{
	private boolean isPurchase;
	private int price;
	private String cure;

	public GalaktikRequest( KoLmafia client )
	{
		super( client, "galaktik.php" );
		this.isPurchase = false;
	}

	public GalaktikRequest( KoLmafia client, String name )
	{
		super( client, "galaktik.php" );
		this.isPurchase = true;

		// Parse string to determine which cure he's buying and how
		// much it costs.

		// "Restore all HP for 400 Meat"
		// "Restore all MP for 3,520 Meat"
		Matcher cureMatcher = Pattern.compile( "Restore all (..) for ([0123456789,]*) Meat" ).matcher( name );

		if ( !cureMatcher.find( 0 ) )
			return;

		this.price = 0;
		try
		{
			this.price = df.parse( cureMatcher.group(2) ).intValue();
			cure = cureMatcher.group(1);
			// HP Cure: action=curehp
			// MP Cure: action=curemp
			addFormField( "action", "cure" + cure.toLowerCase() );
			addFormField( "pwd", client.getPasswordHash() );
		}
		catch ( Exception e )
		{
			// Should never come here
		}
	}

	public void run()
	{
		if ( !isPurchase )
		{
			// Assuming KoLmafia accurately tracks HP & MP,
			// we can derive the prices for the cures

			// retrieveCures();

			client.getGalaktikCures().clear();

			int currentHP = KoLCharacter.getCurrentHP();
			int maxHP = KoLCharacter.getMaximumHP();

			if ( currentHP < maxHP )
				client.getGalaktikCures().add( "Restore all HP for " + ( maxHP - currentHP ) * 10 + " Meat" );

			int currentMP = KoLCharacter.getCurrentMP();
			int maxMP = KoLCharacter.getMaximumMP();

			if ( currentMP < maxMP )
				client.getGalaktikCures().add( "Restore all MP for " + ( maxMP - currentMP ) * 20 + " Meat" );

			updateDisplay( ENABLED_STATE, "Cures retrieved." );

			return;
		}

		if ( price == 0 )
		{
			client.updateDisplay( ERROR_STATE, "You don't need that cure." );
			client.cancelRequest();
			return;
		}

		client.updateDisplay( DISABLED_STATE, "Visiting Doc Galaktik..." );

		super.run();

		client.processResult( new AdventureResult( AdventureResult.MEAT, 0 - price ) );

		if ( cure.equals( "HP" ) )
			client.processResult( new AdventureResult( AdventureResult.HP, KoLCharacter.getMaximumHP() ) );
		else
			client.processResult( new AdventureResult( AdventureResult.MP, KoLCharacter.getMaximumMP() ) );

		updateDisplay( ENABLED_STATE, "Cure purchased." );
	}

	private void retrieveCures()
	{
		// This method visits Doc Galaktik to find what he offers.

		client.updateDisplay( DISABLED_STATE, "Visiting Doc Galaktik..." );

		super.run();

		int lastMatchIndex = 0;
		Matcher cureMatcher = Pattern.compile( "Restore all .. for [0123456789,]* Meat" ).matcher( responseText );

		client.getGalaktikCures().clear();
		while ( cureMatcher.find( lastMatchIndex ) )
		{
			lastMatchIndex = cureMatcher.end();
			client.getGalaktikCures().add( cureMatcher.group(0) );
		}
	}
}
