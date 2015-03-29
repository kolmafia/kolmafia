(function(window) {
	var WIKI_URL = "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?go=Go&search=";

	var SEARCH_HTML = "<div style='width:100%; padding-bottom: 3px;'><b>Search:</b> ";
	var MALL_HTML = "<a href='/mall.php?pudnuggler=%NAME' class='small'>[mall]</a>&nbsp;";
	var WIKI_HTML = "<a target='_blank' href='" + WIKI_URL + "%NAME' class='small'>[wiki]</a></div>";

	var original_function = window.pop_ircm_contents;

	window.pop_ircm_contents = function(i, some) {
		var original = original_function(i, some);
		var name = $("[rel^='id=" + i.id + "&'] b").eq(0).text();

		if (name) {
			var html = SEARCH_HTML;

			// mallable == !quest && !gift && tradeable
			if (i.q == 0 && i.g == 0 && i.t == 1) {
				html += MALL_HTML;
			}

			html += WIKI_HTML;

			return [original[0] + html.replace(/%NAME/g, encodeURIComponent(name)), original[1] + 1];
		}

		return original;
	}
})(this);
