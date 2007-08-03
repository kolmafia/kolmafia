/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

public class QuestLogRequest extends KoLRequest
{
	public static final String TRAPPER = "Am I my Trapper's Keeper?";
	public static final String ISLAND_WAR = "Make War, Not...";
	public static final String CITADEL = "White Citadel";
	public static final String GALAKTIK = "What's Up, Doc?";
	public static final String MACGUFFIN = "Quest for the Holy MacGuffin";

	private static String started = "";
	private static String finished = "";

	public QuestLogRequest()
	{	super( "questlog.php" );
	}

	public void run()
	{
		addFormField( "which", "1" );
		super.run();
		registerQuests( this.getURLString(), this.responseText );

		addFormField( "which", "2" );
		super.run();
		registerQuests( this.getURLString(), this.responseText );
	}

	public static final boolean startedQuest( String quest )
	{	return started.indexOf( quest ) != -1;
	}

	public static final boolean finishedQuest( String quest )
	{	return finished.indexOf( quest ) != -1;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public static final void registerQuests( String urlString, String responseText )
	{
		if ( urlString.indexOf( "which=1" ) != -1 )
			started = responseText;
		if ( urlString.indexOf( "which=2" ) != -1 )
			finished = responseText;
	}
}
