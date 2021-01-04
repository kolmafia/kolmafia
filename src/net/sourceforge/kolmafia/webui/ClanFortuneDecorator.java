/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ClanFortuneDecorator
{
	private static final Pattern QUESTION_PATTERN = Pattern.compile( "name=\"q(\\d)\" required (?:value=\"(.*?)\"|)" );

	public static final void decorateQuestion( final StringBuffer buffer )
	{
		// Don't decorate if no questions set
		String q[] = new String[3];
		q[0] = Preferences.getString( "clanFortuneWord1" );
		q[1] = Preferences.getString( "clanFortuneWord2" );
		q[2] = Preferences.getString( "clanFortuneWord3" );
		if ( q[0].length() + q[1].length() + q[2].length() == 0 )
		{
			return;
		}
		// Check for questions, don't replace unless null
		Matcher matcher = QUESTION_PATTERN.matcher( buffer.toString() );
		while ( matcher.find() )
		{
			int num = StringUtilities.parseInt( matcher.group( 1 ) );
			String question = matcher.group( 2 );
			if ( num >= 1 && num <= 3 )
			{
				if ( q[num-1].length() > 0 && question == null )
				{
					String findString = "name=\"q" + num + "\" required ";
					String replaceString = "name=\"q" + num + "\" required value=\"" + q[num-1] + "\"";
					StringUtilities.singleStringReplace( buffer, findString, replaceString );
				}
			}
		}
	}

	public static final void decorateAnswer( final StringBuffer buffer )
	{
		// Don't decorate if no answers set
		String q[] = new String[3];
		q[0] = Preferences.getString( "clanFortuneReply1" );
		q[1] = Preferences.getString( "clanFortuneReply2" );
		q[2] = Preferences.getString( "clanFortuneReply3" );
		if ( q[0].length() + q[1].length() + q[2].length() == 0 )
		{
			return;
		}
		// Check for answers, don't replace unless null (in case one day KoL supports remembering them
		Matcher matcher = QUESTION_PATTERN.matcher( buffer.toString() );
		while ( matcher.find() )
		{
			int num = StringUtilities.parseInt( matcher.group( 1 ) );
			String question = matcher.group( 2 );
			if ( num >= 1 && num <= 3 )
			{
				if ( q[num-1].length() > 0 && question == null )
				{
					String findString = "name=\"q" + num + "\" required ";
					String replaceString = "name=\"q" + num + "\" required value=\"" + q[num-1] + "\"";
					StringUtilities.singleStringReplace( buffer, findString, replaceString );
				}
			}
		}
	}
}
