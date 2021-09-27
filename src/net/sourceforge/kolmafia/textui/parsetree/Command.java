package net.sourceforge.kolmafia.textui.parsetree;

public abstract class Command
	extends ParseTreeNode
{
	// A barrier is any code construct that is fundamentally incapable of
	// completing normally; any subsequent code in the same scope is
	// therefore dead code (exception: case labels can make code live again).
	// The basic barriers are the RETURN, EXIT, BREAK, and CONTINUE statements
	// (THROW would also be a barrier if we had something like that).
	// Any compound statement that unavoidably hits a barrier is itself a
	// barrier: IF/ELSEIF/ELSE with barriers in all branches; SWITCH with no
	// breaks and a barrier at the end; TRY with a barrier in either the code
	// block or the FINALLY clause; and infinite loops with no break.
	// Note that most looping constructs cannot be barriers, due to the
	// possibility of the loop executing zero times and just falling thru.
	// Note that function calls cannot be a barrier, not even abort(), due
	// to the existence of the "disable" CLI command.  The code currently
	// assumes that no expression can be a barrier.
	// Note that BREAK and CONTINUE are disallowed outside of loops, so the
	// presence of a barrier in a non-void function's body is exactly
	// equivalent to the requirement that it not return without a return value.
	public boolean assertBarrier()
	{
		return false;
	}

	// A breakable code construct is a BREAK statement, or any compound
	// statement that can possibly execute a BREAK that isn't caught internally.
	// Looping statements are therefore not considered breakable, since they
	// would handle the break themselves.
	public boolean assertBreakable()
	{
		return false;
	}

	// There is no need for a corresponding check for CONTINUE statements.
	// Since they can only branch back to already-executed code, they have
	// no effect on code reachability.
}
