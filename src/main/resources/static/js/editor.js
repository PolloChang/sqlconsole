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
window.updateEditorSchema = (tableList) => {
    const newSchema = {};
    if (tableList && Array.isArray(tableList)) {
        tableList.forEach(t => {
            newSchema[t] = []; // We could fetch columns later if needed
        });
    }

    window.editorView.dispatch({
        effects: languageConf.reconfigure(sql({schema: newSchema, upperCaseKeywords: true}))
    });
    console.log("Editor schema updated with tables:", tableList);
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
