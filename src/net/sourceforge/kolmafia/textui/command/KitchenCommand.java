/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.HellKitchenRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KitchenCommand
	extends AbstractCommand
{
	public KitchenCommand()
	{
		this.usage = "[?] <item> - consumes item at Hell's Kitchen, if available.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KitchenCommand.visit( parameters );
	}

	public static boolean visit( final String parameters )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			return false;
		}

		if ( KoLConstants.kitchenItems.isEmpty() )
		{
			HellKitchenRequest.getMenu();
		}

		if ( parameters.equals( "" ) )
		{
			return false;
		}

		String[] splitParameters = AbstractCommand.splitCountAndName( parameters );
		String countString = splitParameters[ 0 ];
		String nameString = splitParameters[ 1 ];

		if ( nameString.startsWith( "\u00B6" ) )
		{
			String name = ItemDatabase.getItemName( StringUtilities.parseInt( nameString.substring( 1 ) ) );
			if ( name != null )
			{
				nameString = name;
			}
		}

		for ( int i = 0; i < KoLConstants.kitchenItems.size(); ++i )
		{
			String name = (String) KoLConstants.kitchenItems.get( i );

			if ( !StringUtilities.substringMatches( name.toLowerCase(), nameString, false ) )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( name );
				return true;
			}

			int count = countString == null || countString.length() == 0 ? 1 : StringUtilities.parseInt( countString );

			if ( count == 0 )
			{
				if ( name.equals( "Imp Ale" ) )
				{
					int inebriety = ItemDatabase.getInebriety( name );
					if ( inebriety > 0 )
					{
						count = ( KoLCharacter.getInebrietyLimit() - KoLCharacter.getInebriety() ) / inebriety;
					}
				}
				else
				{
					int fullness = ItemDatabase.getFullness( name );
					if ( fullness > 0 )
					{
						count = ( KoLCharacter.getFullnessLimit() - KoLCharacter.getFullness() ) / fullness;
					}
				}
			}

			for ( int j = 0; j < count; ++j )
			{
				RequestThread.postRequest( new HellKitchenRequest( name ) );
			}

			return true;
		}

		return false;
	}
}
