/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

package net.sourceforge.kolmafia.svn;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import net.sourceforge.kolmafia.RequestLogger;

public class CheckStatusRunnable
	implements Runnable
{
	private CheckStatusResult result = CheckStatusResult.UNSTARTED;

	private File originalFile;
	private SVNRepository repo;
	private long wcRevisionNumber;
	File repoFile;
	boolean quiet;

	private long repoRevision;
	private SVNException exception;

	public CheckStatusRunnable( File originalFile, SVNRepository repo, long wcRevisionNumber, File repoFile, boolean quiet )
	{
		this.originalFile = originalFile;
		this.repo = repo;
		this.wcRevisionNumber = wcRevisionNumber;
		this.repoFile = repoFile;
		this.quiet = quiet;
	}

	public void run()
	{
		result = CheckStatusResult.STARTED;
		try
		{
			repoRevision = repo.getLatestRevision();
		}
		catch ( SVNException e )
		{
			exception = e;
			result = CheckStatusResult.SVN_EXCEPTION;
			if ( !quiet ) {
				RequestLogger.printLine( repoFile.getName() + " not checked - exception: " + e.toString() );
			}
			return;
		}
		
		if (repoRevision == wcRevisionNumber)
		{
			result = CheckStatusResult.AT_HEAD;
			if ( !quiet )
			{
				RequestLogger.printLine( repoFile.getName() + " is at HEAD (r" + repoRevision + ")" );
			}
		}
		else
		{
			result = CheckStatusResult.NEEDS_UPDATE;
			if ( !quiet )
			{
				RequestLogger.printLine( repoFile.getName() + " needs updating from (r" + wcRevisionNumber + ") to (r" + repoRevision + ")" );
			}
		}
		return;
	}

	public void reportInterrupt()
	{
		result = CheckStatusResult.OTHER_ERROR;
		if ( !quiet )
		{
			RequestLogger.printLine( repoFile.getName() + " update check was interrupted." );
		}
	}

	public CheckStatusResult getResult()
	{
		return result;
	}

	public File getOriginalFile()
	{
		return originalFile;
	}

	public SVNException getException()
	{
		return exception;
	}

	public boolean isAtHead()
	{
		return getResult() == CheckStatusResult.AT_HEAD;
	}

	public boolean shouldBeUpdated()
	{
		return getResult() == CheckStatusResult.NEEDS_UPDATE;
	}

	public static enum CheckStatusResult
	{
		UNSTARTED, STARTED, SVN_EXCEPTION, OTHER_ERROR, AT_HEAD, NEEDS_UPDATE
	}

}
