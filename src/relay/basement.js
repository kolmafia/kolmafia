function basementUpdate( select, command )
{
	var httpObject1 = getHttpObject();
	if ( !httpObject1 )
	return true;

	isRefreshing = true;
	httpObject1.onreadystatechange = function()
	{
		if ( httpObject1.readyState != 4 )
			return;

		var bodyBegin = httpObject1.responseText.indexOf( ">", httpObject1.responseText.indexOf( "<body" ) ) + 1;
		var bodyEnd = httpObject1.responseText.indexOf( "</body>" );

		if ( bodyBegin > 0 && bodyEnd > 0 )
		{
			top.charpane.document.getElementsByTagName( "body" )[0].innerHTML =
				httpObject1.responseText.substring( bodyBegin, bodyEnd );
		}

		isRefreshing = false;
		document.location.href = "basement.php";
	}

	select.disabled = true;
	httpObject1.open( "POST", "/KoLmafia/sideCommand?cmd=" + command );
	httpObject1.send( "" );
}


function changeBasementOutfit()
{
	var select = document.getElementById( "outfit" );
	var option = select.options[select.selectedIndex];
	basementUpdate( select, "outfit+" + option.value );
}


function changeBasementPotion()
{
	var select = document.getElementById( "potion" );
	var option = select.options[select.selectedIndex];
	basementUpdate( select, option.value );
}