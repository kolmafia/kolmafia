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

public class BountyHunterRequest extends KoLRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "<b>([^<]*?)</b></td><td>" );

	private int itemId;
	private boolean isExchange;
	private AdventureResult itemTraded;

	public BountyHunterRequest()
	{
		super( "town_wrong.php" );
		addFormField( "place", "bountyhunter" );
		this.isExchange = false;
	}

	public BountyHunterRequest( int itemId )
	{
		super( "town_wrong.php" );
		addFormField( "place", "bountyhunter" );
		addFormField( "action", "bsellall" );
		addFormField( "what", String.valueOf( itemId ) );

		this.itemId = itemId;
		this.isExchange = true;

		itemTraded = new AdventureResult( itemId, 0 );
		itemTraded = itemTraded.getInstance( itemTraded.getCount( inventory ) );
	}

	public void run()
	{
		if ( isExchange )
		{
			if ( itemTraded.getCount() == 0 )
				return;

			KoLmafia.updateDisplay( "Hunting rabbits (or something)..." );
		}

		super.run();
	}

	protected void processResults()
	{
		if ( isExchange )
		{
			StaticEntity.getClient().processResult( itemTraded.getNegation() );
			KoLmafia.updateDisplay( "Items successfully sold to hunter." );
			return;
		}

		Matcher exchangeMatcher = ITEM_PATTERN.matcher( responseText );

		hunterItems.clear();
		while ( exchangeMatcher.find() )
			hunterItems.add( exchangeMatcher.group(1) );
	}
}