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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class RumpleManager
{
	// <span class='guts'>You peer through the portal into a house full of activity.  Children are everywhere!  The portal lets you watch them and their parents without fear of being noticed. You see the father tearing down the blinds to peep out of the window. You watch some of the many children play for awhile, and then you see the mother reclined in an overstuffed chair eating a bag of bacon-flavored onion rings. You're distracted by yet more kids romping around, and when you look back you see the father trying to squeeze into a girdle. Then the portal shimmers and you see no more.</span>
	private static final Pattern GUTS_PATTERN = Pattern.compile( "<span class='guts'>(.*?)</span>", Pattern.DOTALL );
	private static final Pattern PATTERN1 = Pattern.compile( "without fear of being noticed. *([^.]*)", Pattern.DOTALL );
	private static final Pattern PATTERN2 = Pattern.compile( "and then ([^.]*)", Pattern.DOTALL );
	private static final Pattern PATTERN3 = Pattern.compile( "when you look back ([^.]*)", Pattern.DOTALL );

	private static final String NEITHER = "neither parent";
	private static final String FATHER = "the father";
	private static final String MOTHER = "the mother";
	private static final String BOTH = "both parents";

	private static String parent = RumpleManager.NEITHER;

	private static final String NONE = "good nature";
	private static final String GREED = "inherent greed";
	private static final String GLUTTONY = "gluttony";
	private static final String VANITY = "vanity";
	private static final String LAZINESS = "laziness";
	private static final String LUSTFULNESS = "lustfulness";
	private static final String VIOLENCE = "violent nature";

	private static String sin = RumpleManager.NONE;

	public static final void spyOnParents( final String responseText )
	{
		Matcher gutsMatcher = RumpleManager.GUTS_PATTERN.matcher( responseText );
		if ( !gutsMatcher.find() )
		{
			System.out.println( "no guts" );
			return;
		}

		String guts = gutsMatcher.group( 1 );
		RumpleManager.detectSin( RumpleManager.PATTERN1, guts );
		RumpleManager.detectSin( RumpleManager.PATTERN2, guts );
		RumpleManager.detectSin( RumpleManager.PATTERN3, guts );
	}

	private static final void detectSin( final Pattern pattern, final String text )
	{
		Matcher matcher = pattern.matcher( text );
		if ( !matcher.find() )
		{
			System.out.println( "no glory" );
			return;
		}

		String sin = matcher.group( 1 );
		RequestLogger.printLine( sin );
		RequestLogger.updateSessionLog( sin );

		// And here is where we would look at the message and decide which sin applies to which parent
	}

	public static final void pickParent( final String responseText )
	{
		RumpleManager.parent =
			responseText.contains( "You approach the Husband" ) ? RumpleManager.FATHER :
			responseText.contains( "You approach the Wife" ) ? RumpleManager.MOTHER :
			responseText.contains( "You approach the couple" ) ? RumpleManager.BOTH :
			RumpleManager.NEITHER;
	}

	public static final void pickSin( final String responseText )
	{
		RumpleManager.sin =
			responseText.contains( "greediness" ) ? RumpleManager.GREED :
			responseText.contains( "appetite" ) ? RumpleManager.GLUTTONY :
			responseText.contains( "vanity" ) ? RumpleManager.VANITY :
			responseText.contains( "laziness" ) ? RumpleManager.LAZINESS :
			responseText.contains( "lustfulness" ) ? RumpleManager.LUSTFULNESS :
			responseText.contains( "violent nature" ) ? RumpleManager.VIOLENCE :
			RumpleManager.NONE;
	}

	public static final void recordTrade( final String text )
	{
		int kids =
			( text.contains( "one of h" ) || text.contains( "one child" ) ) ? 1 :
			( text.contains( "three of their" ) || text.contains( "three kids" ) || text.contains( "three whole children" ) ) ? 3 :
			( text.contains( "semi-precious children" ) || text.contains( "five kids" ) ) ? 5 : 
			( text.contains( "seven children" ) || text.contains( "seven kids" ) || text.contains( "seven of their not-so-precious-after-all children" ) ) ? 7 :
			0;
		Preferences.increment( "rumpelstiltskinKidsRescued", kids );

		// You get the sense that your bartering proposals are becoming wearisome.

		String message = "Appealing to the " + RumpleManager.sin + " of " + RumpleManager.parent + " allowed you to rescue " + kids + " " +
			(kids == 1 ? "child" : "children") + ".";

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}
}
