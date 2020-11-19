package net.sourceforge.kolmafia.textui;

import java.util.LinkedHashMap;

import net.sourceforge.kolmafia.request.RelayRequest;

public interface ScriptRuntime
{
	public enum State
	{
		NORMAL, RETURN, BREAK, CONTINUE, EXIT
	};

	public ScriptException runtimeException( final String message );
	public ScriptException runtimeException2( final String message1, final String message2 );
	public RelayRequest getRelayRequest();
	public StringBuffer getServerReplyBuffer();
	public State getState();
	public void setState( final State newState );
	public LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> getBatched();
	public void setBatched( LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched );
}
