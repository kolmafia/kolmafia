function basementUpdate( command )
{
	var httpObject = getHttpObject();
	if ( !httpObject )
		return;

	isRefreshing = true;
	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		var bodyBegin = httpObject.responseText.indexOf( ">", httpObject.responseText.indexOf( "<body" ) ) + 1;
		var bodyEnd = httpObject.responseText.indexOf( "</body>" );

		if ( bodyBegin > 0 && bodyEnd > 0 )
		{
			top.charpane.document.getElementsByTagName( "body" )[0].innerHTML =
				httpObject.responseText.substring( bodyBegin, bodyEnd );
		}

		isRefreshing = false;
		document.location.href = "basement.php";
	}

	var selects = document.getElementsByTagName( "select" );
	for ( var i = 0; i < selects.length; ++i )
		selects[i].disabled = true;

	var buttons = document.getElementsByTagName( "input" );
	for ( var j = 0; j < buttons.length; ++j )
		buttons[j].disabled = true;

	httpObject.open( "GET", "/KoLmafia/sideCommand?cmd=" + encodeURIComponent( command ) + "&MAFIAHIT" );
	httpObject.send( "" );
}


function changeBasementGear()
{
	var select = document.getElementById( "gear" );
	if ( select.selectedIndex != 0 )
		basementUpdate( select.options[select.selectedIndex].value );
}


function changeBasementEffects()
{
	var command = "";
	var select = document.getElementById( "potion" );

	var current;

	for ( var i = 0; i < select.options.length; ++i )
	{
		if ( select.options[i].selected )
		{
			current = select.options[i].innerHTML;
			current = current.substring( 0, current.lastIndexOf( " (" ) );

			if ( current.indexOf( "acquire " ) == 0 )
				current = current.substring( current.indexOf( "&amp; " ) + 6 );
			else if ( current.indexOf( "(~" ) == 0 )
				current = current.substring( current.indexOf( ") " ) + 2 );

			command += current + "; ";
		}
	}

	basementUpdate( command );
}


function computeNetBoost( value, target )
{
	var boost = 0;
	var unknown = false;
	var select = document.getElementById( "potion" );

	for ( var i = 0; i < select.options.length; ++i )
	{
		if ( select.options[i].selected )
		{
			if ( select.options[i].disabled )
				select.options[i].selected = false;
			else if ( 1 * select.options[i].value == 0 )
				unknown = true;
			else
				boost += 1 * select.options[i].value;
		}
	}

	var changevalue = getObject( "changevalue" );
	var changetarget = getObject( "changetarget" );

	if ( unknown )
	{
		changevalue.innerHTML = "???";
		changetarget.innerHTML = "???";
	}
	else
	{
		changevalue.innerHTML = "" + (value + boost);
		changetarget.innerHTML = "" + target;
	}

	if ( boost == 0 && !unknown )
		changevalue.style.color = "black";
	else
		changevalue.style.color = "blue";
}


function runBasementScript()
{
	basementUpdate( "divehelp" );
}