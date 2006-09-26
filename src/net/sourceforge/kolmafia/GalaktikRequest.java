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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;

public class GalaktikRequest extends KoLRequest
{
	public static final int HP = 1;
	public static final int MP = 2;

	private int restoreAmount;
	private int type;

	public GalaktikRequest( int type )
	{
		super( "galaktik.php" );

		this.type = type;
		switch ( type )
		{
			case HP:
				addFormField( "action", "curehp" );
				addFormField( "pwd" );
				this.restoreAmount = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
				break;

			case MP:
				addFormField( "action", "curemp" );
				addFormField( "pwd" );
				this.restoreAmount = KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP();
				break;

			default:
				this.restoreAmount = 0;
				break;
		}
	}

	public void run()
	{
		if ( restoreAmount == 0 )
		{
			KoLmafia.updateDisplay( CONTINUE_STATE, "You don't need that cure." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting Doc Galaktik..." );
		super.run();
	}

	public static LockableListModel retrieveCures()
	{
		LockableListModel cures = new LockableListModel();

		if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
			cures.add( "Restore all HP with Curative Nostrum" );

		if ( KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			cures.add( "Restore all MP with Fizzy Invigorating Tonic" );

		return cures;
	}

	protected void processResults()
	{
		if ( responseText.indexOf( "You can't afford that" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't afford that cure." );
			return;
		}

		CharpaneRequest.getInstance().run();
		KoLmafia.updateDisplay( "Cure purchased." );
	}

	public String getCommandForm()
	{	return "galaktik " + (type == HP ? "hp" : "mp");
	}
}
