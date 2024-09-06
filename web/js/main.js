window.$$ = function (id) {
    return document.getElementById(id);
}

window.getOnceDate = function (callback) {
    getDate(function (dateStart, dateEnd) {
        if (dateStart != dateEnd) {
            alert("Для этой операции надо, что бы дата начала была равна дате конца");
            return;
        }
        callback(dateStart);
    });
}

window.getDate = function (callback) {
    var dateStart = $$('all_date_start').value;
    if (dateStart == undefined || dateStart.trim() == "") {
        alert("Не задана дата начала");
        return;
    }
    var dateEnd = $$('all_date_end').value;
    if (dateEnd == undefined || dateEnd.trim() == "") {
        alert("Не задана дата конца");
        return;
    }
    callback(dateStart, dateEnd);
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

function load() {
    window.getDate(function (dateStart, dateEnd) {
        ajax("/StatisticDb?date_start=" + dateStart + "&" + "date_end=" + dateEnd, function (data) {
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
                if (key === "orange-agg") {
                    continue;
                }
                for (var i = 0; i < data[key].length; i++) {
                    var id = key + "-" + (data[key][i].title == null ? "" : data[key][i].title);
                    document.getElementById(id).innerHTML = data[key][i].count;
                }
            }
            if (data["orange-agg"] != undefined) {
                var str = "";
                for (var i = 0; i < data[key].length; i++) {
                    str += "<div>" + data[key][i].title + ": " + data[key][i].count + "</div>";
                }
                $$("orange_group").innerHTML = str;
            }
        });
    });
}

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

window.total_save = function (obj) {
    var total_date = document.getElementById('total_date').value;
    if (total_date == undefined || total_date.trim() == "") {
        alert("Не задана дата");
        return;
    }
    window.do('/SaveTotal?docDate=' + total_date, obj);
}

window.total_remove = function (obj) {
    var total_date = document.getElementById('total_date_remove').value;
    if (total_date == undefined || total_date.trim() == "") {
        alert("Не задана дата");
        return;
    }
    window.do('/RemoveTotal?docDate=' + total_date, obj);
}

window.total_get = function () {
    var total_date_start = document.getElementById('total_date_start').value;
    if (total_date_start == undefined || total_date_start.trim() == "") {
        alert("Не задана дата начала");
        return;
    }
    var total_date_end = document.getElementById('total_date_end').value;
    if (total_date_end == undefined || total_date_end.trim() == "") {
        alert("Не задана дата конца");
        return;
    }
    window.blank('/CsvTotal?docDateStart=' + total_date_start + '&docDateEnd=' + total_date_end);
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

onReady(function () {
    $$('all_date_start').value = new Date().toISOString().substring(0, 10);
    $$('all_date_end').value = new Date().toISOString().substring(0, 10);
    setInterval(function () {
        load();
    }, 5000);
    load_kkt($$("load_kkt"));
    load();
})