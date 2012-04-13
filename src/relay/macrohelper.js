var input = document.getElementsByName("macrotext")[0];
input.style.width = "48%";
input.setAttribute("onchange", "defercheck()");
input.setAttribute("onkeyup", "defercheck()");
document.write('&nbsp;<textarea id=syntax cols=50 style="width: 48%;" readonly rows=', input.rows, '></textarea>');
var output = document.getElementById("syntax");
var timeout = false;
function defercheck()
{
	if (timeout) {
		clearTimeout(timeout);
	}
	timeout = setTimeout("syntaxcheck()", 2000);
}
defercheck();

function trim(str)
{
	return str.replace(/^\s+|\s+$/g, "");
}

var errCmds = {
// "<key>" is not a known command; did you mean "<value>"?
	"item": "use",
	"summon": "summonspirit",
	"steal": "pickpocket",
	"run": "runaway",
};

var arglessCmds = {
// Cmds which never take an argument.
// Cmds in closeCmds also never take an argument.
	"attack": true,
	"pickpocket": true,
	"summonspirit": true,
	"runaway": true,
	"jiggle": true,
	"scrollwhendone": true,
};

var optargCmds = {
// Cmds which are valid with no argument.
// They must appear elsewhere to validate an argument if present.
	"abort": true,
	"repeat": true,
};

var argCmds = {
// Cmds which can be followed by an arbitrary argument (not further parsed)
	"abort": true,
	"skill": true,
	"use": true,
	"icon": true,
};

var labelCmds = {
// Cmds which can be followed by a single word
	"mark": true,
	"sub": true,
	"call": true,
};

var predCmds = {
// Cmds which can be followed by a predicate expression
	"abort": true,
	"repeat": true,
	"while": true,
	"if": true,
};

var openCmds = {
// Cmds which open a block -> corresponding closing cmd
// Must always be listed somewhere above for argument validation
	"while": "endwhile",
	"if": "endif",
	"sub": "endsub",
};

var closeCmds = {
// Cmds which close a block - lack of argument is assumed
	"endwhile": true,
	"endif": true,
	"endsub": true,
};

var arglessPreds = {
// Predicates which take no argument
	"gotjump": true,
};

var numPreds = {
// Predicates which take a numeric argument
	"times": true,
	"hpbelow": true,
	"hppercentbelow": true,
	"mpbelow": true,
	"mppercentbelow": true,
	"didcritical": true,
	"beenhit": true,
	"missed": true,
	"familiarattacked": true,
	"pastround": true,
};

var stringPreds = {
// Predicates which take a string argument
	"match": true,
};

var argPreds = {
// Predicates which take an arbitrary argument (not further parsed)
	"haseffect": true,
	"hascombatitem": true,
	"hasskill": true,
	"monstername": true,
	"happymediumglow": true,
};

function predicate(text, err)
{
	var tokens = text.match(/\s*(!|&&|\|\||\(|\)|([^!()&|]|&(?!&))+)/g);
	var nesting = 0;
	var wantpred = true;
	while (tokens.length > 0) {
		var token = trim(tokens.shift());
		if ((wantpred && (token == '&&' || token == '||' || token == ')')) ||
			(!wantpred && (token == '!' || token == '('))) {
			output.value += err + '- unexpected "' + token + '" in predicate.\n';
			return;
		}
		if (token == '!') continue;
		if (token == '&&' || token =='||') {
			wantpred = true;
			continue;
		}
		if (token == '(') {
			++nesting;
			continue;
		}
		if (token == ')') {
			if (--nesting < 0) {
				output.value += err + '- too many ")" in predicate.\n';
				return;
			}
			continue;
		}
		
		var params = '';
		var pos = token.indexOf(' ');
		if (pos != -1) {
			params = trim(token.slice(pos));
			token = token.slice(0, pos);
		}
		var perr = err + '- "' + token + '" ';
		if (!wantpred) {
			output.value += perr + ' is not valid at this point.\n';
			return;
		}
		wantpred = false;
		if (arglessPreds[token]) {
			if (params != '') {
				output.value += perr + 'takes no parameter.\n';
			}
			continue;
		}
		if (argPreds[token]) {
			if (params == '') {
				output.value += perr + 'requires a parameter.\n';
			}
			continue;
		}
		if (numPreds[token]) {
			if (!(/^\d+/.test(params))) {
				output.value += perr + 'requires a numeric parameter.\n';
			}
			continue;
		}
		if (stringPreds[token]) {
			if (params == '') {
				output.value += perr + 'requires a parameter.\n';
			}
			else if (params.indexOf(' ') != -1) {
				output.value += perr + 'requires double-quotes around ' +
					"parameters containing spaces.\n";
			}
			continue;
		}
	
		output.value += perr + 'is not a known predicate.\n';
	}
	if (wantpred) {
		output.value += err + "- unexpected end of predicate.\n";
	}
	else if (nesting > 0) {
		output.value += err + "- unclosed parentheses in predicate.\n";
	}
}

function syntaxcheck()
{
	timeout = false;
	output.value = "";
	var ln = 1;
	var stack = [];
	var lines = input.value.match(/\x0D\x0A|\x0D|\x0A|;|#?[^\x0D\x0A;#]*/g);
	for (ix in lines) {
		var cmd = lines[ix];
		var c0 = cmd.charAt(0);
		cmd = trim(cmd);
		if (c0 == '\x0D' || c0 == '\x0A') {
			++ln;
			continue;
		}
		if (c0 == ';' || c0 == '#' || cmd =='') {
			continue;
		}
	
		var params = '', param1 = '', paramrest = '';
		var pos = cmd.indexOf(' ');
		if (pos != -1) {
			param1 = params = trim(cmd.slice(pos)).replace(/".*?"/g, '"STRING"');
			cmd = cmd.slice(0, pos)
			pos = params.indexOf(' ');
			if (pos != -1) {
				paramrest = trim(params.slice(pos));
				param1 = params.slice(0, pos);
			}
		}
		var err = "Line " + ln + ': "' + cmd + '" ';
		
		if (openCmds[cmd]) {
			stack.push(openCmds[cmd]);
		}
		else if (closeCmds[cmd]) {
			if (stack.length == 0) {
				output.value += err + "without corresponding block opener.\n";
				continue;
			}
			while (stack.length > 0) {
				var block = stack.pop();
				if (block == cmd) break;
				output.value += "Line " + ln + ': an "' + block +
					'" is missing.\n';
			}
		}
		
		if (errCmds[cmd]) {
			output.value += err + 'is not a known command; did you mean "' +
				errCmds[cmd] + '"?\n';
			continue;
		}
		if (arglessCmds[cmd] || closeCmds[cmd]) {
			if (params != '') {
				output.value += err + 'should not be followed by anything.  ' +
					'Especially not "' + params + '"!\n';
			}
			continue;
		}
		if (optargCmds[cmd] && params == '') continue;
		if (argCmds[cmd]) {
			if (params == '') {
				output.value += err + 'requires a parameter.\n';
			}
			continue;
		}
		if (labelCmds[cmd]) {
			if (params == '') {
				output.value += err + 'requires a label name.\n';
			}
			else if (paramrest != '') {
				output.value += err + 'should be followed by a single word, ' +
					'not "' + params + '".\n';
			}
			continue;
		}
		if (predCmds[cmd]) {
			if (params == '') {
				output.value += err + 'requires a predicate.\n';
			}
			predicate(params, err);
			continue;
		}
		
		// Special cases:
		if (cmd == "goto") {
			if (params == '') {
				output.value += err + 'requires a label name.\n';
			}
			else if (paramrest != '') {
				predicate(paramrest, err);
			}
			continue;
		}
		
		output.value += err + "is not a known command.\n";
	}
	while (stack.length > 0) {
		output.value += 'At end: there is a missing "' + stack.pop() +
			'" somewhere.';
	}

	if (output.value == "") {
		output.value = "No errors found (KoLmafia syntax checker v1.4)";
	}
}
