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
	private static final Pattern PATTERN1 = Pattern.compile( "without fear of being noticed. *Y([^.]*)", Pattern.DOTALL );
	private static final Pattern PATTERN2 = Pattern.compile( "and then y([^.]*)", Pattern.DOTALL );
	private static final Pattern PATTERN3 = Pattern.compile( "when you look back y([^.]*)", Pattern.DOTALL );

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

	private static String[][] sins = new String[3][];

	public static final void spyOnParents( final String responseText )
	{
		Matcher gutsMatcher = RumpleManager.GUTS_PATTERN.matcher( responseText );
		if ( !gutsMatcher.find() )
		{
			System.out.println( "no guts" );
			return;
		}

		String guts = gutsMatcher.group( 1 );
		RumpleManager.sins[0] = RumpleManager.detectSins( RumpleManager.PATTERN1, guts );
		RumpleManager.sins[1] = RumpleManager.detectSins( RumpleManager.PATTERN2, guts );
		RumpleManager.sins[2] = RumpleManager.detectSins( RumpleManager.PATTERN3, guts );
	}

	private static final String[] detectSins( final Pattern pattern, final String text )
	{
		Matcher matcher = pattern.matcher( text );
		if ( !matcher.find() )
		{
			System.out.println( "no glory" );
			return null;
		}

		// Look at the message and decide which sin(s) applies to which parent
		String sin = "Y" + matcher.group( 1 );
		String[] record = RumpleManager.parseSins( sin );

		String sin1 = record[1];
		String sin2 = record[2];

		StringBuilder buffer = new StringBuilder();
		buffer.append( sin );
		buffer.append( " (" );
		buffer.append( sin1 == RumpleManager.NONE ? "UNKNOWN" : sin1 );
		if ( sin2 != RumpleManager.NONE  )
		{
			buffer.append( " or " );
			buffer.append( sin2 );
		}
		buffer.append( ")" );

		String message = buffer.toString();

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return record;
	}

	private static final String[][] TELLS =
	{
		{ "counting his possessions", RumpleManager.GREED, RumpleManager.NONE },
		{ "counting her possessions", RumpleManager.GREED, RumpleManager.NONE },
		{ "gold-plating a lily", RumpleManager.GREED, RumpleManager.NONE },
		{ "studying a treasure map", RumpleManager.GREED, RumpleManager.NONE },
		{ "writing a contract to make a high-interest loan", RumpleManager.GREED, RumpleManager.NONE },
		{ "chowing down on a fistful of bacon", RumpleManager.GLUTTONY, RumpleManager.NONE },
		{ "eating a chocolate rabbit", RumpleManager.GLUTTONY, RumpleManager.NONE },
		{ "putting a fried egg on top of a cheeseburger", RumpleManager.GLUTTONY, RumpleManager.NONE },
		{ "sprinkling extra cheese on a pizza already dripping with the stuff", RumpleManager.GLUTTONY, RumpleManager.NONE },
		{ "checking his reflection in a spoon", RumpleManager.VANITY, RumpleManager.NONE },
		{ "checking her reflection in a spoon", RumpleManager.VANITY, RumpleManager.NONE },
		{ "plucking his eyebrows", RumpleManager.VANITY, RumpleManager.NONE },
		{ "plucking her eyebrows", RumpleManager.VANITY, RumpleManager.NONE },
		{ "putting in a teeth-whitening tray", RumpleManager.VANITY, RumpleManager.NONE },
		{ "telling everyone that the song on the radio", RumpleManager.VANITY, RumpleManager.NONE },
		{ "asking one of the kids to find the remote control", RumpleManager.LAZINESS, RumpleManager.NONE },
		{ "collapsed deep in an easy chair, stifling a yawn", RumpleManager.LAZINESS, RumpleManager.NONE },
		{ "lying on the floor, calling his spouse to come give a kiss", RumpleManager.LAZINESS, RumpleManager.LUSTFULNESS },
		{ "lying on the floor, calling her spouse to come give a kiss", RumpleManager.LAZINESS, RumpleManager.LUSTFULNESS },
		{ "lying on the floor", RumpleManager.LAZINESS, RumpleManager.NONE },
		{ "sleeping soundly", RumpleManager.LAZINESS, RumpleManager.NONE },
		{ "eyeing his spouse lasciviously", RumpleManager.LUSTFULNESS, RumpleManager.NONE },
		{ "flipping through a lingerie catalog", RumpleManager.LUSTFULNESS, RumpleManager.NONE },
		{ "moaning softly", RumpleManager.LUSTFULNESS, RumpleManager.NONE },
		{ "peeking through the blinds at the attractive neighbors", RumpleManager.LUSTFULNESS, RumpleManager.NONE },
		{ "kicking a dog", RumpleManager.VIOLENCE, RumpleManager.NONE },
		{ "punching a hole in the wall of the house", RumpleManager.VIOLENCE, RumpleManager.NONE },
		{ "screaming at the television", RumpleManager.VIOLENCE, RumpleManager.NONE },
		{ "stubbing his toe on an ottoman", RumpleManager.VIOLENCE, RumpleManager.NONE },
		{ "stubbing her toe on an ottoman", RumpleManager.VIOLENCE, RumpleManager.NONE },
		{ "cutting a huge piece of cake to eat alone", RumpleManager.GREED, RumpleManager.GLUTTONY },
		{ "opening a family-size bag of Cheat-Os", RumpleManager.GREED, RumpleManager.GLUTTONY },
		{ "putting a golden ring on each finger", RumpleManager.GREED, RumpleManager.VANITY },
		{ "shining a golden chalice to a reflective finish", RumpleManager.GREED, RumpleManager.VANITY },
		{ "reclined in a chair, ordering stuff from the Home Shopping Network", RumpleManager.GREED, RumpleManager.LAZINESS },
		{ "trying to shortchange the maid who is washing the dishes", RumpleManager.GREED, RumpleManager.LAZINESS },
		{ "admiring a solid marble nude statue", RumpleManager.GREED, RumpleManager.LUSTFULNESS },
		{ "admiring a valuable collection of artistic nudes", RumpleManager.GREED, RumpleManager.LUSTFULNESS },
		{ "polishing a collection of solid silver daggers", RumpleManager.GREED, RumpleManager.VIOLENCE },
		{ "loading a jewel-encrusted pistol with golden bullets", RumpleManager.GREED, RumpleManager.VIOLENCE },
		{ "checking for stretch marks while downing a huge chocolate shake", RumpleManager.GLUTTONY, RumpleManager.VANITY },
		{ "trying to squeeze into a girdle", RumpleManager.GLUTTONY, RumpleManager.VANITY },
		{ "calling the dog over to lick french-fry grease", RumpleManager.GLUTTONY, RumpleManager.LAZINESS },
		{ "reclined in an overstuffed chair eating a bag of bacon-flavored onion rings", RumpleManager.GLUTTONY, RumpleManager.LAZINESS },
		{ "licking an all-day sucker", RumpleManager.GLUTTONY, RumpleManager.LUSTFULNESS },
		{ "sensually over a rack of ribs", RumpleManager.GLUTTONY, RumpleManager.LUSTFULNESS },
		{ "tearing apart an entire roasted chicken so hard the bones snap", RumpleManager.GLUTTONY, RumpleManager.VIOLENCE },
		{ "throwing a bag of chips on the ground and stomping on it to open it", RumpleManager.GLUTTONY, RumpleManager.VIOLENCE },
		{ "collapsed in an overstuffed chair, curling his eyelashes", RumpleManager.VANITY, RumpleManager.LAZINESS },
		{ "collapsed in an overstuffed chair, curling her eyelashes", RumpleManager.VANITY, RumpleManager.LAZINESS },
		{ "using the remote control to turn the TV to the Beauty Channel", RumpleManager.VANITY, RumpleManager.LAZINESS },
		{ "checking out own his body and licking his lips seductively", RumpleManager.VANITY, RumpleManager.LUSTFULNESS },
		{ "checking out own her body and licking her lips seductively", RumpleManager.VANITY, RumpleManager.LUSTFULNESS },
		{ "practicing pick-up lines on his own reflection in a window", RumpleManager.VANITY, RumpleManager.LUSTFULNESS },
		{ "practicing pick-up lines on her own reflection in a window", RumpleManager.VANITY, RumpleManager.LUSTFULNESS },
		{ "angrily plucking stray eyebrow hairs", RumpleManager.VANITY, RumpleManager.VIOLENCE },
		{ "kicking the dog, then making sure the kick didn't scuff", RumpleManager.VANITY, RumpleManager.VIOLENCE },
		{ "reclined on the bed, idly peeping through the window to the neighbor's house", RumpleManager.LAZINESS, RumpleManager.LUSTFULNESS },
		{ "half-heartedly kicking at the cat when it comes too close", RumpleManager.LAZINESS, RumpleManager.VIOLENCE },
		{ "sleepily swiping at a whining kid", RumpleManager.LAZINESS, RumpleManager.VIOLENCE },
		{ "aggressively kissing", RumpleManager.LUSTFULNESS, RumpleManager.VIOLENCE },
		{ "tearing down the blinds to peep out of the window", RumpleManager.LUSTFULNESS, RumpleManager.VIOLENCE },
	};

	private static final String[] parseSins( final String text )
	{
		String[] record = new String[3];
		record[ 0 ] =
			text.contains( "father" ) ?
			RumpleManager.FATHER :
			text.contains( "mother" ) ?
			RumpleManager.MOTHER :
			RumpleManager.NEITHER;

		String sin1 = RumpleManager.NONE;
		String sin2 = RumpleManager.NONE;

		for ( String[] tell : RumpleManager.TELLS )
		{
			if ( text.contains( tell[ 0 ] ) )
			{
				sin1 = tell[ 1 ];
				sin2 = tell[ 2 ];
				break;
			}
		}

		record[ 1 ] = sin1;
		record[ 2 ] = sin2;

		return record;
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
