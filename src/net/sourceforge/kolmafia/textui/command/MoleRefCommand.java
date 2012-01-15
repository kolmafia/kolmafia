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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

public class MoleRefCommand
	extends AbstractCommand
{
	public MoleRefCommand()
	{
		this.usage = " - Path of the Mole spoilers.";
	}

	public void run( final String cmd, final String parameters )
	{
		RequestLogger.printLine( "<table border=2>" + "<tr><td>9</td><td rowspan=6></td><td rowspan=3></td><td>+30% all stats</td></tr>" + "<tr><td>8</td><td rowspan=5>+10 fam weight</td></tr>" + "<tr><td>7</td></tr>" + "<tr><td>6</td><td>MP</td></tr>" + "<tr><td>5</td><td rowspan=6>food</td></tr>" + "<tr><td>4</td></tr>" + "<tr><td>3</td><td>HP</td><td rowspan=7>+3 stats/fight</td></tr>" + "<tr><td>2</td><td rowspan=5>+meat</td></tr>" + "<tr><td>1</td></tr>" + "<tr><td>0</td></tr>" + "<tr><td>-1</td><td rowspan=5>booze</td></tr>" + "<tr><td>-2</td></tr>" + "<tr><td>-3</td><td>stats</td></tr>" + "<tr><td>-4</td><td rowspan=6></td><td rowspan=5>regenerate</td></tr>" + "<tr><td>-5</td></tr>" + "<tr><td>-6</td><td>-3MP/skill</td></tr>" + "<tr><td>-7</td><td rowspan=3></td></tr>" + "<tr><td>-8</td></tr>" + "<tr><td>-9</td><td>+30 ML</td></tr>" + "</table><br>" );
	}
}
