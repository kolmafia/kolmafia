/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GalaktikRequest
	extends GenericRequest
{
	private static final Pattern TYPE_PATTERN = Pattern.compile( "action=(\\w*)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=([\\d,]+)" );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "You spent ([\\d,]+) Meat" );

	public static final String HP = "curehp";
	public static final String MP = "curemp";

	private static boolean discount = false;

	private String action = "";
	private int restoreAmount;

	public GalaktikRequest()
	{
		super( "galaktik.php" );
	}

	public GalaktikRequest( final String type )
	{
		this( type, 0 );
	}

	public GalaktikRequest( final String type, final int restoreAmount )
	{
		super( "galaktik.php" );

		this.addFormField( "action", type );
		this.action = type;

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

		if ( this.restoreAmount > 0 )
		{
			this.addFormField( "quantity", String.valueOf( this.restoreAmount ) );
		}
	}

	public static void setDiscount( final boolean discount )
	{
		GalaktikRequest.discount = discount;
	}

	public static boolean getDiscount()
	{
		return GalaktikRequest.discount ||
			(InventoryManager.getCount( ItemPool.FRAUDWORT ) >= 3 &&
			InventoryManager.getCount( ItemPool.SHYSTERWEED ) >= 3 &&
			InventoryManager.getCount( ItemPool.SWINDLEBLOSSOM ) >= 3);
	}

	public static int costPerHP()
	{
		return GalaktikRequest.getDiscount() ? 6 : 10;
	}

	public static int costPerMP()
	{
		return GalaktikRequest.getDiscount() ? 12 : 17;
	}

	private static int costPerUnit( final String type )
	{
		if ( type == null )
		{
			return 0;
		}
		if ( type.equals( GalaktikRequest.HP ) || type.equals( "HP" ) )
		{
			return GalaktikRequest.costPerHP();
		}
		if ( type.equals( GalaktikRequest.MP ) || type.equals( "MP" )  )
		{
			return GalaktikRequest.costPerMP();
		}

		return 0;
	}

	private static String cureType( final String type )
	{
		if ( type == null )
		{
			return null;
		}
		if ( type.equals( GalaktikRequest.HP ) )
		{
			return "HP";
		}
		if ( type.equals( GalaktikRequest.MP ) )
		{
			return "MP";
		}

		return null;
	}

	public void run()
	{
		if ( GalaktikRequest.cureType( this.action ) != null && this.restoreAmount == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE, "You don't need that cure." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting Doc Galaktik..." );

		super.run();
	}

	public void processResults()
	{
		GalaktikRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.responseText.indexOf( "You can't afford that" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't afford that cure." );
			return;
		}

		if ( this.action != null )
		{
			String message =  this.action.equals( "startquest" ) ?
				"Quest accepted." : "Cure purchased.";
			KoLmafia.updateDisplay( message );
		}
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "galaktik.php" ) )
		{
			return;
		}

		// Ah, my friend! You've found my herbs! These will come in
		// very, very handy. To show my appreciation, I'd like to offer
		// you a lifetime discount on Curative Nostrums and Fizzy
		// Invigorating Tonics. Of course, I'll be taking a loss every
		// time, but you, my friend, you deserve it!

		if ( responseText.indexOf( "You've found my herbs!" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.FRAUDWORT, -3 );
			ResultProcessor.processItem( ItemPool.SHYSTERWEED, -3 );
			ResultProcessor.processItem( ItemPool.SWINDLEBLOSSOM, -3 );
			GalaktikRequest.discount = true;
		}
		else if ( responseText.indexOf( "Restore HP (6 Meat each)" ) != -1 ||
			  responseText.indexOf( "Restore MP (12 Meat each)" ) != -1 )
		{
			GalaktikRequest.discount = true;
		}
		else if ( responseText.indexOf( "Restore HP (10 Meat each)" ) != -1 ||
			  responseText.indexOf( "Restore MP (17 Meat each)" ) != -1 )
		{
			GalaktikRequest.discount = false;
		}

		if ( responseText.indexOf( "You can't afford that" ) != -1 )
		{
			return;
		}

		Matcher matcher = TYPE_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return;
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "galaktik.php" ) )
		{
			return false;
		}

		String message = "Visiting Doc Galaktik";

		Matcher matcher = TYPE_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			String type = GalaktikRequest.cureType( matcher.group(1) );
			int quantity = 0;

			matcher = QUANTITY_PATTERN.matcher( urlString );
			if ( matcher.find() )
			{
				quantity = StringUtilities.parseInt( matcher.group( 1 ) );
			}

			int costperUnit = GalaktikRequest.costPerUnit( type );
			int cost = quantity * costperUnit;

			if ( cost > 0 && cost <= KoLCharacter.getAvailableMeat() )
			{
				 message = "Restore " + quantity + " " + type + " at Doc Galaktik's";
			}
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
