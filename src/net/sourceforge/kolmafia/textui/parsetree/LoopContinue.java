package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class LoopContinue
	extends ScriptState
{
	public LoopContinue()
	{
		super( ScriptRuntime.State.CONTINUE );
	}
}
