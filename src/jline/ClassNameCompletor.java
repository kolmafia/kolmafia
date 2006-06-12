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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;


/**
 *  A Completor implementation that completes java class names. By default,
 *  it scans the java class path to locate all the classes.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ClassNameCompletor
	extends SimpleCompletor
{
	/**
	 *  Complete candidates using all the classes available in the
	 *  java <em>CLASSPATH</em>.
	 */
	public ClassNameCompletor ()
		throws IOException
	{
		this (null);
	}


	public ClassNameCompletor (final SimpleCompletorFilter filter)
		throws IOException
	{
		super (getClassNames (), filter);
		setDelimiter (".");
	}


	public static String[] getClassNames ()
		throws IOException
	{
		Set urls = new HashSet ();
		for (ClassLoader loader = ClassNameCompletor.class.getClassLoader ();
			loader != null; loader = loader.getParent ())
		{
			if (!(loader instanceof URLClassLoader))
				continue;

			urls.addAll (Arrays.asList (((URLClassLoader)loader).getURLs ()));
		}

		// Now add the URL that holds java.lang.String. This is because
		// some JVMs do not report the core classes jar in the list of
		// class loaders.
		Class[] systemClasses = new Class[] {
			String.class,
			javax.swing.JFrame.class
			};
		for (int i = 0; i < systemClasses.length; i++)
		{
			URL classURL = systemClasses[i].getResource ("/"
				+ systemClasses[i].getName ().replace ('.', '/') + ".class");
			if (classURL != null)
			{
				URLConnection uc = (URLConnection)classURL.openConnection ();
				if (uc instanceof JarURLConnection)
					urls.add (((JarURLConnection)uc).getJarFileURL ());
			}
		}


		Set classes = new HashSet ();
		for (Iterator i = urls.iterator (); i.hasNext (); )
		{
			URL url = (URL)i.next ();
			File file = new File (url.getFile ());
			if (file.isDirectory ())
			{
				Set files = getClassFiles (file.getAbsolutePath (),
					new HashSet (), file, new int[] { 200 });
				classes.addAll (files);
				continue;
			}

			if (file == null || !file.isFile ()) // TODO: handle directories
				continue;

			JarFile jf = new JarFile (file);
			for (Enumeration entries = jf.entries ();
				entries.hasMoreElements () ;)
			{
				JarEntry entry = (JarEntry)entries.nextElement ();
				if (entry == null)
					continue;

				String name = entry.getName ();
				if (!name.endsWith (".class")) // only use class files
					continue;

				classes.add (name);
			}
		}

		// now filter classes by changing "/" to "." and trimming the
		// trailing ".class"
		Set classNames = new TreeSet ();
		for (Iterator i = classes.iterator (); i.hasNext (); )
		{
			String name = (String)i.next ();
			classNames.add (name.replace ('/', '.').substring (0,
				name.length () - 6));
		}

		return (String[])classNames.toArray (new String[classNames.size ()]);
	}


	private static Set getClassFiles (String root, Set holder, File directory,
		int[] maxDirectories)
	{
		// we have passed the maximum number of directories to scan
		if (maxDirectories[0]-- < 0)
			return holder;

		File[] files = directory.listFiles ();
		for (int i = 0; files != null && i < files.length; i++)
		{
			String name = files[i].getAbsolutePath ();
			if (!(name.startsWith (root)))
				continue;
			else if (files[i].isDirectory ())
				getClassFiles (root, holder, files[i], maxDirectories);
			else if (files[i].getName ().endsWith (".class"))
				holder.add (files[i].getAbsolutePath ().substring (
					root.length () + 1));
		}

		return holder;
	}
}

