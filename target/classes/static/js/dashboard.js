/*
 * Dashboard JavaScript
 *
 * Handles UI interactions, API calls, and dynamic content.
 */

/* ========================================================
   1. GLOBAL VARIABLES & INITIALIZATION
   ======================================================== */

const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

// Will hold user info from /api/user/me
let currentUser = {
    username: "",
    roles: [],
    displayRole: "",
    isAdmin: false
};

// Initialize on load
document.addEventListener("DOMContentLoaded", function () {

    // Fetch user info first
    fetchUserInfo().then(() => {
        // Load initial dashboard stats
        loadDashboardStats();

        // If on the dashboard tab, load recent reports
        if (document.getElementById('dashboard').classList.contains('active')) {
            loadDashboardRecentReports();
        }
    });

});


/* ========================================================
   2. AUTH & USER INFO
   ======================================================== */

async function fetchUserInfo() {
    try {
        const response = await fetch('/api/user/me');
        if (response.ok) {
            const data = await response.json();
            currentUser = data;

            // Update UI
            document.getElementById('sidebarUsername').textContent = currentUser.username;
            document.getElementById('topbarUsername').textContent = currentUser.username;

            // Set avatar (first letter of username)
            const firstLetter = currentUser.username.charAt(0).toUpperCase();
            document.getElementById('userAvatar').textContent = firstLetter;

            // Update settings page
            const settingsUser = document.getElementById('settingsUsername');
            if (settingsUser) settingsUser.value = currentUser.username;
            const settingsRole = document.getElementById('settingsRole');
            if (settingsRole) settingsRole.value = currentUser.displayRole;

            // Set role badge
            const roleBadge = document.getElementById('sidebarUserRole');
            roleBadge.textContent = currentUser.displayRole;

            // Role specific styling
            roleBadge.className = 'user-role-badge'; // reset
            if (currentUser.displayRole === 'Admin') roleBadge.classList.add('role-admin');
            else if (currentUser.displayRole === 'Sales Team') roleBadge.classList.add('role-sales');
            else if (currentUser.displayRole === 'Research Team') roleBadge.classList.add('role-research');
            else if (currentUser.displayRole === 'Digital Marketing') roleBadge.classList.add('role-marketing');
            else roleBadge.classList.add('role-user');


            // Admin visibility
            const adminElements = document.querySelectorAll('.admin-only');
            if (currentUser.isAdmin) {
                adminElements.forEach(el => el.style.display = ''); // use default
            } else {
                adminElements.forEach(el => el.style.display = 'none');
            }

        } else {
            console.error("Failed to fetch user info. Redirecting to login.");
            window.location.href = '/login';
        }
    } catch (error) {
        console.error("Error fetching user info:", error);
    }
}

function logout() {
    // Spring Security POST /logout requires CSRF
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/logout';

    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;

    form.appendChild(csrfInput);
    document.body.appendChild(form);
    form.submit();
}


/* ========================================================
   3. NAVIGATION
   ======================================================== */

function showSection(sectionId, buttonElement) {

    // Update active button
    if (buttonElement) {
        document.querySelectorAll('.navigation-button')
            .forEach(btn => btn.classList.remove('active'));
        buttonElement.classList.add('active');
    } else {
        // Fallback if no button provided
        document.querySelectorAll('.navigation-button')
            .forEach(btn => {
                btn.classList.remove('active');
                if (btn.getAttribute('data-section') === sectionId) {
                    btn.classList.add('active');
                }
            });
    }

    // Hide all sections
    document.querySelectorAll('.content-section')
        .forEach(section => section.classList.remove('active'));

    // Show target section
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.add('active');
    }

    // Call specific init functions
    if (sectionId === 'edit-sample') {
        // Only load if we are looking at the list (not actively editing one)
        if (document.getElementById('reportsListContainer').style.display !== 'none') {
            loadMyReports();
        }
    } else if (sectionId === 'dashboard') {
        loadDashboardStats();
        loadDashboardRecentReports();
    } else if (sectionId === 'users') {
        loadUsers();
    }
}


/* ========================================================
   4. DASHBOARD STATS
   ======================================================== */

async function loadDashboardStats() {
    try {
        // Load stats from API (if admin get all, else get user's)
        const endpoint = currentUser.isAdmin ? '/api/reports/all' : '/api/reports/my-reports';
        const response = await fetch(endpoint);

        if (response.ok) {
            const reports = await response.json();
            document.getElementById('statMyReports').textContent = reports.length;

            // Just some fake data for other stats for now
            document.getElementById('statTotalSamples').textContent = reports.length + 150;
            document.getElementById('statUpdated').textContent = Math.floor(reports.length * 0.8);

        }
    } catch (e) {
        console.error("Error loading stats", e);
    }
}

async function loadDashboardRecentReports() {
    try {
        const endpoint = currentUser.isAdmin ? '/api/reports/all' : '/api/reports/my-reports';
        const response = await fetch(endpoint);

        const tbody = document.getElementById('dashboardRecentReports');
        tbody.innerHTML = '';

        if (response.ok) {
            const reports = await response.json();

            // Take top 5
            const recent = reports.slice(0, 5);

            if (recent.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" style="text-align: center;">No reports created yet.</td></tr>`;
                return;
            }

            recent.forEach(report => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${report.keyId}</td>
                    <td><strong>${report.keyName}</strong></td>
                    <td>${report.createdAt || 'N/A'}</td>
                    <td>
                        <div class="action-buttons-inline">
                            <button class="btn-action btn-action-edit" onclick="openReportForEdit(${report.id})" title="Edit">
                                <i class="fa-solid fa-pen"></i>
                            </button>
                            <button class="btn-action btn-action-word" onclick="downloadWordReport(${report.id})" title="Download Word">
                                <i class="fa-solid fa-file-word"></i>
                            </button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });

        }
    } catch (e) {
        console.error("Error loading recent", e);
    }
}


/* ========================================================
   5. CREATE REPORT
   ======================================================== */

document.getElementById("createReportForm")
    .addEventListener("submit", async function (event) {
        event.preventDefault();

        // Show loading state
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Creating...';
        submitBtn.disabled = true;

        try {
            // Build request object
            const request = {
                keyId: document.getElementById("keyId").value,
                keyName: document.getElementById("keyName").value,
                scope: document.getElementById("scope").value,
                scopeName: document.getElementById("scopeName").value,
                valueVolume: document.getElementById("valueVolume").value,
                unit: document.getElementById("unit").value,
                language: document.getElementById("language").value,
                historicYear: document.getElementById("historicYear").value ? parseInt(document.getElementById("historicYear").value) : null,
                baseYear: document.getElementById("baseYear").value ? parseInt(document.getElementById("baseYear").value) : null,
                forecastYear: document.getElementById("forecastYear").value ? parseInt(document.getElementById("forecastYear").value) : null,
                marketValueBaseYear: document.getElementById("marketValueBaseYear").value ? parseFloat(document.getElementById("marketValueBaseYear").value) : null,
                marketValueForecastYear: document.getElementById("marketValueForecastYear").value ? parseFloat(document.getElementById("marketValueForecastYear").value) : null,
                category: document.getElementById("category").value,
                segments: getSegmentsData(),
                companies: getCompaniesData()
            };

            const response = await fetch('/api/reports', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(request)
            });

            if (response.ok) {
                const reportId = await response.json();
                alert(`Sample Report Created Successfully! ID: ${reportId}`);
                resetReportForm();

                // Switch to edit section to view the new report
                showSection('edit-sample');
            } else {
                let errorMsg = "Failed to create report.";
                try {
                    const errorObj = await response.json();
                    if (errorObj.message) errorMsg = errorObj.message;
                } catch (e) { }
                alert(errorMsg);
            }
        } catch (error) {
            console.error("Error creating report:", error);
            alert("An error occurred while creating the report.");
        } finally {
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
    });

function resetReportForm() {
    document.getElementById("createReportForm").reset();
    document.getElementById("segmentsContainer").innerHTML = "";
    document.getElementById("companiesContainer").innerHTML = "";
}

// Data extraction helpers for Create
function getSegmentsData() {
    const container = document.getElementById("segmentsContainer");
    const segmentNodes = container.querySelectorAll(":scope > .segment-node");
    const segments = [];

    segmentNodes.forEach(node => {
        const seg = extractSegmentData(node);
        if (seg) segments.push(seg);
    });

    return segments;
}

function extractSegmentData(segmentNode) {
    const input = segmentNode.querySelector(":scope > .segment-row > .segment-input");
    if (!input || !input.value.trim()) return null;

    const segment = {
        segmentName: input.value.trim(),
        children: []
    };

    const childrenContainer = segmentNode.querySelector(":scope > .subsegments-container");
    if (childrenContainer) {
        const childNodes = childrenContainer.querySelectorAll(":scope > .segment-node");
        childNodes.forEach(childNode => {
            const childData = extractSegmentData(childNode);
            if (childData) segment.children.push(childData);
        });
    }

    return segment;
}

function getCompaniesData() {
    const container = document.getElementById("companiesContainer");
    const inputs = container.querySelectorAll(".company-input");
    const companies = [];

    inputs.forEach(input => {
        if (input.value.trim()) {
            companies.push({ companyName: input.value.trim() });
        }
    });

    return companies;
}

// UI builders for Create
function addRootSegment() {
    const container = document.getElementById("segmentsContainer");
    container.appendChild(createSegmentNode());
}

function createSegmentNode() {
    const segmentNode = document.createElement("div");
    segmentNode.className = "segment-node";

    segmentNode.innerHTML = `
        <div class="segment-row">
            <input type="text" class="segment-input form-control" placeholder="Segment Name">
            <button type="button" class="btn-icon-green" onclick="addSubsegment(this)" title="Add Subsegment">
                <i class="fa-solid fa-plus"></i>
            </button>
            <button type="button" class="btn-icon-red" onclick="removeSegment(this)" title="Remove">
                <i class="fa-solid fa-trash"></i>
            </button>
        </div>
        <div class="subsegments-container"></div>
    `;
    return segmentNode;
}

window.addSubsegment = function (button) {
    const segmentNode = button.closest(".segment-node");
    const childrenContainer = segmentNode.querySelector(":scope > .subsegments-container");
    childrenContainer.appendChild(createSegmentNode());
};

window.removeSegment = function (button) {
    button.closest(".segment-node").remove();
};

function addCompany() {
    const container = document.getElementById("companiesContainer");
    const companyRow = document.createElement("div");
    companyRow.className = "company-row mb-2 d-flex gap-2";

    companyRow.innerHTML = `
        <input type="text" class="company-input form-control" placeholder="Company Name">
        <button type="button" class="btn-icon-red" onclick="removeCompany(this)" title="Remove">
            <i class="fa-solid fa-trash"></i>
        </button>
    `;
    container.appendChild(companyRow);
}

window.removeCompany = function (button) {
    button.closest(".company-row").remove();
};


/* ========================================================
   6. EDIT / LIST REPORTS
   ======================================================== */

async function loadMyReports() {
    const tableBody = document.getElementById("reportsTableBody");
    tableBody.innerHTML = `<tr><td colspan="4" class="loading-cell"><i class="fa-solid fa-spinner fa-spin"></i> Loading reports...</td></tr>`;

    try {
        const endpoint = currentUser.isAdmin ? '/api/reports/all' : '/api/reports/my-reports';
        const response = await fetch(endpoint);

        if (response.ok) {
            const reports = await response.json();
            displayReportsList(reports, tableBody);
        } else {
            tableBody.innerHTML = `<tr><td colspan="4" class="error-cell">Failed to load reports.</td></tr>`;
        }
    } catch (error) {
        console.error("Error loading reports:", error);
        tableBody.innerHTML = `<tr><td colspan="4" class="error-cell">Network error occurred.</td></tr>`;
    }
}

function displayReportsList(reports, tableBodyElement) {
    tableBodyElement.innerHTML = '';

    if (!reports || reports.length === 0) {
        tableBodyElement.innerHTML = `<tr><td colspan="4" style="text-align: center;">No reports found.</td></tr>`;
        return;
    }

    reports.forEach(report => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${report.keyId}</td>
            <td><strong>${report.keyName}</strong></td>
            <td>${report.createdAt || 'N/A'}</td>
            <td>
                <div class="action-buttons-inline">
                    <button class="btn-action btn-action-edit" onclick="editReport(${report.id})" title="Edit / Update">
                        <i class="fa-solid fa-pen"></i> Update
                    </button>
                    <button class="btn-action btn-action-word" onclick="downloadWordReport(${report.id})" title="Download Word (.docx)">
                        <i class="fa-solid fa-file-word"></i>
                    </button>
                    <button class="btn-action btn-action-ppt" onclick="downloadPptReport(${report.id})" title="Download PPT">
                        <i class="fa-solid fa-file-powerpoint"></i>
                    </button>
                </div>
            </td>
        `;
        tableBodyElement.appendChild(tr);
    });
}

function openReportForEdit(id) {
    showSection('edit-sample');
    editReport(id);
}


/* ========================================================
   7. EDIT REPORT WORKFLOW
   ======================================================== */

async function editReport(reportId) {
    // UI Switches
    document.getElementById('reportsListContainer').style.display = 'none';
    document.getElementById('editReportContainer').style.display = 'flex';
    document.getElementById('editReportContent').style.display = 'none';
    document.getElementById('editReportLoading').style.display = 'flex';

    try {
        const response = await fetch(`/api/reports/${reportId}`);

        if (response.ok) {
            const report = await response.json();
            populateEditForm(report);

            document.getElementById('editReportLoading').style.display = 'none';
            document.getElementById('editReportContent').style.display = 'flex';
        } else {
            alert("Failed to load report details.");
            closeEditReport();
        }
    } catch (error) {
        console.error("Error loading report details:", error);
        alert("An error occurred loading the report.");
        closeEditReport();
    }
}

function closeEditReport() {
    document.getElementById('editReportContainer').style.display = 'none';
    document.getElementById('reportsListContainer').style.display = 'block';

    // Clear forms to be clean for next time
    document.getElementById("editSegmentsContainer").innerHTML = "";
    document.getElementById("editCompaniesContainer").innerHTML = "";

    // Refresh the list to show any updates
    loadMyReports();
}

function populateEditForm(report) {
    // hidden ID
    document.getElementById('editingReportId').value = report.id;

    // Fields
    document.getElementById('editKeyId').value = report.keyId || '';
    document.getElementById('editKeyName').value = report.keyName || '';
    document.getElementById('editScope').value = report.scope || '';
    document.getElementById('editScopeName').value = report.scopeName || '';
    document.getElementById('editValueVolume').value = report.valueVolume || '';
    document.getElementById('editUnit').value = report.unit || '';
    document.getElementById('editLanguage').value = report.language || '';
    document.getElementById('editHistoricYear').value = report.historicYear || '';
    document.getElementById('editBaseYear').value = report.baseYear || '';
    document.getElementById('editForecastYear').value = report.forecastYear || '';

    // New fields
    document.getElementById('editMarketValueBaseYear').value = report.marketValueBaseYear || '';
    document.getElementById('editMarketValueForecastYear').value = report.marketValueForecastYear || '';
    document.getElementById('editCategory').value = report.category || '';

    // Read-only info panel
    document.getElementById('infoPanelReportId').textContent = report.id;
    document.getElementById('infoPanelKeyId').textContent = report.keyId || 'N/A';
    document.getElementById('infoPanelCreatedBy').textContent = report.createdByUsername || 'N/A';
    document.getElementById('infoPanelCreatedAt').textContent = report.createdAt || 'N/A';
    document.getElementById('infoPanelCategory').textContent = report.category || 'General';

    // Segments
    const segmentsContainer = document.getElementById('editSegmentsContainer');
    segmentsContainer.innerHTML = '';
    if (report.segments && report.segments.length > 0) {
        report.segments.forEach(seg => {
            segmentsContainer.appendChild(createEditSegmentNode(seg));
        });
    }

    // Companies
    const companiesContainer = document.getElementById('editCompaniesContainer');
    companiesContainer.innerHTML = '';
    if (report.companies && report.companies.length > 0) {
        report.companies.forEach(comp => {
            addEditCompany(comp.companyName);
        });
    }
}


// UPDATE SUBMIT
async function updateSampleReport() {
    const reportId = document.getElementById('editingReportId').value;
    if (!reportId) return;

    const btn = document.getElementById('updateReportButton');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Updating...';
    btn.disabled = true;

    try {
        const request = {
            keyId: document.getElementById("editKeyId").value,
            keyName: document.getElementById("editKeyName").value,
            scope: document.getElementById("editScope").value,
            scopeName: document.getElementById("editScopeName").value,
            valueVolume: document.getElementById("editValueVolume").value,
            unit: document.getElementById("editUnit").value,
            language: document.getElementById("editLanguage").value,
            historicYear: document.getElementById("editHistoricYear").value ? parseInt(document.getElementById("editHistoricYear").value) : null,
            baseYear: document.getElementById("editBaseYear").value ? parseInt(document.getElementById("editBaseYear").value) : null,
            forecastYear: document.getElementById("editForecastYear").value ? parseInt(document.getElementById("editForecastYear").value) : null,
            marketValueBaseYear: document.getElementById("editMarketValueBaseYear").value ? parseFloat(document.getElementById("editMarketValueBaseYear").value) : null,
            marketValueForecastYear: document.getElementById("editMarketValueForecastYear").value ? parseFloat(document.getElementById("editMarketValueForecastYear").value) : null,
            category: document.getElementById("editCategory").value,
            segments: getEditSegmentsData(),
            companies: getEditCompaniesData()
        };

        const response = await fetch(`/api/reports/${reportId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(request)
        });

        if (response.ok) {
            alert("Report updated successfully!");
            // Refresh right side panel
            editReport(reportId);
        } else {
            let errorMsg = "Failed to update report.";
            try {
                const errorObj = await response.json();
                if (errorObj.message) errorMsg = errorObj.message;
            } catch (e) { }
            alert(errorMsg);
        }

    } catch (error) {
        console.error("Error updating report:", error);
        alert("An error occurred during update.");
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}


/* ========================================================
   8. COMPACT SEGMENTS & COMPANIES (EDIT VIEW)
   ======================================================== */

function addEditRootSegment() {
    const container = document.getElementById("editSegmentsContainer");
    container.appendChild(createEditSegmentNode({ segmentName: '', children: [] }));
}

function createEditSegmentNode(segment = { segmentName: '', children: [] }) {
    const segmentNode = document.createElement("div");
    segmentNode.className = "segment-node";

    segmentNode.innerHTML = `
        <div class="segment-row">
            <input type="text" class="segment-input form-control" placeholder="Segment Name" value="${escapeHtml(segment.segmentName || '')}">
            <button type="button" class="btn-icon-green" onclick="addEditSubsegment(this)" title="Add Subsegment">
                <i class="fa-solid fa-plus"></i>
            </button>
            <button type="button" class="btn-icon-red" onclick="removeEditSegment(this)" title="Remove">
                <i class="fa-solid fa-trash"></i>
            </button>
        </div>
        <div class="subsegments-container"></div>
    `;

    const subContainer = segmentNode.querySelector('.subsegments-container');
    if (segment.children && segment.children.length > 0) {
        segment.children.forEach(child => {
            subContainer.appendChild(createEditSegmentNode(child));
        });
    }

    return segmentNode;
}

window.addEditSubsegment = function (btn) {
    const segmentNode = btn.closest('.segment-node');
    const subContainer = segmentNode.querySelector(':scope > .subsegments-container');
    subContainer.appendChild(createEditSegmentNode({ segmentName: '', children: [] }));
};

window.removeEditSegment = function (btn) {
    btn.closest('.segment-node').remove();
};

function getEditSegmentsData() {
    const container = document.getElementById("editSegmentsContainer");
    const nodes = container.querySelectorAll(":scope > .segment-node");
    const segments = [];

    nodes.forEach(node => {
        const seg = extractEditSegmentData(node);
        if (seg) segments.push(seg);
    });

    return segments;
}

function extractEditSegmentData(node) {
    const input = node.querySelector(":scope > .segment-row > .segment-input");
    if (!input || !input.value.trim()) return null;

    const segment = {
        segmentName: input.value.trim(),
        children: []
    };

    const subContainer = node.querySelector(":scope > .subsegments-container");
    if (subContainer) {
        const childNodes = subContainer.querySelectorAll(":scope > .segment-node");
        childNodes.forEach(child => {
            const childData = extractEditSegmentData(child);
            if (childData) segment.children.push(childData);
        });
    }

    return segment;
}


// Companies
function addEditCompany(name = '') {
    const container = document.getElementById("editCompaniesContainer");
    const companyRow = document.createElement("div");
    companyRow.className = "company-row mb-2 d-flex gap-2";

    companyRow.innerHTML = `
        <input type="text" class="company-input form-control" placeholder="Company Name" value="${escapeHtml(name)}">
        <button type="button" class="btn-icon-red" onclick="removeEditCompany(this)" title="Remove">
            <i class="fa-solid fa-trash"></i>
        </button>
    `;
    container.appendChild(companyRow);
}

window.removeEditCompany = function (btn) {
    btn.closest('.company-row').remove();
};

function getEditCompaniesData() {
    const container = document.getElementById("editCompaniesContainer");
    const inputs = container.querySelectorAll(".company-input");
    const companies = [];

    inputs.forEach(input => {
        if (input.value.trim()) {
            companies.push({ companyName: input.value.trim() });
        }
    });

    return companies;
}

// Utility
function escapeHtml(unsafe) {
    return (unsafe || "").toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


/* ========================================================
   9. SEARCH REPORTS
   ======================================================== */

async function searchReport() {
    const query = document.getElementById("reportSearch").value.trim();
    if (!query) return;

    showSection('search-results');
    const tableBody = document.getElementById("searchResultsTableBody");
    const desc = document.getElementById("searchResultsDescription");

    tableBody.innerHTML = `<tr><td colspan="4" class="loading-cell"><i class="fa-solid fa-spinner fa-spin"></i> Searching...</td></tr>`;
    desc.textContent = `Search results for "${query}"`;

    try {
        const response = await fetch(`/api/reports/search?query=${encodeURIComponent(query)}`);

        if (response.ok) {
            const reports = await response.json();
            displayReportsList(reports, tableBody); // Reuse the same render function
        } else {
            tableBody.innerHTML = `<tr><td colspan="4" class="error-cell">Search failed.</td></tr>`;
        }
    } catch (error) {
        console.error("Search error:", error);
        tableBody.innerHTML = `<tr><td colspan="4" class="error-cell">Network error occurred.</td></tr>`;
    }
}


/* ========================================================
   10. DOWNLOADS
   ======================================================== */

function downloadWordReport(id) {
    window.location.href = `/api/reports/${id}/word`;
}

async function downloadPptReport(id) {
    try {
        const response = await fetch(`/api/reports/${id}/ppt`);
        const data = await response.json();
        alert(data.message || "PPT generation not implemented.");
    } catch (e) {
        alert("PPT generation coming soon.");
    }
}

// Helpers for the right-side panel buttons
function downloadWordReportCurrent() {
    const id = document.getElementById('editingReportId').value;
    if (id) downloadWordReport(id);
}

function downloadPptReportCurrent() {
    const id = document.getElementById('editingReportId').value;
    if (id) downloadPptReport(id);
}

/* ========================================================
   11. ADMIN USER MANAGEMENT
   ======================================================== */

async function loadUsers() {
    const tbody = document.getElementById("adminUsersTableBody");
    tbody.innerHTML = `<tr><td colspan="5" class="loading-cell"><i class="fa-solid fa-spinner fa-spin"></i> Loading users...</td></tr>`;

    try {
        const response = await fetch('/api/admin/users');
        if (response.ok) {
            const users = await response.json();
            tbody.innerHTML = '';

            if (users.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align: center;">No users found.</td></tr>`;
                return;
            }

            users.forEach(user => {
                const tr = document.createElement('tr');
                const rolesStr = user.roles.join(', ');
                const statusBadge = user.enabled
                    ? '<span class="status-badge status-active">Active</span>'
                    : '<span class="status-badge status-inactive" style="background-color: rgba(239, 68, 68, 0.1); color: var(--brand-danger);">Inactive</span>';

                tr.innerHTML = `
                    <td>${user.id}</td>
                    <td><strong>${escapeHtml(user.username)}</strong></td>
                    <td>${escapeHtml(rolesStr)}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div class="action-buttons-inline">
                            <button class="btn-action btn-action-edit" onclick="editUser(${user.id}, '${escapeHtml(user.username)}', '${escapeHtml(user.roles[0] || 'USER')}', ${user.enabled})" title="Edit User">
                                <i class="fa-solid fa-user-gear"></i> Modify
                            </button>
                            <button class="btn-action btn-action-delete" onclick="deleteUser(${user.id})" title="Delete User">
                                <i class="fa-solid fa-trash"></i> Delete
                            </button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = `<tr><td colspan="5" class="error-cell">Failed to load users.</td></tr>`;
        }
    } catch (error) {
        console.error("Error loading users:", error);
        tbody.innerHTML = `<tr><td colspan="5" class="error-cell">Network error loading users.</td></tr>`;
    }
}

function openCreateUserModal() {
    document.getElementById("userFormContainer").style.display = "block";
    document.getElementById("userFormTitle").textContent = "Create New User";
    document.getElementById("adminUserId").value = "";
    document.getElementById("adminUsername").value = "";
    document.getElementById("adminPassword").value = "";
    document.getElementById("adminPassword").required = true;
    document.getElementById("pwdRequiredStar").style.display = "inline";
    document.getElementById("pwdHint").style.display = "none";
    document.getElementById("adminRole").value = "USER";
    document.getElementById("adminUserStatusGroup").style.display = "none";
}

function editUser(id, username, roleName, enabled) {
    document.getElementById("userFormContainer").style.display = "block";
    document.getElementById("userFormTitle").textContent = "Modify User";
    document.getElementById("adminUserId").value = id;
    document.getElementById("adminUsername").value = username;
    document.getElementById("adminPassword").value = "";
    document.getElementById("adminPassword").required = false;
    document.getElementById("pwdRequiredStar").style.display = "none";
    document.getElementById("pwdHint").style.display = "block";
    document.getElementById("adminRole").value = roleName;
    document.getElementById("adminUserStatusGroup").style.display = "block";
    document.getElementById("adminUserEnabled").value = enabled ? "true" : "false";
}

function closeUserForm() {
    document.getElementById("userFormContainer").style.display = "none";
    document.getElementById("userAdminForm").reset();
}

async function saveUser(event) {
    event.preventDefault();
    const id = document.getElementById("adminUserId").value;
    const username = document.getElementById("adminUsername").value.trim();
    const password = document.getElementById("adminPassword").value;
    const roleName = document.getElementById("adminRole").value;
    const enabled = document.getElementById("adminUserEnabled").value === "true";

    const saveBtn = document.getElementById("saveUserBtn");
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...';
    saveBtn.disabled = true;

    try {
        let response;
        if (id) {
            // Update
            const payload = { username, roleName, enabled };
            if (password) payload.password = password;

            response = await fetch(`/api/admin/users/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(payload)
            });
        } else {
            // Create
            response = await fetch('/api/admin/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ username, password, roleName })
            });
        }

        if (response.ok) {
            alert(id ? "User modified successfully!" : "User created successfully!");
            closeUserForm();
            loadUsers();
        } else {
            let msg = "Failed to save user.";
            try {
                const data = await response.json();
                if (data.message) msg = data.message;
            } catch (e) { }
            alert(msg);
        }
    } catch (e) {
        console.error("Error saving user:", e);
        alert("An error occurred saving user.");
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

async function deleteUser(id) {
    if (!confirm("Are you sure you want to delete this user? This action cannot be undone.")) {
        return;
    }

    try {
        const response = await fetch(`/api/admin/users/${id}`, {
            method: 'DELETE',
            headers: {
                [csrfHeader]: csrfToken
            }
        });

        if (response.ok) {
            alert("User deleted successfully!");
            loadUsers();
        } else {
            let msg = "Failed to delete user.";
            try {
                const data = await response.json();
                if (data.message) msg = data.message;
            } catch (e) { }
            alert(msg);
        }
    } catch (e) {
        console.error("Error deleting user:", e);
        alert("An error occurred deleting user.");
    }
}