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

/**
 * A special class made to create meat paste.  This class
 * accepts the appropriate meat type and creates the given
 * quantity by using the improved interface for paste creation.
 */

public class CombineMeatRequest extends ItemCreationRequest
{
	private int meatType;
	private int costToMake;

	public CombineMeatRequest( KoLmafia client, int meatType, int quantityNeeded )
	{
		super( client, "inventory.php", meatType, quantityNeeded );
		addFormField( "action", "makestuff" );

		addFormField( "whichitem", String.valueOf( meatType ) );

		this.meatType = meatType;
		this.costToMake = meatType == MEAT_PASTE ? -10 : meatType == MEAT_STACK ? -100 : -1000;
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Creating " + getQuantityNeeded() + " " + TradeableItemDatabase.getItemName( meatType ) + "..." );
		addFormField( "quantity", String.valueOf( getQuantityNeeded() ) );
		super.run();
	}

	protected void processResults()
	{
		client.processResult( new AdventureResult( AdventureResult.MEAT, costToMake * getQuantityNeeded() ) );
		super.processResults();
	}
}
