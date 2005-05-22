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
import java.util.StringTokenizer;

public class AutoSellRequest extends KoLRequest
{
	private int sellType;
	private int price, limit;
	private AdventureResult soldResult;

	public static final int AUTOSELL = 1;
	public static final int AUTOMALL = 2;

	public AutoSellRequest( KoLmafia client, AdventureResult itemToSell )
	{
		super( client, "sellstuff.php" );
		addFormField( "whichitem", String.valueOf( itemToSell.getItemID() ) );
		addFormField( "action", "sell" );
		addFormField( "type", "quant" );
		addFormField( "howmany", String.valueOf( itemToSell.getCount() ) );
		addFormField( "pwd", client.getPasswordHash() );

		this.limit = 0;
		this.price = TradeableItemDatabase.getPriceByID( itemToSell.getItemID() );
		this.sellType = AUTOSELL;
		this.soldResult = new AdventureResult( itemToSell.getItemID(), 0 - itemToSell.getCount() );
	}

	public String getName()
	{	return soldResult.getName();
	}

	public AutoSellRequest( KoLmafia client, AdventureResult itemToSell, int desiredPrice )
	{	this( client, itemToSell, desiredPrice, 0 );
	}

	public AutoSellRequest( KoLmafia client, AdventureResult itemToSell, int desiredPrice, int limit )
	{
		super( client, "managestore.php" );
		addFormField( "whichitem", String.valueOf( itemToSell.getItemID() ) );
		addFormField( "action", "additem" );
		addFormField( "sellprice", String.valueOf( desiredPrice ) );
		addFormField( "limit", String.valueOf( limit ) );
		addFormField( "addtype", "addquantity" );
		addFormField( "quantity", String.valueOf( itemToSell.getCount() ) );
		addFormField( "pwd", client.getPasswordHash() );

		this.limit = limit;
		this.price = desiredPrice;
		this.sellType = AUTOMALL;
		this.soldResult = new AdventureResult( itemToSell.getItemID(), 0 - itemToSell.getCount() );
	}

	public int getSellType()
	{	return sellType;
	}

	public int getPrice()
	{	return price;
	}

	public int getLimit()
	{	return limit;
	}

	/**
	 * Executes the <code>AutoSellRequest</code>.  This will automatically
	 * sell the item for its autosell value and update the client with
	 * the needed information.
	 */

	public void run()
	{
		if ( sellType == AUTOSELL )
			updateDisplay( DISABLED_STATE, "Autoselling " + soldResult.getName() + "..." );
		else
			updateDisplay( DISABLED_STATE, "Placing " + soldResult.getName() + " in the mall..." );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Otherwise, update the client with the information stating that you
		// sold all the items of the given time, and acquired a certain amount
		// of meat from the recipient.

		client.processResult( soldResult );

		String plainTextResult = responseText.replaceAll( "<.*?>", "" );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, " " );

		if ( sellType == AUTOSELL )
		{
			try
			{
				while ( !parsedResults.nextToken().equals( "for" ) );

				int amount = df.parse( parsedResults.nextToken() ).intValue();
				client.processResult( new AdventureResult( AdventureResult.MEAT, amount ) );
			}
			catch ( Exception e )
			{
				// If an exception is caught, then this is a situation that isn't
				// currently handled by the parser.  Report it to the LogStream
				// and continue on.

				logStream.println( e );
				e.printStackTrace( logStream );
			}
		}
		else
			updateStoreManager();
	}

	private void updateStoreManager()
	{
		client.getStoreManager().clear();
		int lastFindIndex = 0;
		Matcher itemMatcher = Pattern.compile(
			"<tr><td>.*?</td><td>.*?</td><td>([\\d,]+)</td><td>(.*?)</td><td><a href=\"managestore.php\\?action=take&whichitem=(\\d+)\".*?</tr>" ).matcher( responseText );

		try
		{
			while ( itemMatcher.find( lastFindIndex ) )
			{
				lastFindIndex = itemMatcher.end();
				int price = df.parse( itemMatcher.group(1) ).intValue();
				int limit = itemMatcher.group(2).startsWith( "<" ) ? 0 :
					Integer.parseInt( itemMatcher.group(2) );

				int itemID = Integer.parseInt( itemMatcher.group(3) );
				client.getStoreManager().registerItem( itemID, price, limit );
			}
		}
		catch ( Exception e )
		{
		}
	}
}