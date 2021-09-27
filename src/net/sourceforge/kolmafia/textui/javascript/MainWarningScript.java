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
