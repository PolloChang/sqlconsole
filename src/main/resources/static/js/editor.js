import {EditorView, basicSetup} from "codemirror"
import {EditorState, Prec, Compartment} from "@codemirror/state"
import {keymap} from "@codemirror/view"
import {sql} from "@codemirror/lang-sql"
import {defaultKeymap} from "@codemirror/commands"
import {acceptCompletion, completionStatus} from "@codemirror/autocomplete"

// Language Compartment for dynamic schema updates
const languageConf = new Compartment;

// Initial Schema (empty)
const initialSchema = {};

// Smart SQL Extraction
function getSmartSql(view) {
    const state = view.state;
    const selection = state.selection.main;

    if (!selection.empty) {
        return state.sliceDoc(selection.from, selection.to);
    }

    const doc = state.doc.toString();
    const cursor = selection.head;

    // Search backwards for ;
    let start = doc.lastIndexOf(';', cursor - 1);
    if (start === -1) {
        start = 0;
    } else {
        start += 1; // Skip the semicolon itself
    }

    // Search forwards for ;
    let end = doc.indexOf(';', cursor);
    if (end === -1) {
        end = doc.length;
    }

    return doc.substring(start, end).trim();
}

// Global hook for doSql to access
window.getSmartSql = () => getSmartSql(window.editorView);

// Global hook to update schema
window.updateEditorSchema = (schemaData) => {
    // schemaData is now expected to be { "TableName": ["Col1", "Col2"], ... }
    // The CM6 sql extension accepts exactly this format.
    const newSchema = schemaData || {};

    window.editorView.dispatch({
        effects: languageConf.reconfigure(sql({schema: newSchema, upperCaseKeywords: true}))
    });
    console.log("Editor schema updated with metadata:", newSchema);
};

// Command Handlers
const runQuery = (view) => {
    window.doSql('EXEC');
    return true;
};

const commitTx = (view) => {
    window.doSql('COMMIT');
    return true;
};

const rollbackTx = (view) => {
    window.doSql('ROLLBACK');
    return true;
};

const handleEnter = (view) => {
    // If autocomplete is open, Enter picks the suggestion
    if (completionStatus(view.state) === "active") {
        return acceptCompletion(view);
    }
    // Otherwise, default Enter behavior (insert newline)
    return false;
};

const myKeymap = [
    {key: "Ctrl-Enter", run: runQuery},
    {key: "Enter", run: handleEnter},
    {key: "Ctrl-[", run: commitTx},
    {key: "Ctrl-]", run: rollbackTx}
];

window.editorView = new EditorView({
    doc: "-- Select a DB and start typing...",
    extensions: [
        Prec.highest(keymap.of(myKeymap)),
        basicSetup,
        languageConf.of(sql({schema: initialSchema, upperCaseKeywords: true}))
    ],
    parent: document.getElementById("editor")
});

window.startExport = function() {
    let sql = window.getSmartSql();
    let dbId = $("#dbId").val();

    $("#btn-export").prop("disabled", true);
    $("#export-status").text("Submitting...").css("color", "black");

    $.ajax({
        url: "/api/export/execute",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify({ dbId: parseInt(dbId), sql: sql }),
        success: function(jobId) {
            $("#export-status").text("Export Started...").css("color", "blue");
            pollExportStatus(jobId);
        },
        error: function(xhr) {
            $("#btn-export").prop("disabled", false);
            $("#export-status").text("Failed to start").css("color", "red");
            alert("Export failed: " + xhr.responseText);
        }
    });
};

function pollExportStatus(jobId) {
    let interval = setInterval(function() {
        $.get("/api/export/status/" + jobId, function(status) {
            if (status.status === 'COMPLETED') {
                clearInterval(interval);
                $("#export-status").text("Completed! Downloading...").css("color", "green");
                $("#btn-export").prop("disabled", false);
                window.location.href = "/api/export/download/" + jobId;
                setTimeout(() => $("#export-status").text(""), 5000);
            } else if (status.status === 'FAILED') {
                clearInterval(interval);
                $("#export-status").text("Failed: " + status.errorMessage).css("color", "red");
                $("#btn-export").prop("disabled", false);
            } else {
                $("#export-status").text("Exporting... " + status.percentage + "%");
            }
        });
    }, 1000);
}
