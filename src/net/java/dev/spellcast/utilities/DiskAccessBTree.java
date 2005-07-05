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

// input output
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

// Java utilities
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;

import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * As a database begins to grow in size, one of the things that becomes
 * important is memory usage.  Thus, many databases which reach these
 * sizes instead become massive look-up tables stored on disk.  However,
 * supposing that a database only uses a single key, it would be to one's
 * advantage to have an extremely fast search structure.  The purpose of
 * this class is to allow for a search-tree-style on-disk database which
 * possesses the benefit of not being extremely expensive in terms of memory
 * usage, but provides speeds comparable to an on-disk balanced binary tree
 * that does not suffer severely from disk-access speeds.  In order to
 * accomplish this, the <code>DiskAccessBTree</code> constructs two files:
 * a directory file (with the .dir extension) used as a look-up table, and
 * a data file (with the .dat extension) containing the actual serialized
 * data.  Note that multiple keys could theoretically be allowed through
 * the creation of multiple .dir files; however, this functionality is yet
 * to be implemented.
 */

public class DiskAccessBTree extends AbstractBTree
{
	private static final int MAGIC_NUMBER = "DiskAccessBTree".hashCode();
	private static final int DEFAULT_NODE_SIZE = 1024;

	private int nodeSize;
	private long rootOffset;
	private String baseFilename, databaseName;

	private RandomObjectAccessFile dirfile, keyfile, datfile;
	private DiskAccessBTreeNode root;

	/**
	 * Constructs a new <code>DiskAccessBTree</code> using the current
	 * date and time as the filename, prefixed by "DiskAccessBTree-".
	 * The default key will be of type Comparable, with no other types
	 * of specifications beyond that.
	 */

	public DiskAccessBTree()
	{
		super( Comparable.class );

		try
		{
			SimpleDateFormat dateformat = new SimpleDateFormat( "yyyyMMdd-kkmmss.SS" );
			this.baseFilename = "DiskAccessBTree-" + dateformat.format( Calendar.getInstance().getTime() );
			initialize( baseFilename, DEFAULT_NODE_SIZE );
		}
		catch ( IOException e )
		{	throw new RuntimeException( e );
		}
	}

	/**
	 * Constructs a new <code>DiskAccessBTree</code> using the current
	 * date and time as the filename, prefixed by "DiskAccessBTree-".
	 * The default key will be of type Comparable, with no other types
	 * of specifications beyond that.  The initial elements of the tree
	 * will be those contained in the given map.
	 *
	 * @param	t	The map with which this <code>DiskAccessBTree</code> will be initialized
	 */

	public DiskAccessBTree( Map t )
	{	this();  putAll( t );
	}

	/**
	 * Constructs a new <code>DiskAccessBTree</code> using the given base file
	 * name for the .dir and .dat files, with each node in the directory file
	 * having the default number of elements.
	 *
	 * @param	baseFilename	The extensionless filename to be used for the DiskAccessBTree
	 */

	public DiskAccessBTree( String baseFilename )
		throws IOException
	{
		this( Comparable.class, baseFilename, baseFilename, DEFAULT_NODE_SIZE );
	}

	/**
	 * Constructs a new <code>DiskAccessBTree</code> using the given base file
	 * name for the .dir and .dat files, with each node in the directory file
	 * having the given number of elements.  Note that the name of the database
	 * is used as a key to access the files - future recreations of the DiskAccessBTree will
	 * require this database name to be resupplied.
	 *
	 * @param	keyClass	The class of which all keys will be instances
	 * @param	baseFilename	The extensionless filename to be used for the DiskAccessBTree
	 * @param	databaseName	The name of the database
	 * @param	nodeSize	The size of each node in the directory file
	 */

	public DiskAccessBTree( Class keyClass, String baseFilename, String databaseName, int nodeSize )
		throws IOException
	{
		super( keyClass );
		this.baseFilename = baseFilename;
		initialize( databaseName, nodeSize );
	}

	/**
	 * An internal method used for creating the files which will be used by the
	 * B-Tree.  As noted by the class description, there are two files that are
	 * used in order to provide the B-Tree functionality; this method creates
	 * the two files in the event that they do not exist, and initializes them
	 * with the appropriate information.
	 *
	 * @param	databaseName	The name of the database
	 * @param	nodeSize	The size of each node in the directory file
	 */

	private void initialize( String databaseName, int nodeSize )
		throws IOException
	{
		this.nodeSize = nodeSize;
		this.databaseName = databaseName;

		// create the abstract files
		boolean createdFiles = false;
		File dirfile = new File( baseFilename + ".dir" );
		File keyfile = new File( baseFilename + ".key" );
		File datfile = new File( baseFilename + ".dat" );

		// create the files in the event that they do not exist;
		// however, if only one of the files exists, that means
		// that either one of the B-Tree files was deleted or a
		// second application has created a file with the default
		// dir/dat extensions - either scenario translates to an
		// unreadable B-Tree

		if ( !dirfile.exists() && !keyfile.exists() && !datfile.exists() )
		{
			createdFiles = true;
			File parentDirectory = dirfile.getParentFile();
			parentDirectory.mkdirs();

			dirfile.createNewFile();
			keyfile.createNewFile();
			datfile.createNewFile();
		}
		else if ( !dirfile.exists() )
			throw new RuntimeException( "DiskAccessBTree initialization failed:  missing " + dirfile.getName() );
		else if ( !keyfile.exists() )
			throw new RuntimeException( "DiskAccessBTree initialization failed:  missing " + keyfile.getName() );
		else if ( !datfile.exists() )
			throw new RuntimeException( "DiskAccessBTree initialization failed:  missing " + datfile.getName() );


		// enable random access to the two files, and in the event
		// that they were recently created, also write the data
		// and directory headers to the files so that they can be
		// properly read from

		this.dirfile = new RandomObjectAccessFile( dirfile, "rw" );
		this.keyfile = new RandomObjectAccessFile( keyfile, "rw" );
		this.datfile = new RandomObjectAccessFile( datfile, "rw" );

		if ( createdFiles )
		{
			rootOffset = 0;
			updateDatabaseSize( 0 );

			// in actuality, there is no root - thus, a root must
			// be created so that the root can be properly loaded,
			// and the *true* offset needs to be written to disk

			rootOffset = this.dirfile.length();
			(new DiskAccessBTreeNode( this.dirfile, this.keyfile, this.datfile, -1, 0, nodeSize )).getParent();
			writeFileHeaders();
		}

		readFileHeaders();

		if ( this.nodeSize != nodeSize )
			throw new IOException( "DiskAccessBTree initialization failed:  node size mismatch" );
	}

	/**
	 * An internal method used to read the file headers and validate them.
	 * Calling this function initializes all the relevant information to
	 * the files used by the <code>DiskAccessBTree</code>.
	 */

	private void readFileHeaders()
		throws IOException
	{
		// validate the directory file associated with this B-Tree;
		// if the file is invalid, throw an IOException

		dirfile.seek(0);
		if ( dirfile.readInt() != MAGIC_NUMBER )
			throw new IOException( dirfile.getName() + " is not a DiskAccessBTree directory file" );
		if ( !dirfile.readUTF().equals( databaseName ) )
			throw new IOException( dirfile.getName() + " is not a " + databaseName + " database" );

		String keyClassName = dirfile.readUTF();
		try
		{	setKeyClass( Class.forName( keyClassName ) );
		}
		catch ( ClassNotFoundException e )
		{	throw new IOException( "The class " + keyClassName + " does not exist in this runtime environment" );
		}

		// validate the key file associated with this B-Tree; if
		// the file is invalid, throw an IOException

		keyfile.seek(0);
		if ( keyfile.readInt() != MAGIC_NUMBER )
			throw new IOException( keyfile.getName() + " is not a DiskAccessBTree database file" );
		if ( !keyfile.readUTF().equals( databaseName ) )
			throw new IOException( keyfile.getName() + " is not a " + databaseName + " database" );

		// validate the database file associated with this B-Tree;
		// if the file is invalid, throw an IOException

		datfile.seek(0);
		if ( datfile.readInt() != MAGIC_NUMBER )
			throw new IOException( datfile.getName() + " is not a DiskAccessBTree database file" );
		if ( !datfile.readUTF().equals( databaseName ) )
			throw new IOException( datfile.getName() + " is not a " + databaseName + " database" );

		// read in the information from the directory file
		// that will be used in locating information in
		// the database

		int nodeSize = dirfile.readInt();
		if ( this.nodeSize != nodeSize )
			throw new IOException( "node size mismatch for DiskAccessBTree: expected " + this.nodeSize + ", read " + nodeSize );

		rootOffset = dirfile.readLong();
		setRoot( new DiskAccessBTreeNode( dirfile, keyfile, datfile, rootOffset ) );
		updateDatabaseSize( dirfile.readInt() );
	}

	/**
	 * An internal method used to write the file headers to the given.
	 * file.  Note that this function should only be used if it can be
	 * made certain that the name of the database has not been changed
	 * from the first time the database files were initialized.
	 */

	private void writeFileHeaders()
	{
		try
		{
			// reset the file pointers to the beginning of the file
			// where the file headers are stored, and write the file
			// identifier information

			dirfile.seek(0);  dirfile.writeInt( MAGIC_NUMBER );
			keyfile.seek(0);  keyfile.writeInt( MAGIC_NUMBER );
			datfile.seek(0);  datfile.writeInt( MAGIC_NUMBER );

			// now that the file identifiers have been written,
			// write the name of the database to the header

			dirfile.writeUTF( databaseName );
			keyfile.writeUTF( databaseName );
			datfile.writeUTF( databaseName );

			// the directory file also includes other information
			// for recreating the database

			dirfile.writeUTF( getKeyClass().getName() );
			dirfile.writeInt( nodeSize );
			dirfile.writeLong( rootOffset );
			dirfile.writeInt( size() );
		}
		catch ( IOException e )
		{	throw new RuntimeException( "An IOException occurred while attempting to write the file header", e );
		}
	}

	/**
	 * Please refer to {@link java.util.Map#clear()} for more information
	 * regarding this function.
	 */

	public void clear()
	{
		try
		{
			File keyfile = this.keyfile.getAssociatedFile();
			this.keyfile.close();
			this.keyfile = null;

			File dirfile = this.dirfile.getAssociatedFile();
			this.dirfile.close();
			this.dirfile = null;

			File datfile = this.datfile.getAssociatedFile();
			this.datfile.close();
			this.datfile = null;

			keyfile.delete();  dirfile.delete();  datfile.delete();
			initialize( databaseName, nodeSize );
		}
		catch ( IOException e )
		{	throw new RuntimeException( "Delete and file recreate failed:  clear operation was not successful", e );
		}
	}

	/**
	 * In addition to the standard <code>put()</code> operation ensuring
	 * that the elements match the intended class, the <code>DiskAccessBTree</code>
	 * also ensures that the elements are <code>Serializable</code> as well.
	 */

	public Object put( Object key, Object element )
	{
		if ( !(element instanceof Serializable) )
			throw new IllegalArgumentException( "All elements must be serializable in a DiskAccessBTree" );
		return super.put( key, element );
	}

	/**
	 * Overrides the default <code>updateDatabaseSize</code> method with
	 * the updating of the file headers, in addition to the modification
	 * of the database information.
	 */

	protected void updateDatabaseSize( int databaseSize )
	{
		super.updateDatabaseSize( databaseSize );
		writeFileHeaders();
	}
}