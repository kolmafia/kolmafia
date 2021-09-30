package net.sourceforge.kolmafia.textui.parsetree;

import org.eclipse.lsp4j.Location;

import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class LoopBreak
	extends ScriptState
{
	public LoopBreak( final Location location )
	{
		super( location, ScriptRuntime.State.BREAK );
	}

	@Override
	public boolean assertBreakable()
	{
		return true;
	}
}
