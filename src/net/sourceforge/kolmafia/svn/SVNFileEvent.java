package net.sourceforge.kolmafia.svn;

import java.io.File;

import org.tmatesoft.svn.core.wc.SVNEvent;

public class SVNFileEvent
{
	private final File file;
	private final SVNEvent event;

	public SVNFileEvent( File f, SVNEvent e )
	{
		file = f;
		event = e;
	}

	public File getFile()
	{
		return file;
	}

	public SVNEvent getEvent()
	{
		return event;
	}
}
