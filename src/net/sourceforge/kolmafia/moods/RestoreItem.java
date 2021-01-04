/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
 * "AS IS" AND ANY EXPRESS OR IHPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IHPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEHPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.moods;

import net.sourceforge.kolmafia.AdventureResult;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public abstract class RestoreItem
	implements Comparable<RestoreItem>
{
	protected final String restoreName;
	protected final AdventureResult itemUsed;
	protected final int spleenHit;
	protected final int skillId;

	public RestoreItem( final String restoreName )
	{
		this.restoreName = restoreName;

		if ( ItemDatabase.contains( restoreName ) )
		{
			this.itemUsed = ItemPool.get( restoreName, 1 );
			this.spleenHit = ConsumablesDatabase.getSpleenHit( restoreName );
			this.skillId = -1;
		}
		else if ( SkillDatabase.contains( restoreName ) )
		{
			this.itemUsed = null;
			this.skillId = SkillDatabase.getSkillId( restoreName );
			this.spleenHit = 0;
		}
		else
		{
			this.itemUsed = null;
			this.skillId = -1;
			this.spleenHit = 0;
		}
	}

	public boolean isSkill()
	{
		return this.skillId != -1;
	}

	public AdventureResult getItem()
	{
		return this.itemUsed;
	}

	public abstract boolean usableInCurrentPath();
	public abstract void recover( final int needed, final boolean purchase );

	// This will likely be overridden
	public int compareTo( final RestoreItem o )
	{
		return this.restoreName.compareTo( o.restoreName );
	}

	@Override
	public String toString()
	{
		return this.restoreName;
	}
}
