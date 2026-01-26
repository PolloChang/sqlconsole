$(document).ready(function() {
    loadUsers();
});

let currentUsers = [];

function loadUsers() {
    $.get("/api/users", function(data) {
        currentUsers = data;
        let tbody = $("#usersTable tbody");
        tbody.empty();
        data.forEach(user => {
            let tr = $("<tr>");
            tr.append($("<td>").text(user.id));
            tr.append($("<td>").text(user.username));
            tr.append($("<td>").text(user.role));

            let actionsTd = $("<td>");
            let editBtn = $("<button>").addClass("btn-blue").text("Edit").click(function() { editUser(user.id); });
            let deleteBtn = $("<button>").addClass("btn-red").text("Delete").css("margin-left", "5px").click(function() { deleteUser(user.id); });

            actionsTd.append(editBtn).append(deleteBtn);
            tr.append(actionsTd);
            tbody.append(tr);
        });
    });
}

function openModal(id) {
    if (id) {
        $("#modalTitle").text("Edit User");
        let user = currentUsers.find(u => u.id === id);
        if (user) {
            $("#userId").val(user.id);
            $("#username").val(user.username);
            $("#role").val(user.role);
            $("#password").val("");

            // Uncheck all first
            $("input[name='dbPermission']").prop('checked', false);

            // Check accessible DBs
            if (user.accessibleDatabases) {
                user.accessibleDatabases.forEach(db => {
                    $(`input[name='dbPermission'][value='${db.id}']`).prop('checked', true);
                });
            }
        }
    } else {
        $("#modalTitle").text("Add User");
        $("#userForm")[0].reset();
        $("#userId").val("");
        $("input[name='dbPermission']").prop('checked', false);
    }
    $("#userModal").show();
}

function closeModal() {
    $("#userModal").hide();
}

function editUser(id) {
    openModal(id);
}

function deleteUser(id) {
    if (confirm("Are you sure?")) {
        $.ajax({
            url: '/api/users/' + id,
            type: 'DELETE',
            success: function(result) {
                loadUsers();
            },
            error: function(err) {
                alert("Error deleting: " + (err.responseJSON ? err.responseJSON.message : err.statusText));
            }
        });
    }
}

function saveUser() {
    let id = $("#userId").val();
    let payload = {
        username: $("#username").val(),
        role: $("#role").val(),
        password: $("#password").val() || null // Send null if empty so backend ignores it (or handled there)
    };

    let checkedDbIds = [];
    $("input[name='dbPermission']:checked").each(function() {
        checkedDbIds.push(parseInt($(this).val()));
    });

    let method = id ? 'PUT' : 'POST';
    let url = id ? '/api/users/' + id : '/api/users';

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function(savedUser) {
            // After saving user, assign databases
            assignDatabases(savedUser.id, checkedDbIds);
        },
        error: function(err) {
            alert("Error saving: " + (err.responseJSON ? err.responseJSON.message : err.statusText));
        }
    });
}

function assignDatabases(userId, dbIds) {
    $.ajax({
        url: '/api/users/' + userId + '/databases',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(dbIds),
        success: function(res) {
            closeModal();
            loadUsers();
            // Optional: Show toast
        },
        error: function(err) {
            alert("Error assigning databases: " + (err.responseJSON ? err.responseJSON.message : err.statusText));
        }
    });
}

// Close modal when clicking outside
window.onclick = function(event) {
    if (event.target == document.getElementById("userModal")) {
        closeModal();
    }
}
