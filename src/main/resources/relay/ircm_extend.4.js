(function(window) {
	var original_function = window.pop_ircm_contents;

	window.pop_ircm_contents = function(i, some) {
		var original = original_function(i, some);
		var name = $("[rel^='id=" + i.id + "&'] b").eq(0).text();

		if (name) {
			var encodedName = encodeURIComponent(name);
			var wikiUrl = "https://wiki.kingdomofloathing.com/Special:Search?search=" + encodedName;

			var html = '<div style="width:100%; padding-bottom: 3px;"><b>Search:</b> ';
			// mallable == !quest && !gift && tradeable
			if (i.q == 0 && i.g == 0 && i.t == 1) {
				html += '<a href="/mall.php?pudnuggler=' + encodedName + '" class="small">[mall]</a>&nbsp;';
			}
			html += '<a target="_blank" href="' + wikiUrl + '" class="small">[wiki]</a></div>';

			original[0] += html;
			original[1]++;
		}

		return original;
	}
})(this);
