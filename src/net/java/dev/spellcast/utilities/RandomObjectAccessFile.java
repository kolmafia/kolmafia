/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.java.dev.spellcast.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The purpose of this class is to add the ability to read and write
 * objects to a file through random access.  The various elements of
 * the constructor are more thoroughly described in the documentation
 * for {@link java.io.RandomAccessFile}.
 */

public class RandomObjectAccessFile extends java.io.RandomAccessFile
{
	private File associatedFile;

	/**
	 * The constructor for a <code>RandomObjectAccessFile</code> that
	 * merely calls the super constructor.  For more information,
	 * please see the constructor for the <code>RandomAccessFile</code>
	 * class, which details what each element means.
	 *
	 * @param	filename	The name of the file to be loaded
	 * @param	mode	The file access mode to be used
	 */

	public RandomObjectAccessFile( String filename, String mode )
		throws FileNotFoundException
	{	this ( new File( filename ), mode );
	}

	/**
	 * The constructor for a <code>RandomObjectAccessFile</code>
	 * that merely calls the super constructor with the given file,
	 * specifying read-write access.  Note, however, that the file
	 * must exist, or an exception will be thrown.
	 *
	 * @param	file	The file to be loaded
	 * @param	mode	The file access mode to be used
	 */

	public RandomObjectAccessFile( File file, String mode )
		throws FileNotFoundException
	{
		super( file, mode );
		this.associatedFile = file;
	}

	/**
	 * A method used to retrieve the name of the file which is currently
	 * being accessed by this <code>RandomObjectAccessFile</code>.  In
	 * essence, this function is identical to the value returned if the
	 * method <code>getName()</code> were called on the file object
	 * accessed by this <code>RandomObjectAccessFile</code>.
	 *
	 * @return	The name of the file being accessed
	 */

	public String getName()
	{	return associatedFile.getName();
	}

	/**
	 * Returns the file associated with this <code>RandomObjectAccessFile</code>.
	 * This can be used in the event that you wish to do something else with the
	 * file.  Note that on some operating systems, no other program will have access
	 * to the file if the random access permits writing.
	 *
	 * @return	The file associated with this <code>RandomObjectAccessFile</code>.
	 */

	public File getAssociatedFile()
	{	return associatedFile;
	}

	/**
	 * Reads an object from this file, starting from the current file pointer.
	 * Because of its use of underlying methods of <code>RandomAccessFile</code>,
	 * this method naturally blocks until all the information is read.  Note
	 * that because there is no natural way to convert an object to bytes, this
	 * method utilizes a combination of a <code>ObjectInputStream</code> and a
	 * <code>ByteArrayInputStream</code> in order to convert the bytes read
	 * from the file to the appropriate object.
	 *
	 * @return	The next object in the file
	 */

	public final Object readObject()
		throws IOException, ClassNotFoundException
	{
		byte [] buffer = new byte[readInt()];  readFully( buffer );
		java.io.ObjectInputStream oistream = new java.io.ObjectInputStream(
			new java.io.ByteArrayInputStream( buffer ) );
		Object content = oistream.readObject();  oistream.close();  oistream = null;
		return content;
	}

	/**
	 * Reads an object from this file, starting from the given file offset.
	 * This method is shorthand for <code>seek( offset )</code>;
	 * <code>readObject()</code>.
	 *
	 * @param	offset	The offset into the file at which the object is to be found
	 * @return	The object in the file located at the given offset
	 */

	public final Object readObject( long offset )
		throws IOException, ClassNotFoundException
	{
		seek( offset );
		return readObject();
	}

	/**
	 * Writes an object to the file, starting from the current file pointer.
	 * Note that because there is no direct method of doing this, the method
	 * uses a combination of an <code>ObjectOutputStream</code> and an
	 * <code>ByteArrayOutputStream</code> in order to convert the object into
	 * an array of bytes, which are then written to the <code>RandomAccessFile</code>.
	 *
	 * @param	o	The object to be written to the file
	 */

	public final void writeObject( Object o )
		throws IOException
	{
		writeObject( o, Integer.MAX_VALUE );
	}

	/**
	 * Writes an object to the file, starting from the current file pointer,
	 * and ensures that the number of bytes written does not exceed the given
	 * value.  Note that because there is no direct method of doing this, the
	 * method uses a combination of an <code>ObjectOutputStream</code> and an
	 * <code>ByteArrayOutputStream</code> in order to convert the object into
	 * an array of bytes, which are then written to the <code>RandomAccessFile</code>.
	 *
	 * @param	o	The object to be written to the file
	 */

	public final void writeObject( Object o, int maxByteCount )
		throws IOException
	{
		java.io.ByteArrayOutputStream bostream = new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream oostream = new java.io.ObjectOutputStream( bostream );
		oostream.writeObject( o );  oostream.flush();  oostream.close();  oostream = null;
		byte [] buffer = bostream.toByteArray();  bostream = null;

		if ( buffer.length + 4 > maxByteCount )
			throw new IllegalArgumentException(
				"The serialized form for this object was asserted to be, at most, " + maxByteCount +
					" bytes wide; the supplied object will be " + (buffer.length + 4) + " bytes wide" );

		writeInt( buffer.length );
		write( buffer );
	}

	/**
	 * Appends the object to the end of the file.  This method is shorthand
	 * for <code>seek( length() )</code>; <code>writeObject( o )</code>.
	 *
	 * @param	o	The object to be appended to the file
	 */

	public void appendObject( Object o )
		throws IOException
	{
		seek( length() );
		writeObject( o );
	}
}