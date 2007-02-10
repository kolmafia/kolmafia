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


function refreshSidebar()
{
	var httpObject = getHttpObject();
	if ( !httpObject )
	return true;

	isRefreshing = true;
	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		top.charpane.document.getElementsByTagName( "body" )[0].innerHTML =
			httpObject.responseText.replace( new RegExp( "<html>.*<body[^>]*>", "g" ), "" ).replace( new RegExp( "</body>.*</html>", "g" ), "" );

		isRefreshing = false;
	}

	httpObject.open( "GET", "http://" + window.location.host + "/charpane.php" );
	httpObject.send( null );
}


function inlineLoad( location, id )
{
	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.onreadystatechange = function()
	{
		if ( httpObject.readyState != 4 )
			return;

		var toRemove = getObject( "multiuse" + id );
		if ( toRemove )
			toRemove.style.display = "none";

		toRemove = getObject( "link" + id )
		if ( toRemove )
			toRemove.style.display = "none";

		refreshSidebar();
	};

	httpObject.open( "GET", "http://" + window.location.host + "/" + location );
	httpObject.send( null );
	return true;
}


function singleUse( location, id )
{	return inlineLoad( location, id );
}

function multiUse( location, id )
{
	var qfield = "quantity";
	if ( location == "skills.php" )
		qfield = "itemquantity";

	var qvalue = getObject( "quantity" + id ).value;
	return inlineLoad( location + "?pwd&action=useitem&whichitem= " + id + "&" + qfield + "=" + qvalue, id );
}

function showObject( id )
{
	var object = getObject(id);
	if ( !object )
		return false;

	object.style.display = 'inline';
	return true;
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
