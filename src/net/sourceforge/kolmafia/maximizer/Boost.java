/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class Boost
implements Comparable
{
	private boolean isEquipment, isShrug, priority;
	private String cmd, text;
	private int slot;
	private double boost;
	private AdventureResult item, effect;
	private FamiliarData fam;

	private Boost( String cmd, String text, AdventureResult item, double boost )
	{
		this.cmd = cmd;
		this.text = text;
		this.item = item;
		this.boost = boost;
		if ( cmd.length() == 0 )
		{
			this.text = "<html><font color=gray>" +
				text.replaceAll( "&", "&amp;" ) +
				"</font></html>";
		}
	}

	public Boost( String cmd, String text, AdventureResult effect, boolean isShrug, AdventureResult item, double boost, boolean priority )
	{
		this( cmd, text, item, boost );
		this.isEquipment = false;
		this.effect = effect;
		this.isShrug = isShrug;
		this.priority = priority;
	}

	public Boost( String cmd, String text, int slot, AdventureResult item, double boost )
	{
		this( cmd, text, item, boost );
		this.isEquipment = true;
		this.slot = slot;
	}

	public Boost( String cmd, String text, FamiliarData fam, double boost )
	{
		this( cmd, text, (AdventureResult) null, boost );
		this.isEquipment = true;
		this.fam = fam;
		this.slot = -1;
	}

	@Override
	public String toString()
	{
		return this.text;
	}

	public int compareTo( Object o )
	{
		if ( !(o instanceof Boost) ) return -1;
		Boost other = (Boost) o;

		if ( this.isEquipment != other.isEquipment )
		{
			return this.isEquipment ? -1 : 1;
		}
		if ( this.priority != other.priority )
		{
			return this.priority ? -1 : 1;
		}
		if ( this.isEquipment ) return 0;	// preserve order of addition
		int rv = Double.compare( other.boost, this.boost );
		return rv;
	}

	public boolean execute( boolean equipOnly )
	{
		if ( equipOnly && !this.isEquipment ) return false;
		if ( this.cmd.length() == 0 ) return false;
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.cmd );
		return true;
	}

	public void addTo( Spec spec )
	{
		if ( this.isEquipment )
		{
			if ( this.fam != null )
			{
				spec.setFamiliar( fam );
			}
			else if ( this.slot >= 0 && this.item != null )
			{
				spec.equip( slot, this.item );
			}
		}
		else if ( this.effect != null )
		{
			if ( this.isShrug )
			{
				spec.removeEffect( this.effect );
			}
			else
			{
				spec.addEffect( this.effect );
			}
		}
	}

	public AdventureResult getItem()
	{
		if ( this.effect != null ) return this.effect;
		return this.item;
	}
}