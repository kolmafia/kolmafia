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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestEditorKit;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonorailManager
{
	public static final Map<String, String[]> lyleSpoilers = new HashMap<>();
	static
	{
		MonorailManager.lyleSpoilers.put( "Exchange 10 shovelfuls of dirt and 10 hunks of granite for an earthenware muffin tin!",
		                                  new String[]{"", "earthenware muffin tin"} );
		MonorailManager.lyleSpoilers.put( "Order a blueberry muffin", new String[]{ "", "blueberry muffin" } );
		MonorailManager.lyleSpoilers.put( "Order a bran muffin", new String[]{ "", "bran muffin" } );
		MonorailManager.lyleSpoilers.put( "Order a chocolate chip muffin", new String[]{ "", "chocolate chip muffin" } );
		MonorailManager.lyleSpoilers.put( "Back to the Platform!", new String[]{ "", null } );
	}

	public static void resetMuffinOrder()
	{
		String muffinOrder = Preferences.getString( "muffinOnOrder" );
		if (muffinOrder.equals("blueberry muffin") ||
				muffinOrder.equals("bran muffin") ||
				muffinOrder.equals("chocolate chip muffin"))
		{
			Preferences.setString( "muffinOnOrder", "earthenware muffin tin" );
		}
	}

	public static Object[][] choiceSpoilers(final int choice, final StringBuffer buffer )
	{
		if ( choice != 1308 || buffer == null )
		{
			return null;
		}

		// Lazy! They're lazy, I tell you!
		// Options in On A Downtown Train are dynamically assigned to values
		// rather than being bound to them, so it's literally impossible to
		// know what each choice will do without parsing them and reading their value.

		// We'll have to imitate RequestEditorKit.addChoiceSpoilers( final String location, final StringBuffer buffer )
		List<ChoiceManager.Option> options = new ArrayList<>();

		Matcher matcher = Pattern.compile( "name=choiceform\\d+(.*?)</form>", Pattern.DOTALL ).matcher( buffer );

		while ( matcher.find() )
		{
			String currentSection = matcher.group( 1 );
			Matcher optionMatcher = RequestEditorKit.OPTION_PATTERN.matcher( currentSection );
			if ( !optionMatcher.find() )
			{	// this wasn't actually a choice option - strange!
				continue;
			}
			Matcher buttonTextMatcher = RequestEditorKit.BUTTON_TEXT_PATTERN.matcher( currentSection );
			if ( !buttonTextMatcher.find() )
			{	// no... button? a blank one, maybe? weird!
				continue;
			}

			String buttonText = buttonTextMatcher.group( 1 );
			int choiceNumber = StringUtilities.parseInt( optionMatcher.group( 1 ) );

			if ( lyleSpoilers.containsKey( buttonText ) )
			{
				String[] thisOption = MonorailManager.lyleSpoilers.get( buttonText );
				options.add( new ChoiceManager.Option( thisOption[ 0 ], choiceNumber, thisOption[ 1 ] ) );
			}
		}

		return new Object[][] { new String[] { "" }, new String[] { "On a Downtown Train" }, options.toArray() };
	}
}