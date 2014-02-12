/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.persistence;

import java.util.Vector;

public class Script
	extends Vector<String>
	implements Comparable<Object>
{
	protected String scriptName;
	protected String authors;
	protected String shortDesc;
	protected String category;
	protected String repo;
	protected String longDesc;
	protected String forumThread;

	public Script()
	{
	}

	public Script( String name, String authors, String shortDesc, String repo, String longDesc, String category, String forumThread )
	{
		this.scriptName = name;
		this.authors = authors;
		this.shortDesc = shortDesc;
		this.category = category;
		this.repo = repo;
		this.longDesc = longDesc;
		this.forumThread = forumThread;

		super.add( scriptName );
		super.add( authors );
		super.add( shortDesc );
		super.add( category );
		super.add( repo );
		super.add( longDesc );
	}

	public int compareTo( Object o )
	{
		if ( o == null || !( o instanceof Script ) )
		{
			return -1;
		}

		return this.scriptName.compareTo( ( (Script) o ).getScriptName() );
	}

	public String getScriptName()
	{
		return this.scriptName;
	}

	public String getAuthors()
	{
		return authors;
	}

	public String getShortDesc()
	{
		return shortDesc;
	}

	public String getCategory()
	{
		return category;
	}

	public String getRepo()
	{
		return repo;
	}

	public String getLongDesc()
	{
		return longDesc;
	}

	public String getForumThread()
	{
		return forumThread;
	}
}