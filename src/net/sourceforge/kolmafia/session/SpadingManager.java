/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.io.File;
import java.util.List;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.parsetree.Value;


public class SpadingManager
{
	public enum SpadingEvent
	{
		COMBAT_ROUND,
		CONSUME_DRINK,
		CONSUME_EAT,
		CONSUME_SPLEEN,
		MEAT_DROP,
	}

	private static String getScriptName()
	{
		String scriptName = Preferences.getString( "spadingScript" ).trim();
		if ( scriptName.length() == 0 )
		{
			return null;
		}

		return scriptName;
	}

	public static boolean hasSpadingScript()
	{
		return SpadingManager.getScriptName() != null;
	}

	public static boolean processCombatRound( final String responseText )
	{
		return SpadingManager.invokeSpadingScript( SpadingEvent.COMBAT_ROUND, responseText );
	}

	public static boolean processMeatDrop( final String meatDrop )
	{
		return SpadingManager.invokeSpadingScript( SpadingEvent.MEAT_DROP, meatDrop );
	}

	public static boolean processConsumeDrink( final String responseText )
	{
		return SpadingManager.invokeSpadingScript( SpadingEvent.CONSUME_DRINK, responseText );
	}

	public static boolean processConsumeEat( final String responseText )
	{
		return SpadingManager.invokeSpadingScript( SpadingEvent.CONSUME_EAT, responseText );
	}

	public static boolean processConsumeSpleen( final String responseText )
	{
		return SpadingManager.invokeSpadingScript( SpadingEvent.CONSUME_SPLEEN, responseText );
	}

	private static boolean invokeSpadingScript( final SpadingEvent event, final String responseText )
	{
		String scriptName = SpadingManager.getScriptName();

		if ( responseText == null || scriptName == null )
		{
			return false;
		}

		List<File> scriptFiles = KoLmafiaCLI.findScriptFile( scriptName );
		Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFiles );

		if ( interpreter == null )
		{
			return false;
		}

		File scriptFile = scriptFiles.get( 0 );

		Object[] parameters = new Object[2];
		parameters[0] = event.toString();
		parameters[1] = responseText;

		KoLmafiaASH.logScriptExecution( "Starting spading script: ", scriptFile.getName(), interpreter );

		// Since we are automating, let the script execute without interruption
		KoLmafia.forceContinue();

		interpreter.execute( "main", parameters );

		KoLmafiaASH.logScriptExecution( "Finished spading script: ", scriptFile.getName(), interpreter );

		return true;
	}
}
