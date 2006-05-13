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

import java.awt.Color;
import java.util.Random;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormatSymbols;

import java.io.File;
import javax.swing.JLabel;
import java.awt.Toolkit;

import net.java.dev.spellcast.utilities.UtilityConstants;
import net.java.dev.spellcast.utilities.LockableListModel;

public interface KoLConstants extends UtilityConstants
{
	public static final Random RNG = new Random();
	public static final String LINE_BREAK = System.getProperty( "line.separator" );
	public static final Class [] NOPARAMS = new Class[0];

	public static final JLabel BLANK_LABEL = new JLabel();
	public static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();

	public static final String VERSION_NAME = "KoLmafia v7.5";
	public static final String VERSION_DATE = "Released May 13, 2006";

	public static final KoLSettings GLOBAL_SETTINGS = new KoLSettings();
	public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI( System.in );
	public static final File SCRIPT_DIRECTORY = new File( "scripts" );

	public static final LockableListModel existingFrames = new LockableListModel();
	public static final LockableListModel existingPanels = new LockableListModel();

	public static final DecimalFormat df = new DecimalFormat(
		"#,##0", new DecimalFormatSymbols( Locale.US ) );

	public static final DecimalFormat df2 = new DecimalFormat(
		"+#0;-#0", new DecimalFormatSymbols( Locale.US ) );

	public static final DecimalFormat ff = new DecimalFormat(
		"#,##0.00", new DecimalFormatSymbols( Locale.US ) );

	public static final DecimalFormat sff = new DecimalFormat(
		"+#0.00;-#0.00", new DecimalFormatSymbols( Locale.US ) );

	public static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd" );

	public static final int ENABLE_STATE   = 1;
	public static final int ERROR_STATE    = 2;
	public static final int ABORT_STATE    = 3;

	public static final int PENDING_STATE  = 4;
	public static final int CONTINUE_STATE = 5;

	public static final Color ERROR_COLOR = new Color( 255, 192, 192 );
	public static final Color ENABLED_COLOR = new Color( 192, 255, 192 );
	public static final Color DISABLED_COLOR = null;

	public static final String [][] WIN_GAME_TEXT = new String [][]
	{
		{
			"Petitioning the Seaside Town Council for automatic game completion...",
			"The Seaside Town Council has rejected your petition.  Game incomplete.",
			"You reject the Seaside Town's decision.  Fighting the council...",
			"You have been defeated by the Seaside Town Council."
		},

		{
			"You enter the super-secret code into the Strange Leaflet...",
			"Your ruby W and heavy D fuse to form the mysterious R!",
			"Moxie sign backdoor accessed.  Supertinkering The Ultimate Weapon...",
			"Supertinkering complete.  Executing tower script...",
			"Your RNG spawns an enraged cow on Floors 1-6."
		},

		{
			"You win the game. What, you were expecting more?",
			"You are now standing in an open field to the west of the Kingdom.",
			"You hear a gurgling ocean to the south, and a path leads north into Valhalla.",
			"What now, Adventurer?"
		},

		{
			"You touch your star starfish!  You surge with power!",
			"Accessing tower backdoor.  Fighting Naughty Sorceress...",
			"Connection timed out during post.  Retrying...",
			"Connection timed out during reply.  Retrying...",
			"Your star power has expired.  You have been defeated!"
		},

		{
			"You raise your metallic A to the sky. Victory is yours!",
			"Original game concept by Jick (Asymmetric Publications).",
			"Co-written by Mr. Skullhead, Riff, and the /dev team.",
			"Special thanks to: the Mods, the Ascension testers, and you.",
			"We present you a new quest, which is basically the same thing, only harder.",
			"Crap!  You've been using KoLmafia so long you can't remember how to play!  Game Over."
		},

		{
			"Executing secret trail script...",
			"Crossing first obstacle, admiring landmarks...",
			"Path set to oxygenarian, familiar pace set to grue-ing...",
			"You have died from KoLera.  Game Over."
		}
	};
}
