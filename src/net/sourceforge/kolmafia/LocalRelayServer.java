/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *	  notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *	  notice, this list of conditions and the following disclaimer in
 *	  the documentation and/or other materials provided with the
 *	  distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *	  its contributors may be used to endorse or promote products
 *	  derived from this software without specific prior written
 *	  permission.
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

package net.sourceforge.kolmafia;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.util.Vector;

public class LocalRelayServer implements Runnable
{
	private static final byte [] NEW_LINE = {(byte)'\r', (byte)'\n' };
	private static final int MAX_AGENT_THREADS = 5;
	private static final int TIMEOUT = 5000;

	protected static Vector agentThreads = new Vector();
	protected static int agents = 0;
	
	private ServerSocket serverSocket = null;
	private int port = 60080;
	private boolean listening = false;

	protected static Vector statusMessages = new Vector();

	public int getPort()
	{	return port;
	}
	
	public boolean isRunning()
	{	return listening;
	}

	public synchronized void run()
	{
		try
		{	runServer();
		}
		catch ( Exception e )
		{	
			if ( port > 60090 )
				return;
			else
			{
				port++;
				run();
			}
		}
		finally
		{	close();
		}
	}
	
	private void closeAgents()
	{
		agents = 0;
		synchronized ( agentThreads )
		{
			while ( !agentThreads.isEmpty() )
			{
				RelayAgent agent = (RelayAgent) agentThreads.elementAt( 0 );
				agentThreads.removeElementAt( 0 );
				agent.setSocket( null );
			}
		}
	}

	public void close()
	{
		try
		{
			closeAgents();

			// Close server thread (it's blocking on accept() right now)
			if ( serverSocket != null )
				serverSocket.close();
		}
		catch ( Exception e )
		{	e.printStackTrace();
		}
		finally
		{
			// Reset state for next run()
			serverSocket = null;
			port = 60080;
			listening = false;
			agents = 0;
			agentThreads.removeAllElements();
		}
	}	
	
	private void dispatchAgent( Socket socket )
	{
		RelayAgent agent = null;
		synchronized ( agentThreads )
		{
			for ( int i = 0; i < agentThreads.size(); ++i )
			{
				agent = (RelayAgent) agentThreads.elementAt(i);
				if ( agent.isWaiting() )
				{
					agent.setSocket( socket );
					return;
				}
			}
		}
	}

	public void runServer() throws Exception
	{
		serverSocket = new ServerSocket( port, 25, InetAddress.getByName( "127.0.0.1" ) );

		// Initialize all the agent threads first
		// to ensure every request is caught.

		for ( int i = 0; i < MAX_AGENT_THREADS; ++i )
		{
			RelayAgent agent = new RelayAgent();
			(new Thread( agent )).start();
			agentThreads.add( agent );
		}
		
		listening = true;
		while ( listening )
		{
			try
			{	dispatchAgent( serverSocket.accept() );
			}
			catch ( Exception e )
			{
				// Someone has called serverSocket.close(). Exit thread.
				return;
			}
		}
	}

	public void addStatusMessage( String message )
	{
		synchronized ( statusMessages )
		{
			statusMessages.addElement( message );
		}
	}
	
	public static String getNewStatusMessages()
	{
		StringBuffer messages = new StringBuffer();
		synchronized ( statusMessages )
		{
			while ( !statusMessages.isEmpty() )
			{
				messages.append( (String) statusMessages.elementAt( 0 ) );
				statusMessages.removeElementAt( 0 );
			}
		}

		return messages.toString();
	}	

	private class RelayAgent implements Runnable 
	{		
		protected Socket socket = null;
		
		boolean isWaiting()
		{	return socket == null;
		}
		
		synchronized void setSocket( Socket socket )
		{
			this.socket = socket;
			notify();
		}
		
		public synchronized void run()
		{
			while ( true )
			{
				if ( socket == null )
				{
					// wait for a client
					try
					{	wait();
					}
					catch ( InterruptedException e )
					{	continue;
					}
				}
				
				try
				{	performRelay();
				}
				catch ( Exception e )
				{	e.printStackTrace();
				}
				
				socket = null;
			}
		}
		
		protected void sendHeaders( PrintStream printStream, LocalRelayRequest request ) throws IOException
		{
			String header = null;
			for ( int i = 0; null != ( header = request.getHeader( i ) ); ++i )
			{
				printStream.print( header );
				printStream.write( NEW_LINE );
			}  	
		}
		
		protected void performRelay() throws IOException
		{
			if ( socket == null )
				return;
			
			socket.setSoTimeout( TIMEOUT );
			socket.setTcpNoDelay( true );
			
			try
			{
				BufferedReader reader = new BufferedReader( new InputStreamReader( new BufferedInputStream( socket.getInputStream() ) ) );
				PrintStream printStream = new PrintStream( socket.getOutputStream() );
				
				String path;
				String method;
				
				String [] tokens = reader.readLine().trim().split( " " );
				method = tokens[0];
				LocalRelayRequest request = new LocalRelayRequest( StaticEntity.getClient(), tokens[1], false );
				
				int contentLength = 0;
				while ( true )
				{
					String line = reader.readLine();

					if ( line == null || line.trim().length() <= 0 )
						break;
					
					if ( line.indexOf( ": " ) == -1 )
						continue;
					
					tokens = line.split( "(: )" );
					if ( tokens[0].equals( "Content-Length" ) )
						contentLength = Integer.parseInt( tokens[1].trim(), 10 );
				}
				if ( method.equals( "POST" ) )
				{
					StringBuffer postBuffer = new StringBuffer();
					for ( int i = 0; i < contentLength; ++i )
						postBuffer.append( (char) reader.read() );

					request.addEncodedFormFields( postBuffer.toString() );
				}
				
				request.run();
				sendHeaders( printStream, request );
				printStream.write( NEW_LINE );
				printStream.print( request.getFullResponse() );
				
			}
			finally
			{	socket.close();
			}
		}
	}
}
