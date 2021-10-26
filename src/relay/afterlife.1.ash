static int [string] classes = {
    "Seal Clubber" : 1,
    "Turtle Tamer" : 2,
    "Pastamancer" : 3,
    "Sauceror" : 4,
    "Disco Bandit" : 5,
    "Accordion Thief" : 6,
};

static int [string] signs = {
    "Mongoose" : 1,
    "Wallaby" : 2,
    "Vole" : 3,
    "Platypus" : 4,
    "Opossum" : 5,
    "Marmot" : 6,
    "Wombat" : 7,
    "Blender" : 8,
    "Packrat" : 9,
};

static boolean[item] equipment = $items[
    astral bludgeon,
    astral shield,
    astral chapeau,
    astral bracer,
    astral longbow,
    astral shorts,
    astral mace,
    astral trousers,
    astral ring,
    astral statuette,
    astral pistol,
    astral mask,
    astral pet sweater,
    astral shirt,
    astral belt,
];

static boolean[item] consumables = $items[
    astral hot dog,
    astral pilsner,
    steel margarita,
];

static string [string, string, item ] pet_modifiers;
static string [string, string, item ] consumable_modifiers;

static {
    file_to_map( "data/TCRS.astral_pets.txt", pet_modifiers );
    file_to_map( "data/TCRS.astral_consumables.txt", consumable_modifiers );
}

void insert_class_sign_dropdowns( buffer html, string class_id, string class_function, string sign_id, string sign_function )
{
    html.append( "Select a Class and a Sign to see modifiers in the Two Crazy Random Summer path" );

    html.append( "<p><select id='" );
    html.append( class_id );
    html.append( "' onChange=\"" );
    html.append( class_function );
    html.append( "();\">" );
    html.append( "<option value=\"0\"> -- Pick a Class -- </option>" );
    html.append( "<option value=\"1\">Seal Clubber</option>" );
    html.append( "<option value=\"2\">Turtle Tamer</option>" );
    html.append( "<option value=\"3\">Pastamancer</option>" );
    html.append( "<option value=\"4\">Sauceror</option>" );
    html.append( "<option value=\"5\">Disco Bandit</option>" );
    html.append( "<option value=\"6\">Accordion Thief</option>" );
    html.append( "</select>" );

    html.append( "&nbsp;&nbsp;&nbsp;&nbsp;" );

    html.append( "<select id='" );
    html.append( sign_id );
    html.append( "' onChange=\"" );
    html.append( sign_function );
    html.append( "();\">" );
    html.append( "<option value=\"0\"> -- Pick a Sign -- </option>" );
    html.append( "<option value=\"1\">The Mongoose</option>" );
    html.append( "<option value=\"2\">The Wallaby</option>" );
    html.append( "<option value=\"3\">The Vole</option>" );
    html.append( "<option value=\"4\">The Platypus</option>" );
    html.append( "<option value=\"5\">The Opossum</option>" );
    html.append( "<option value=\"6\">The Marmot</option>" );
    html.append( "<option value=\"7\">The Wombat</option>" );
    html.append( "<option value=\"8\">The Blender</option>" );
    html.append( "<option value=\"9\">The Packrat</option>" );
    html.append( "</select>" );

    html.append( "</td></tr>" );
}

void append_line( buffer b, string line )
{
    b.append( line );
    b.append( "\n" );
}

// <tr><td><img style='vertical-align: middle' class=hand src='https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/ast_bludgeon.gif' onclick='descitem(864672857)'></td><td valign=center><b><span onclick='descitem(864672857)'>astral bludgeon<span>&nbsp;&nbsp;&nbsp;&nbsp;</b></td><form action=afterlife.php method=post><input type=hidden name=action value=buyarmory><input type=hidden name=whichitem value=5028><td><input class=button type=submit value="Purchase (10 Karma)"></td></form></tr>

string pet_pattern = "<tr><td><img.*?descitem\\((\\d+)\\).*?(</td>)</form></tr>";

void modify_pet_store( buffer page )
{
    int head = page.index_of( "</head>" );
    if ( head < 0 ) {
	return;
    }

    buffer html;

    // Insert Javascript variables and functions we need.
    html.set_length( 0 );

    html.append_line( "<script language=\"javascript\">" );

    html.append_line( "var petClass = 0;" );
    html.append_line( "var petSign = 0;" );

    html.append_line( "function petDivName( c, s, it ) {" );
    html.append_line( "    return 'petclass' + c + 'sign' + s + 'item' + it;" );
    html.append_line( "}" );
    
    html.append_line( "function petClassSignHideDivs() {" );
    html.append_line( "    for ( var it = 5028; it <= 5042; ++it ) {" );
    html.append_line( "        hidediv( petDivName( '0', '0', it ) );" );
    html.append_line( "    }" );
    html.append_line( "    for ( var c=1; c < 7; ++c ) {" );
    html.append_line( "        for ( var s = 1; s < 10; ++s ) {" );
    html.append_line( "            for ( var it = 5028; it <= 5042; ++it ) {" );
    html.append_line( "                hidediv( petDivName( c, s, it ) );" );
    html.append_line( "            }" );
    html.append_line( "        }" );
    html.append_line( "    }" );
    html.append_line( "}" );

    html.append_line( "function petClassSignShowDivs( c, s ) {" );
    html.append_line( "    for ( var it = 5028; it <= 5042; ++it ) {" );
    html.append_line( "        showdiv( petDivName( c, s, it ) );" );
    html.append_line( "    }" );
    html.append_line( "}" );

    html.append_line( "function petClassSignChange() {" );
    html.append_line( "    if ( petClass == 0 || petSign == 0 ) {" );
    html.append_line( "        petClassSignHideDivs();" );
    html.append_line( "        petClassSignShowDivs(0, 0);" );
    html.append_line( "    } else {" );
    html.append_line( "        petClassSignHideDivs();" );
    html.append_line( "        petClassSignShowDivs(petClass, petSign);" );
    html.append_line( "    }" );
    html.append_line( "}" );

    html.append_line( "function petClassChange() {" );
    html.append_line( "    petClass = document.getElementById( 'petclassselect' ).value;" );
    html.append_line( "    petClassSignChange();" );
    html.append_line( "}" );

    html.append_line( "function petSignChange() {" );
    html.append_line( "    petSign = document.getElementById( 'petsignselect' ).value;" );
    html.append_line( "    petClassSignChange();" );
    html.append_line( "}" );

    html.append( "</script>" );

    page.insert( head, html.to_string() );

    int index = page.index_of( "/table></center></td></tr></table>" );
    if ( index < 0 ) {
	return;
    }

    index = page.index_of( "</table>", index );

    html.set_length( 0 );

    html.append( "<tr><td>" );
    html.insert_class_sign_dropdowns( "petclassselect", "petClassChange", "petsignselect", "petSignChange" );
    html.append( "</td></tr>" );

    page.insert( index, html.to_string() );

    // </table></center></td></tr></table></center></td></tr><tr><td height=4></td></tr></table>
    matcher m = create_matcher( pet_pattern, page );

    // Save insertion points in inverse order, since modifying the
    // buffer while it is being searched doesn't work
    record insertion
    {
	int index;
	string text;
    } [int] imap;

    int count = 1;

    while ( m.find() ){
	item it = desc_to_item( m.group(1) );
	if ( it == $item[ none ] ) {
	    continue;
	}

	html.set_length( 0 );

	void append_div( int cnum, int snum, string mods, boolean hidden )
	{
	    html.append( "<div " );
	    if ( hidden ) {
		html.append( "style=\"display:none\" " );
	    }
	    html.append( "id='petclass" );
	    html.append( to_string( cnum ) );
	    html.append( "sign" );
	    html.append( to_string( snum ) );
	    html.append( "item" );
	    html.append( it.to_int() );
	    html.append( "'>" );
	    html.append( mods );
	    html.append( "</div>" );
	}

	html.append( "<td><small>" );
	append_div( 0, 0, pet_modifiers[ "none", "none", it ], false );
	foreach c, cnum in classes {
	    foreach s, snum in signs {
		append_div( cnum, snum, pet_modifiers[ c, s, it ], true );
	    }
	}
	html.append( "</small></td>" );

	imap[ 0 - count++ ] = new insertion( m.end( 2 ), html.to_string() );
    }

    // Do the insertions
    foreach i, ins in imap {
	page.insert( ins.index, ins.text );
    }
}

void modify_deli( buffer page )
{
    int head = page.index_of( "</head>" );
    if ( head < 0 ) {
	return;
    }

    buffer html;

    // Insert Javascript variables and functions we need.
    html.set_length( 0 );

    html.append_line( "<script language=\"javascript\">" );

    html.append_line( "var deliClass = 0;" );
    html.append_line( "var deliSign = 0;" );

    html.append_line( "function deliDivName( c, s, it ) {" );
    html.append_line( "    return 'deliclass' + c + 'sign' + s + 'item' + it;" );
    html.append_line( "}" );
    
    html.append_line( "function deliClassSignHideDivs() {" );
    html.append_line( "    hidediv( deliDivName( '0', '0', '5043' ) );" );
    html.append_line( "    hidediv( deliDivName( '0', '0', '5044' ) );" );
    html.append_line( "    hidediv( deliDivName( '0', '0', '2743' ) );" );
    html.append_line( "    for ( var c=1; c < 7; ++c ) {" );
    html.append_line( "        for ( var s = 1; s < 10; ++s ) {" );
    html.append_line( "            hidediv( deliDivName( c, s, '5043' ) );" );
    html.append_line( "            hidediv( deliDivName( c, s, '5044' ) );" );
    html.append_line( "            hidediv( deliDivName( c, s, '2743' ) );" );
    html.append_line( "        }" );
    html.append_line( "    }" );
    html.append_line( "}" );

    html.append_line( "function deliClassSignShowDivs( c, s ) {" );
    html.append_line( "    showdiv( deliDivName( c, s, '5043' ) );" );
    html.append_line( "    showdiv( deliDivName( c, s, '5044' ) );" );
    html.append_line( "    showdiv( deliDivName( c, s, '2743' ) );" );
    html.append_line( "}" );

    html.append_line( "function deliClassSignChange() {" );
    html.append_line( "    if ( deliClass == 0 || deliSign == 0 ) {" );
    html.append_line( "        deliClassSignHideDivs();" );
    html.append_line( "        deliClassSignShowDivs(0, 0);" );
    html.append_line( "    } else {" );
    html.append_line( "        deliClassSignHideDivs();" );
    html.append_line( "        deliClassSignShowDivs(deliClass, deliSign);" );
    html.append_line( "    }" );
    html.append_line( "}" );

    html.append_line( "function deliClassChange() {" );
    html.append_line( "    deliClass = document.getElementById( 'deliclassselect' ).value;" );
    html.append_line( "    deliClassSignChange();" );
    html.append_line( "}" );

    html.append_line( "function deliSignChange() {" );
    html.append_line( "    deliSign = document.getElementById( 'delisignselect' ).value;" );
    html.append_line( "    deliClassSignChange();" );
    html.append_line( "}" );

    html.append( "</script>" );

    page.insert( head, html.to_string() );

    // </table></center></td></tr></table></center></td></tr><tr><td height=4></td></tr></table>
    int index = page.index_of( "/table></center></td></tr></table>" );
    if ( index < 0 ) {
	return;
    }

    index = page.index_of( "</table>", index );

    html.set_length( 0 );

    html.append( "<tr><td>" );

    // Display a helpful description of what will be displayed
    html.append_line( "<p>" );
    html.append_line( "Astral consumables generate adventures and stats based on your level when you consume them." );
    html.append_line( "In TCRS, the size may be different, but the generated adventures and stats will be the same." );
    html.append_line( "Additionally, the consumable may grant you turns of an effect." );
    html.append_line( "<p>" );
    html.append_line( "The steel margarita increases your maximum inebriety by 5 and grants no adventures or stats." );
    html.append_line( "In TCRS, the size may be different, but both adventures and stats are generated based on size." );
    html.append_line( "Additionally, it may grant you turns of an effect." );

    // Create the table of items which will be modified
    void append_consumable_divs( item it )
    {
	void append_div( int cnum, int snum, string mods, boolean hidden )
	{
	    html.append( "<div " );
	    if ( hidden ) {
		html.append( "style=\"display:none\" " );
	    }
	    html.append( "id='deliclass" );
	    html.append( to_string( cnum ) );
	    html.append( "sign" );
	    html.append( to_string( snum ) );
	    html.append( "item" );
	    html.append( it.to_int() );
	    html.append( "'>" );

	    string [int] pieces = mods.split_string( "/" );
	    html.append( "size = " );
	    html.append( pieces[0] );
	    if ( count( pieces ) > 1 && pieces[1] != "" ) {
		matcher m = create_matcher( "Effect: \"(.*?)\", Effect Duration: (\\d+)", pieces[ 1 ] );
		if ( m.find() ) {
		    string effect_name = m.group( 1 );
		    string effect_duration = m.group( 2 );
		    html.append( " effect = " );
		    html.append( effect_duration );
		    html.append( " turns of " );
		    html.append( effect_name );
		    html.append( "<br>(" );
		    html.append( string_modifier( to_effect( effect_name ), "Modifiers" ) ); 
		    html.append( " )" );
		}
	    }
	    html.append( "</div>" );
	}

	html.append( "<tr>" );

	html.append( "<td>" );
	html.append( it.to_string() );
	html.append( "</td>" );

	html.append( "<td><small>" );
	append_div( 0, 0, consumable_modifiers[ "none", "none", it ], false );
	foreach c, cnum in classes {
	    foreach s, snum in signs {
		append_div( cnum, snum, consumable_modifiers[ c, s, it ], true );
	    }
	}
	html.append( "</small></td>" );

	html.append( "</tr>" );
    }

    html.append( "<p>" );
    html.append_line( "<table>" );
    foreach it in consumables {
	append_consumable_divs( it );
    }
    html.append_line( "</table>" );

    // Insert the class and sign dropdowns
    html.append( "<p>" );
    html.insert_class_sign_dropdowns( "deliclassselect", "deliClassChange", "delisignselect", "deliSignChange" );

    html.append( "</td></tr>" );

    page.insert( index, html.to_string() );
}


void modify_decision( buffer page )
{
  buffer permery = visit_url("afterlife.php?place=permery");
  if ( permery.contains_text( "It looks like you've already got all of the skills from your last life marked permanent.  There's nothing we can do for you here!" ) )
  {
    string no_perm_warning = `<p style="color:red">Are you sure you want to reincarnate without marking any skills permanent?<br /><label><input type="checkbox" class="req" value="1" name="noskillsok" /> yes</label></p>`;
    string no_perm_ok = `<p style="color:orange">You are reincarnating without marking any skills permanent because you have no skills to make permanent.  Try learning more skills.  Knowledge is power.</p><input type="hidden" value="1" name="noskillsok" />`;
    replace_string(page, no_perm_warning, no_perm_ok );
    }
}

void main()
{
	buffer page = visit_url();
	if ( page.contains_text( "The Astral Pet Salesman" ) ) {
	    modify_pet_store( page );
	} else if ( page.contains_text( "The Deli Lama Counterman" ) ) {
	    modify_deli( page );
	} else if ( page.contains_text("Are you sure you want to reincarnate without marking any skills permanent?" ) ) {
	    modify_decision( page );
	}
	write( page );
}
