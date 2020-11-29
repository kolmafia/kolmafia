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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

public class MainWarningScript implements Script {
	// This is a slight hack to warn folks who have failed to export main().
	@Override
	public Object exec( Context cx, Scriptable scope )
	{
		Object requireObject = ScriptableObject.getProperty( scope, "require" );
		Object moduleObject = ScriptableObject.getProperty( scope, "module" );
		if ( !( requireObject instanceof Scriptable ) || !( moduleObject instanceof Scriptable ) )
		{
			return null;
		}

		// This is the main module if require.main === module.exports.
		Object requireMain = ScriptableObject.getProperty( (Scriptable) requireObject, "main" );
		Object moduleExports = ScriptableObject.getProperty( (Scriptable) moduleObject, "exports" );
		if ( !( moduleExports instanceof Scriptable ) )
		{
			return null;
		}
		if ( requireMain != Scriptable.NOT_FOUND
			&& requireMain == moduleObject
			&& ScriptableObject.getProperty( scope, "main" ) != Scriptable.NOT_FOUND
			&& ScriptableObject.getProperty( (Scriptable) moduleExports, "main" ) == Scriptable.NOT_FOUND )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Warning: You defined 'main' in this script, but did not export it. "
				+ "You may want to set module.exports.main = main in order for it to run." );
		}
		return null;
	}
}
