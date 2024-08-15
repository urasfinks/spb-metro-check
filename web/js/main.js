window.$$ = function (id) {
    return document.getElementById(id);
}

window.selectedItem = {};

window.onChangeCheckBox = function (obj) {
    window.selectedItem[obj.id] = obj.checked;
    console.log(window.selectedItem);
}

window.selectOnChange = function (obj) {
    var act = obj.options[obj.selectedIndex].value;
    console.log(act);
    document.getElementById("form-upload").action = act;
}

var dropContainer = document.getElementById("dropcontainer");
var fileInput = document.getElementById("upload");

dropContainer.addEventListener("dragover", (e) => {
    // prevent default to allow drop
    e.preventDefault();
}, false);

dropContainer.addEventListener("dragenter", () => {
    dropContainer.classList.add("drag-active");
});

dropContainer.addEventListener("dragleave", () => {
    dropContainer.classList.remove("drag-active");
});

dropContainer.addEventListener("drop", (e) => {
    e.preventDefault();
    dropContainer.classList.remove("drag-active");
    fileInput.files = e.dataTransfer.files;
});

function ajax(url, callback) {
    $$("error").innerHTML = "";
    var xmlhttp = new XMLHttpRequest();

    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == XMLHttpRequest.DONE) { // XMLHttpRequest.DONE == 4
            if (xmlhttp.status == 200) {
                var data = JSON.parse(xmlhttp.responseText);
                callback(data);
            } else if (xmlhttp.status == 0) {
                $$("error").innerHTML = 'Server error';
            }
        }
    };

    xmlhttp.open("GET", url, true);
    xmlhttp.send();
}

setInterval(function () {
    load();
}, 5000);

function load() {
    ajax("/StatisticDb", function (data) {
        if (data.exception == true) {
            alert(JSON.stringify(data));
            return;
        }
        var ar = [
            "tpp-",
            "orange-",
            "tpp-accepted_tpp",
            "tpp-not_orange",
            "tpp-fn_future",
            "tpp-cancel",
            "tpp-checked",
            "orange-not_tpp",
            "orange-checked"
        ];
        for (var i = 0; i < ar.length; i++) {
            document.getElementById(ar[i]).innerHTML = "0";
        }
        for (var key in data) {
            if (key === "orange-2") {
                continue;
            }
            for (var i = 0; i < data[key].length; i++) {
                var id = key + "-" + (data[key][i].title == null ? "" : data[key][i].title);
                document.getElementById(id).innerHTML = data[key][i].count;
            }
        }
        if (data["orange-2"] != undefined) {
            var str = "";
            for (var i = 0; i < data[key].length; i++) {
                str += "<div>" + data[key][i].title + ": " + data[key][i].count + "</div>";
            }
            $$("orange_group").innerHTML = str;
        }
    });
}

load();

function load_kkt(obj) {
    obj.classList.toggle('button--loading');
    ajax("/StatisticKkt", function (data) {
        obj.classList.toggle('button--loading');
        if (data.exception == true) {
            alert(JSON.stringify(data));
            return;
        }
        var ar = [
            "kkt-count",
            "kkt-orange",
            "kkt-diff"
        ];
        for (var i = 0; i < ar.length; i++) {
            $$(ar[i]).innerHTML = "0";
        }
        for (var key in data) {
            for (var i = 0; i < data[key].length; i++) {
                var id = key + "-" + (data[key][i].title == null ? "" : data[key][i].title);
                document.getElementById(id).innerHTML = data[key][i].count;
            }
        }
    });
}

load_kkt($$("load_kkt"));

window.do = function (url, obj) {
    obj.classList.toggle('button--loading');
    ajax(url, function (data) {
        obj.classList.toggle('button--loading');
        if (data.exception == true) {
            alert(JSON.stringify(data));
        } else {
            alert("Ok");
        }
    });
}

window.blank_total = function () {
    var total_date = document.getElementById('total_date').value;
    if (total_date == undefined || total_date.trim() == "") {
        alert("Не задана дата");
        return;
    }
    window.blank('/StatisticTotal?docDate=' + total_date);
}

window.blank_correction = function () {
    var refund_date = document.getElementById('refund_date').value;
    if (refund_date == undefined || refund_date.trim() == "") {
        alert("Не задана дата");
        return;
    }
    var doc_number = document.getElementById('doc_number').value;
    if (doc_number == undefined || doc_number.trim() == "") {
        alert("Не задан номер документа");
        return;
    }
    window.blank('/CsvCorrection?docNum=' + doc_number + '&docDate=' + refund_date);
}

window.blank = function (url) {
    var anchor = document.createElement('a');
    anchor.href = url;
    anchor.target = "_blank";
    anchor.click();
}