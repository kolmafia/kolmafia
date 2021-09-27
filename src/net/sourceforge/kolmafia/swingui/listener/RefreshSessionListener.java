package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.KoLmafia;


public class RefreshSessionListener
	extends ThreadedListener
{
	@Override
	protected void execute()
	{
		KoLmafia.refreshSession();
	}
}

