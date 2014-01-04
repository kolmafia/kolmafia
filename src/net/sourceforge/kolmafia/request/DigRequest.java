/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DigRequest
	extends GenericRequest
{
	private static final Pattern SQUARE_PATTERN = Pattern.compile( "s1=(\\d+)&s2=(\\d+)&s3=(\\d+)" );

	public DigRequest()
	{
		super( "dig.php" );
	}

	public static final int getTurnsUsed( GenericRequest request )
	{
		String action = request.getFormField( "action" );
		return action != null && action.equals( "dig" ) ? 1 : 0;
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "dig.php" ) )
		{
			return;
		}

		if ( responseText.indexOf( "Your archaeologing shovel can't take any more abuse." ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.ARCHAEOLOGING_SHOVEL,
				"Your archaeologing shovel crumbled." );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "dig.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action == null )
		{
                        return true;
		}

                if ( action.equals( "dig" ) )
                {
			Matcher matcher = SQUARE_PATTERN.matcher( urlString );
                        if ( !matcher.find() )
                        {
                                return false;
                        }
                        int s1 = StringUtilities.parseInt( matcher.group( 1 ) );
                        int s2 = StringUtilities.parseInt( matcher.group( 2 ) );
                        int s3 = StringUtilities.parseInt( matcher.group( 3 ) );

                        message = "Excavating square " + s1 + "/" + s2 + "/" + s3;
                }

		if ( message == null )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
