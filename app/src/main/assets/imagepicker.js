/* This file is injected with a javascript: URL.  Single-line comments will not work. */

var toDataURL = function (img) {
    var canvas = document.getElementById('canvas');

    canvas.width = img.width;
    canvas.height = img.height;

    canvas.getContext('2d').drawImage(img, 0, 0);

    return canvas.toDataURL();
};

var updateSelected = function () {
    var selected = [];
    var els = document.getElementsByTagName('img');

    for (i = 0; i < els.length; i++) {
        if (els[i].className == 'selected') {
            var src = els[i].src;

            if (src.substr(0, 5) != 'data:') {
                src = toDataURL(els[i]);
            }

            selected.push(src);
        }
    }

    wcm.pushSelected(JSON.stringify(selected));
};

var toggleSelected = function (img) {
    img.className = img.className == 'selected' ? '' : 'selected';
    updateSelected();
};

var getPickerHtml = function() {
    var html = '<style>' +
        'img { padding: 0.1em; margin: 1em; border: 0.5em solid white; }' +
        'img.selected { border: 0.5em solid blue; box-shadow: 0px 12px 22px 1px #333; }' +
        'canvas { display: none; }' +
        '</style>' +
        '<canvas id="canvas"></canvas>';
    var els = document.getElementsByTagName('img');
    var onclick = "toggleSelected(this);";

    for (i = 0; i < els.length; i++) {
        var src = '';

        if (i == 0) {
            continue;
        }

        src = els[i].src;
        if (src == '') {
            src = els[i].dataset.src;
        }

        if (!src) {
            continue;
        }

        /*
         * Change images pointing to the various encrypted-tbn[0-9].gstatic.com
         * servers to point to the local one encrypted-tbn0.gstatic.com, to get
         * around the canvas cross-origin restrictions in toDataURL.
         */
        src = src.replace(/^http.*\.com/, '');

        if (src && (src.substr(0, 5) == 'data:' || src.indexOf('tbn:') != -1)) {
            html += '<img src="' + src + '" onclick="' + onclick + '">';
        }
    }

    wcm.pushPickerHtml(html);
};
