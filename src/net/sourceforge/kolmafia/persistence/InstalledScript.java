package net.sourceforge.kolmafia.persistence;

import java.io.File;

public class InstalledScript
	extends Script
{
	private final File scriptFolder;
	private boolean atHead = true;

	public InstalledScript( Script s, File f )
	{
		this.addAll( s );

		this.authors = s.authors;
		this.category = s.category;
		this.longDesc = s.longDesc;
		this.repo = s.repo;
		this.scriptName = s.scriptName;
		this.shortDesc = s.shortDesc;
		this.forumThread = s.forumThread;
		this.scriptFolder = f;
	}

	public InstalledScript( Script s, File f, boolean atHead )
	{
		this( s, f );

		this.atHead = atHead;
	}

	public File getScriptFolder()
	{
		return scriptFolder;
	}

	public boolean isAtHead()
	{
		return atHead;
	}

	public void setAtHead( boolean atHead )
	{
		this.atHead = atHead;
	}
}
