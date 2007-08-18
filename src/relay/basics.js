function getObject( id )
{
	if ( top.mainpane.document.getElementById )
		return top.mainpane.document.getElementById( id );
	else if ( top.mainpane.document.all )
		return top.mainpane.document.all[ id ];
	else
		return false;
}


function getHttpObject()
{
	var httpObject = false;
	if ( window.ActiveXObject )
	{
		try
		{
			httpObject = new ActiveXObject( "Msxml2.XMLHTTP" );
		}
		catch ( e )
		{
			httpObject = new ActiveXObject( "Microsoft.XMLHTTP" );
		}
	}
	else
	{
		httpObject = new XMLHttpRequest();
	}

	return httpObject;
};


function refreshSidebar( desiredRefresh )
{
	var httpObject = getHttpObject();
	if ( !httpObject )
	return true;

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
	}

	httpObject.open( "POST", desiredRefresh );
	httpObject.send( "" );
}


function updateDisplay( display, responseText )
{
	if ( responseText == null )
		return;

	if ( responseText.length < 2 )
		return;

	display.innerHTML += responseText;

	if ( display.innerHTML.length > 30000 )
	{
		display.innerHTML = display.innerHTML.substring(
			display.innerHTML.lastIndexOf( "<br>", 10000 ) + 4 );
	}

	display.style.width = initwidth;
	display.scrollTop = display.scrollHeight;

	if ( !isRefreshing && responseText.indexOf("<!-- REFRESH -->") != -1 )
		refreshSidebar( "/sidepane.php" );
}


function inlineLoad( location, fields, id )
{
	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		// Remove the use link and the form so that players
		// do not get confused.

		if ( id )
		{
			var toRemove = getObject( "multiuse" + id );
			if ( toRemove )
				toRemove.style.display = "none";
		}

		// Handle insertion of the data into the page.
		// Steal KoL's divs for doing so.

		var text = "<center>" + httpObject.responseText.substring(
			httpObject.responseText.indexOf( "<table" ), httpObject.responseText.indexOf( "</table><table" ) + 8 ) + "</center>";

		var main = top.mainpane.document;
		var container = main.createElement( "div" );
		container.innerHTML = text;
		container.style.textAlign = "center";

		if ( main.location.href.indexOf( "store" ) != -1 )
			main.body.insertBefore( container, main.body.firstChild );
		else
			main.body.appendChild( container );

		if ( httpObject.responseText.indexOf( "charpane" ) != -1 )
			refreshSidebar( "/charpane.php" );

	};

	httpObject.open( "POST", "/" + location + "?" + fields );
	httpObject.send( null );
	return true;
}


function singleUse( location, fields )
{	return inlineLoad( location, fields, false );
}

function multiUse( location, id )
{
	var qfield = "quantity";
	if ( location == "skills.php" )
		qfield = "itemquantity";

	var qvalue = getObject( "quantity" + id ).value;
	return inlineLoad( location, "pwd=&action=useitem&whichitem= " + id + "&" + qfield + "=" + qvalue, id );
}

function showObject( id )
{
	var object = getObject(id);
	if ( !object )
		return false;

	object.style.display = 'inline';
	return true;
}


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
		return true;
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


// ====================================================================
//			 URLEncode and URLDecode functions
//
// Copyright Albion Research Ltd. 2002
// httpObject://www.albionresearch.com/
//
// You may copy these functions providing that
// ( a ) you leave this copyright notice intact, and
// ( b ) if you use these functions on a publicly accessible
//		 web site you include a credit somewhere on the web site
//		 with a link back to http://www.albionresarch.com/
//
// If you find or fix any bugs, please let us know at albionresearch.com
//
// SpecialThanks to Neelesh Thakur for being the first to
// report a bug in URLDecode() - now fixed 2003-02-19.
// ====================================================================

function URLEncode( x )
{
	// The Javascript escape and unescape functions do not correspond
	// with what browsers actually do...
	var SAFECHARS = "0123456789" +					// Numeric
					"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +	// Alphabetic
					"abcdefghijklmnopqrstuvwxyz" +
					"-_.!~*'()";					// RFC2396 Mark characters
	var HEX = "0123456789ABCDEF";

	var plaintext = x;
	var encoded = "";
	for ( var i = 0; i < plaintext.length; i++ ) {
		var ch = plaintext.charAt( i );
		if ( ch=="+" ) {
			encoded+="%2B";
		} else if ( ch == " " ) {
				encoded += "+";				// x-www-urlencoded, rather than %20
		} else if ( SAFECHARS.indexOf( ch ) != -1 ) {
				encoded += ch;
		} else {
				var charCode = ch.charCodeAt( 0 );
			if ( charCode > 255 ) {
					alert( "Unicode Character '" + ch + "' cannot be encoded using standard URL encoding.\n" +
								"( URL encoding only supports 8-bit characters. )\n" +
						"A space ( + ) will be substituted." );
				encoded += "+";
			} else {
				encoded += "%";
				encoded += HEX.charAt( ( charCode >> 4 ) & 0xF );
				encoded += HEX.charAt( charCode & 0xF );
			}
		}
	}

	return encoded;
};

function URLDecode( x )
{
	 // Replace + with ' '
	 // Replace %xx with equivalent character
	 // Put [ERROR] in output if %xx is invalid.
	 var HEXCHARS = "0123456789ABCDEFabcdef";
	 var encoded = x;
	 var plaintext = "";
	 var i = 0;
	 while ( i < encoded.length ) {
			 var ch = encoded.charAt( i );
		 if ( ch == "+" ) {
				 plaintext += " ";
			 i++;
		 } else if ( ch == "%" ) {
			if ( i < ( encoded.length-2 )
					&& HEXCHARS.indexOf( encoded.charAt( i+1 ) ) != -1
					&& HEXCHARS.indexOf( encoded.charAt( i+2 ) ) != -1 ) {
				plaintext += unescape( encoded.substr( i,3 ) );
				i += 3;
			} else {
				alert( 'Bad escape combination near ...' + encoded.substr( i ) );
				plaintext += "%[ERROR]";
				i++;
			}
		} else {
			 plaintext += ch;
			 i++;
		}
	} // while
	 return plaintext;
};
