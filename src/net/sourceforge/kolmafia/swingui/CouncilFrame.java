package net.sourceforge.kolmafia.swingui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.request.CouncilRequest;

public class CouncilFrame
	extends RequestFrame
{
	public CouncilFrame()
	{
		super( "Council of Loathing" );
	}

	@Override
	public void setVisible( boolean isVisible )
	{
		super.setVisible( isVisible );

		if ( isVisible )
		{
			this.displayRequest( new CouncilRequest() );
		}
	}

	@Override
	public boolean hasSideBar()
	{
		return false;
	}

	private static final Pattern FORM_PATTERN = Pattern.compile( "(<form.*?</form>)", Pattern.DOTALL );

	@Override
	public String getDisplayHTML( final String responseText )
	{
		Matcher matcher = FORM_PATTERN.matcher( responseText );
		String form = matcher.find() ? matcher.group( 0 ) : "";
		return super.getDisplayHTML( responseText )
			.replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" )
			.replaceFirst( form, "" )
			.replaceFirst( "table width=95%", "table width=100%" );
	}
}
