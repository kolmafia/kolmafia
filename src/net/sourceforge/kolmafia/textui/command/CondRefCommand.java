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

public class CondRefCommand
	extends AbstractCommand
{
	public CondRefCommand()
	{
		this.usage = " - list <condition>s usable with if/while commands.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestLogger.printLine( "<table border=2>" + "<tr><td colspan=3>today | tomorrow is mus | mys | mox day</td></tr>" + "<tr><td colspan=3>class is [not] sauceror | <i>etc.</i></td></tr>" + "<tr><td colspan=3>skill list contains | lacks <i>skill</i></td></tr>" + "<tr><td>level<br>health<br>mana<br>meat<br>adventures<br>" + "inebriety | drunkenness<br>muscle<br>mysticality<br>moxie<br>" + "worthless item<br>stickers<br><i>item</i><br><i>effect</i></td>" + "<td>=<br>==<br>&lt;&gt;<br>!=<br>&lt;<br>&lt;=<br>&gt;<br>&gt;=</td>" + "<td><i>number</i><br><i>number</i>%&nbsp;(health/mana only)<br>" + "<i>item</i> (qty in inventory)<br><i>effect</i> (turns remaining)</td>" + "</tr></table><br>" );
	}
}
