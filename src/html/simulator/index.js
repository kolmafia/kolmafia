String.prototype.trim = function()
{	return this.replace( /^\s+/, '' ).replace( /\s+$/, '' );
};

function selectOption( combobox, value )
{
	value = value.toLowerCase().trim();
	for ( i = 0; ( i < combobox.options.length ) && ( combobox.options[i].value.toLowerCase().trim() != value ); ++i );
	if ( i < combobox.options.length )
		combobox.selectedIndex = i;
};

function loadKoLmafiaData()
{
	// Load basic character information
	
	document.character.charclass.selectedIndex = /*classIndex*/;
	document.character.basemuscle.value = /*baseMuscle*/;
	document.character.basemysticality.value = /*baseMysticality*/;
	document.character.basemoxie.value = /*baseMoxie*/;
	document.character.mcd.selectedIndex = /*mindControl*/;
	document.character.weight.value = /*familiarWeight*/;

	// Effects & Passive Skills

	var passiveSkills = "/*passiveSkills*/";
	var activeEffects = "/*activeEffects*/";
	for ( j = 0; j < document.forms.length; ++j )
		for ( i = 0; i < document.forms[j].elements.length; ++i )
			if ( document.forms[j].elements[i].type == "checkbox" )
				if ( ( passiveSkills.indexOf( "\t" + document.forms[j].elements[i].name + "\t" ) != -1 ) ||
					( activeEffects.indexOf( "\t" + document.forms[j].elements[i].name + "\t" ) != -1 ) )
				{
					document.forms[j].elements[i].checked = true;
					document.forms[j].style.display = "";
				}

	AdjustForDualWield();
	
	// Snowcones
	
	var snowcone = 0;
	if ( activeEffects.indexOf( "\tblacktongue\t" ) != -1 )
		snowcone = 1;
	if ( activeEffects.indexOf( "\tbluetongue\t" ) != -1 )
		snowcone = 2;
	if ( activeEffects.indexOf( "\tredtongue\t" ) != -1 )
		snowcone = 3;
	if ( activeEffects.indexOf( "\torangetongue\t" ) != -1 )
		snowcone = 4;
	if ( activeEffects.indexOf( "\tgreentongue\t" ) != -1 )
		snowcone = 5;
	if ( activeEffects.indexOf( "\tpurpletongue\t" ) != -1 )
		snowcone = 6;
	if ( snowcone != 0 )
	{
		document.snowcones.style.display = "";
		document.snowcones[snowcone].checked = true;
	}

	// Familiar and Familiar Equipment

	if ( "/*familiar*/" == "Cymbal-Playing Monkey" )
		selectOption( document.character.familiar, "Cheshire Bitten" );
	else if ( "/*familiar*/" == "Cheshire Bat" )
		selectOption( document.character.familiar, "Cheshire Bitten" );
	else
		selectOption( document.character.familiar, "/*familiar*/" );
	selectOption( document.equipment.familiarequip, "/*familiarEquip*/" );

	// All other equipment

	selectOption( document.equipment.hat, "/*hat*/" );
	selectOption( document.equipment.weapon, "/*weapon*/" );
	selectOption( document.equipment.offhand, "/*offhand*/" );
	selectOption( document.equipment.shirt, "/*shirt*/" );
	selectOption( document.equipment.pants, "/*pants*/" );

	selectOption( document.equipment.acc1, "/*accessory1*/" );
	selectOption( document.equipment.acc2, "/*accessory2*/" );
	selectOption( document.equipment.acc3, "/*accessory3*/" );

	document.miscinput.rockandroll.checked = /*rockAndRoll*/;
	
	// Moon phase
	
	moonphase = /*moonPhase*/;
	document.images[0].src=moonimage[0][moonphase].src;
	document.images[1].src=moonimage[1][moonphase].src;

};
