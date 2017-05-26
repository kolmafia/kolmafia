/**
 * Copyright (c) 2005-2017, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VillainLairDecorator
{
	private static final Pattern LABEL_PATTERN = Pattern.compile( "from top to bottom:<br /><center>\"(.*?)\"<br />\"(.*?)\"<br />\"(.*?)\"</center>" );

	private static final String[] OPTIONS =
	{
		"Vent Poisonous Gas",
		"Monorail Shutdown",
		"Roaring Fire",
		"Poison Gas"
	};

	public static final String Symbology( final String responseText )
	{
		if ( !responseText.contains( "Symbology" ) )
		{
			return "0";
		}
		Matcher matcher = LABEL_PATTERN.matcher( responseText );
		int index = 0;
		if ( matcher.find() )
		{
			for ( String option : VillainLairDecorator.OPTIONS )
			{
				for ( int i = 1; i <= 3; i++ )
				{
					if ( option.equals( matcher.group( i ) ) )
					{
						index = i;
						break;
					}
				}
			}
		}
		return String.valueOf( index );
	}

}
