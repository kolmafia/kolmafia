package net.sourceforge.kolmafia.request;

public class InternalChatRequest
	extends ChatRequest
{
	/**
	 * Constructs a new <code>InternalChatRequest</code> that will send the given
	 * string to the server.
	 *
	 * @param graf       The message to be sent
	 */

	public InternalChatRequest( final String graf )
	{
		// Build a ChatRequest to send the given string using non-tabbed chat

		super( graf, false );

		// Mark it as NOT a "chat" message, since it's not really being
		// used to chat; we are just making a request of KoL via chat.

		this.isChatRequest = false;
	}
}
