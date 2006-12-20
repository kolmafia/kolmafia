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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *	<p>
 *  A simple {@link Completor} implementation that handles a pre-defined
 *  list of completion words.
 *  </p>
 *
 *	<p>
 *  Example usage:
 *  </p>
 *  <pre>
 *  myConsoleReader.addCompletor (new SimpleCompletor (new String [] { "now", "yesterday", "tomorrow" }));
 *  </pre>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class SimpleCompletor
	implements Completor, Cloneable
{
	/**
	 *  The list of candidates that will be completed.
	 */
	SortedSet candidates;


	/**
	 *  A delimiter to use to qualify completions.
	 */
	String delimiter;

	final SimpleCompletorFilter filter;


	/**
	 *  Create a new SimpleCompletor with a single possible completion
	 *  values.
	 */
	public SimpleCompletor (final String candidateString)
	{
		this (new String [] { candidateString });
	}


	/**
	 *  Create a new SimpleCompletor with a list of possible completion
	 *  values.
	 */
	public SimpleCompletor (final String [] candidateStrings)
	{
		this (candidateStrings, null);
	}


	public SimpleCompletor (final String[] strings,
		final SimpleCompletorFilter filter)
	{
		this.filter = filter;
		setCandidateStrings (strings);
	}


	/**
	 *  Complete candidates using the contents of the specified Reader.
	 */
	public SimpleCompletor (final Reader reader)
		throws IOException
	{
		this (getStrings (reader));
	}


	/**
	 *  Complete candidates using the whitespearated values in
	 *  read from the specified Reader.
	 */
	public SimpleCompletor (final InputStream in)
		throws IOException
	{
		this (getStrings (new InputStreamReader (in)));
	}


	private static String [] getStrings (final Reader in)
		throws IOException
	{
		final Reader reader = in instanceof BufferedReader
			? in
			: new BufferedReader (in);

		List words = new LinkedList ();
		String line;
		while ((line = ((BufferedReader)reader).readLine ()) != null)
		{
			for (StringTokenizer tok = new StringTokenizer (line);
				tok.hasMoreTokens (); words.add (tok.nextToken ()));
		}

		return (String [])words.toArray (new String [words.size ()]);
	}


	public int complete (final String buffer, final int cursor,
		final List clist)
	{
		String start = buffer == null ? "" : buffer;

		SortedSet matches = candidates.tailSet (start);
		for (Iterator i = matches.iterator (); i.hasNext (); )
		{
			String can = (String)i.next ();
			if (!(can.startsWith (start)))
				break;

			if (delimiter != null)
			{
				int index = can.indexOf (delimiter, cursor);
				if (index != -1)
					can = can.substring (0, index + 1);
			}
			clist.add (can);
		}

		if (clist.size () == 1)
			clist.set (0, ((String)clist.get (0)) + " ");

		// the index of the completion is always from the beginning of
		// the buffer.
		return clist.size () == 0 ? -1 : 0;
	}


	public void setDelimiter (final String delimiter)
	{
		this.delimiter = delimiter;
	}


	public String getDelimiter ()
	{
		return this.delimiter;
	}



	public void setCandidates (final SortedSet candidates)
	{
		if (filter != null)
		{
			TreeSet filtered = new TreeSet ();
			for (Iterator i = candidates.iterator (); i.hasNext (); )
			{
				String element = (String)i.next ();
				element = filter.filter (element);
				if (element != null)
					filtered.add (element);
			}

			this.candidates = filtered;
		}
		else
		{
			this.candidates = candidates;
		}
	}


	public SortedSet getCandidates ()
	{
		return Collections.unmodifiableSortedSet (this.candidates);
	}


	public void setCandidateStrings (final String[] strings)
	{
		setCandidates (new TreeSet (Arrays.asList (strings)));
	}


	public void addCandidateString (final String candidateString)
	{
		final String string = filter == null
			? candidateString
			: filter.filter (candidateString);

		if (string != null)
			candidates.add (string);
	}


	public Object clone ()
		throws CloneNotSupportedException
	{
		return super.clone ();
	}


	/**
	 *  Filter for elements in the completor.
	 *
	 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
	 */
	public static interface SimpleCompletorFilter
	{
		/**
		 *  Filter the specified String. To not filter it, return the
		 *  same String as the parameter. To exclude it, return null.
		 */
		public String filter (String element);
	}


	public static class NoOpFilter
		implements SimpleCompletorFilter
	{
		public String filter (final String element)
		{
			return element;
		}
	}
}
