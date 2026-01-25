import {EditorView, basicSetup} from "codemirror"
import {EditorState, Prec} from "@codemirror/state"
import {keymap} from "@codemirror/view"
import {sql} from "@codemirror/lang-sql"
import {defaultKeymap} from "@codemirror/commands"
import {acceptCompletion, completionStatus} from "@codemirror/autocomplete"

// Context-Aware Schema with Table Names
const schema = {
    "sys_users": ["id", "username", "password", "role"],
    "db_configs": ["id", "name", "jdbc_url", "db_user", "db_password"],
    "sql_history": ["id", "executor", "db_name", "sql_content", "status", "created_at"],
    "USER_TABLES": [], // Generic suggestion
};

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
    // Otherwise, Enter triggers EXEC (as requested)
    // Note: To insert a newline, user might need Shift-Enter if we enforce this strictly.
    // However, the requirement is "Triggered by Enter when no suggestion menu is visible".
    window.doSql('EXEC');
    return true;
};

const myKeymap = [
    {key: "Ctrl-Enter", run: runQuery},
    {key: "Ctrl-[", run: commitTx},
    {key: "Ctrl-]", run: rollbackTx}
    // 這裡不要寫 Enter，讓 basicSetup 處理正常的 Enter 換行與補全選取
];

window.editorView = new EditorView({
    doc: "-- Enter SQL here...",
    extensions: [
        Prec.highest(keymap.of(myKeymap)), // Force highest priority
        basicSetup,
        sql({schema: schema, upperCaseKeywords: true})
    ],
    parent: document.getElementById("editor")
});
