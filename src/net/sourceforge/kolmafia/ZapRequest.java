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

public class ZapRequest extends KoLRequest
{
	private AdventureResult wand;
	private AdventureResult item;

	public ZapRequest( AdventureResult wand, AdventureResult item )
	{
		super( "wand.php" );
		addFormField( "action", "zap" );

		this.wand = wand;
		addFormField( "whichwand", String.valueOf( wand.getItemId() ) );

		this.item = item;
		addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	public void run()
	{
		if ( !inventory.contains( wand ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a " + wand.getName() + "." );
			return;
		}

		if ( !inventory.contains( item ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a " + item.getName() + "." );
			return;
		}

		KoLmafia.updateDisplay( "Zapping " + item.getName() + "..." );
		super.run();
	}

	public void processResults()
	{
		// "The Crown of the Goblin King shudders for a moment, but
		// nothing happens."

		if ( responseText.indexOf( "nothing happens" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The " + item.getName() + " is not zappable." );
			return;
		}

		// If it blew up, remove wand
		if ( responseText.indexOf( "abruptly explodes" ) != -1 )
		     StaticEntity.getClient().processResult( wand.getNegation() );

		// Remove old item and notify the user of success.
		StaticEntity.getClient().processResult( item.getInstance( -1 ) );
		KoLmafia.updateDisplay( item.getName() + " has been transformed." );
	}
}

