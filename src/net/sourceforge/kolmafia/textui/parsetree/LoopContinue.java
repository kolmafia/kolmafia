package net.sourceforge.kolmafia.textui.parsetree;

import org.eclipse.lsp4j.Location;

import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class LoopContinue
	extends ScriptState
{
	public LoopContinue( final Location location )
	{
		super( location, ScriptRuntime.State.CONTINUE );
	}
}
