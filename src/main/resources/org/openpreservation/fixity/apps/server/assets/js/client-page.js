/**
* Document ready function, loaded up on start
*/
$(document).ready(function () {
})

/**
* Sorter for the name anchor columns
*/
function anchorNamesSorter(a, b) {
	let aa = a.match(/<a [^>]+>([^<]+)<\/a>/)[1].toLowerCase();
	let bb = b.match(/<a [^>]+>([^<]+)<\/a>/)[1].toLowerCase();
	if (aa == bb) {
		return 0;
	}
	if (aa > bb) {
		return 1;
	}
	return -1;
}
