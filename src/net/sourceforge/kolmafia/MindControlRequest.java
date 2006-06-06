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

public class MindControlRequest extends KoLRequest
{
	int level;

	public MindControlRequest( KoLmafia client, int level )
	{
		super( client, "canadia.php" );

		addFormField( "action", "changedial" );
		addFormField( "whichlevel", String.valueOf( level ) );

		this.level = level;
	}

	public void run()
	{
		// Avoid server hits if user gives an invalid level

		if ( level < 0 || level > 11 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The dial only goes from 0 to 11." );
			return;
		}

		// This is only available in Mysticality signs

		if ( !KoLCharacter.inMysticalitySign() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find the Mind Control device." );
			return;
		}

		KoLmafia.updateDisplay( "Resetting mind control device..." );
		super.run();
	}

	protected void processResults()
	{
		KoLmafia.updateDisplay( "Mind control device reset." );
		KoLCharacter.setMindControlLevel( level );
	}
}
