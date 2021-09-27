package net.sourceforge.kolmafia.chat;

import java.util.Map;

public class WhoMessage extends EventMessage
{
	private final Map<String, Boolean> contacts;

	public WhoMessage( Map<String, Boolean> contacts, String spacedContent )
	{
		super( spacedContent, null );

		this.contacts = contacts;
	}

	public Map<String, Boolean> getContacts()
	{
		return this.contacts;
	}
}
