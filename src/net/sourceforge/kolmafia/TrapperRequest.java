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

import java.util.List;
import java.util.StringTokenizer;

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * adventures involving trading with the trapper.
 */

public class TrapperRequest extends KoLRequest
{
	private int quantity;
	public static final AdventureResult YETI_FUR = new AdventureResult( 388, 0 );

	/**
	 * Constructs a new <code>TrapperRequest</code>.
	 *
	 * @param	client	The client to which this request will report errors/results
	 * @param	itemID	The item which will be traded at the trapper
	 * @param	quantity	How many items to trade
	 */

	public TrapperRequest( KoLmafia client, int itemID, int quantity )
	{
		super( client, "trapper.php" );

		this.quantity = quantity;
		addFormField( "action", "Yep." );
		addFormField( "pwd" );
		addFormField( "whichitem", String.valueOf( itemID ) );
		addFormField( "qty", String.valueOf( quantity ) );
	}

	public TrapperRequest( KoLmafia client, int itemID )
	{	this( client, itemID, YETI_FUR.getCount( KoLCharacter.getInventory() ) );
	}

	/**
	 * Executes the <code>TrapperRequest</code>.  This will trade the item
	 * specified in the character's <code>KoLSettings</code> for their yeti
	 * furs; if the character has no yeti furs, this method will report an
	 * error to the client.
	 */

	public void run()
	{
		if ( quantity == 0 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You do not have any furs." );
			return;
		}

		DEFAULT_SHELL.updateDisplay( "Robbing the trapper..." );
		super.run();
	}

	protected void processResults()
	{
		DEFAULT_SHELL.updateDisplay( "Trapper has been looted." );
		client.processResult( YETI_FUR.getInstance( 0 - quantity ) );
		super.processResults();
	}
}
