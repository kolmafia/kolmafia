/**
 *	jline - Java console input library
 *	Copyright (c) 2002-2006, Marc Prud'hommeaux <mwp1@cornell.edu>
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *	Redistributions of source code must retain the above copyright
 *	notice, this list of conditions and the following disclaimer.
 *
 *	Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer
 *	in the documentation and/or other materials provided with
 *	the distribution.
 *
 *	Neither the name of JLine nor the names of its contributors
 *	may be used to endorse or promote products derived from this
 *	software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 *	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 *	AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 *	EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *	OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *	LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *	OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jline;

import java.util.LinkedList;
import java.util.List;

/**
 *	<p>
 *	A completor that contains multiple embedded completors. This differs
 *	from the {@link ArgumentCompletor}, in that the nested completors
 *	are dispatched individually, rather than delimited by arguments.
 *	</p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class MultiCompletor
	implements Completor
{
	Completor [] completors = new Completor [0];


	/**
	 *  Construct a MultiCompletor with no embedded completors.
	 */
	public MultiCompletor ()
	{
		this (new Completor [0]);
	}


	/**
	 *  Construct a MultiCompletor with the specified list of
	 *  {@link Completor} instances.
	 */
	public MultiCompletor (final List completors)
	{
		this ((Completor [])completors.toArray (
			new Completor [completors.size ()]));
	}


	/**
	 *  Construct a MultiCompletor with the specified
	 *  {@link Completor} instances.
	 */
	public MultiCompletor (final Completor [] completors)
	{
		this.completors = completors;
	}


	public int complete (final String buffer, final int pos, final List cand)
	{
		int [] positions = new int [completors.length];
		List [] copies = new List [completors.length];
		for (int i = 0; i < completors.length; i++)
		{
			// clone and save the candidate list
			copies [i] = new LinkedList (cand);
			positions [i] = completors [i].complete (buffer, pos, copies [i]);
		}

		int maxposition = -1;
		for (int i = 0; i < positions.length; i++)
			maxposition = Math.max (maxposition, positions [i]);

		// now we have the max cursor value: build up all the
		// candidate lists that have the same cursor value
		for (int i = 0; i < copies.length; i++)
		{
			if (positions [i] == maxposition)
				cand.addAll (copies [i]);
		}

		return maxposition;
	}


	public void setCompletors (final Completor [] completors)
	{
		this.completors = completors;
	}


	public Completor [] getCompletors ()
	{
		return this.completors;
	}
}
