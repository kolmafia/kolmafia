/**
 * Copyright (c) 2006, KoLmafia development team
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PulverizeRequest extends KoLRequest
{
	protected static final Pattern ITEMID_PATTERN = Pattern.compile( "smashitem=(\\d+)" );
	protected static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	private AdventureResult item;

	/**
	 * Constructs a new <code>PulverizeRequest</code>.
	 * @param	client	Theto be notified of completion
	 * @param	item	The item to be pulverized
	 */

	public PulverizeRequest( AdventureResult item )
	{
		super( "smith.php" );
		addFormField( "action", "pulverize" );
		addFormField( "pwd" );
		this.item = item;
		addFormField( "smashitem", String.valueOf( item.getItemId() ) );
		addFormField( "quantity", String.valueOf( item.getCount() ) );

		// 1 to confirm smashing untradables
		addFormField( "conftrade", "1" );
	}

	public void run()
	{
		switch ( TradeableItemDatabase.getConsumptionType( item.getItemId() ) )
		{
		case ConsumeItemRequest.EQUIP_FAMILIAR:
		case ConsumeItemRequest.EQUIP_ACCESSORY:
		case ConsumeItemRequest.EQUIP_HAT:
		case ConsumeItemRequest.EQUIP_PANTS:
		case ConsumeItemRequest.EQUIP_SHIRT:
		case ConsumeItemRequest.EQUIP_WEAPON:
		case ConsumeItemRequest.EQUIP_OFFHAND:
			break;

		default:
			return;
		}

		if ( !KoLCharacter.hasSkill( "Pulverize" ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't know how to pulverize objects." );
			return;
		}

		if ( !AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER ) )
			return;

		if ( item.getCount( inventory ) < item.getCount() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a " + item.getName() + "." );
			return;
		}

		KoLmafia.updateDisplay( "Pulverizing " + item.getName() + "..." );
		super.run();
	}

	public void processResults()
	{
		// "That's too important to pulverize."
		// "That's not something you can pulverize."

		if ( responseText.indexOf( "too important to pulverize" ) != -1 || responseText.indexOf( "not something you can pulverize" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The " + item.getName() + " could not be smashed." );
			StaticEntity.getClient().processResult( item );
			return;
		}

		// Remove old item and notify the user of success.
		KoLmafia.updateDisplay( item + " smashed." );
	}

	public static boolean processRequest( String urlString )
	{
		if ( !urlString.startsWith( "smith.php" ) || urlString.indexOf( "action=pulverize" ) == -1 )
			return false;

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );

		if ( itemMatcher.find() && quantityMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
			int quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			StaticEntity.getClient().processResult( new AdventureResult( itemId, 0 - quantity ) );
			KoLmafia.getSessionStream().println( "pulverize " + quantity + " " + TradeableItemDatabase.getItemName( itemId ) );
		}

		return true;
	}
}
