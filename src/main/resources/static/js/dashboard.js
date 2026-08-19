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

const REGIONAL_COUNTRIES = {
    "North America": ["U.S.", "Canada", "Mexico"],
    "Europe": ["Germany", "France", "UK", "Italy", "Spain", "Rest of Europe"],
    "Asia Pacific": ["China", "Japan", "India", "South Korea", "Australia", "Rest of APAC"],
    "South America": ["Brazil", "Argentina", "Rest of South America"],
    "MEA": ["GCC Countries", "South Africa", "Rest of MEA"]
};

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

    bindCustomizationInputs();
    initRegionalScopeUi();

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
                            <button class="btn-action btn-action-word" onclick="downloadPdfReport(${report.id}, this)" title="Download Pdf">
                                <i class="fa-solid fa-file-pdf"></i>
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


// Import a sample report from a public URL and open it in the edit UI
async function importReportFromUrl() {
    const urlInput = document.getElementById('importReportUrl');
    const statusEl = document.getElementById('importReportStatus');
    const btn = document.getElementById('fetchReportBtn');

    if (!urlInput) return;
    const url = urlInput.value.trim();
    if (!url) {
        statusEl.textContent = 'Please enter a report URL.';
        return;
    }

    const originalBtnHtml = btn ? btn.innerHTML : null;
    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Fetching...';
        }
        statusEl.textContent = '';

        const response = await fetch('/api/sample-reports/import-url', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ url: url })
        });

        if (response.ok) {
            const data = await response.json();
            // Support both { sampleReportId: 123 } and plain numeric responses
            const importedId = (data && (data.sampleReportId || data.id)) || data || null;
            if (importedId) {
                statusEl.style.color = 'green';
                statusEl.textContent = 'Import successful — opening report...';
                // Open edit UI and load the imported report
                showSection('edit-sample');
                // small delay to ensure panel transitions
                setTimeout(() => editReport(importedId), 250);
                return;
            } else {
                statusEl.textContent = 'Import succeeded but no report ID was returned.';
            }
        } else {
            let msg = 'Import failed.';
            try {
                const err = await response.json();
                if (err && err.message) msg = err.message;
            } catch (e) { }
            statusEl.textContent = msg;
        }

    } catch (error) {
        console.error('Error importing report from URL:', error);
        statusEl.textContent = 'Network error occurred while importing.';
    } finally {
        if (btn) {
            btn.disabled = false;
            if (originalBtnHtml) btn.innerHTML = originalBtnHtml;
        }
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
            const scope = document.getElementById("scope").value;
            const scopeName = resolveScopeName("scope", "scopeName", "regionSelect");
            if (scope === "Regional" && !scopeName) {
                alert("Please select a region.");
                return;
            }

            // Build request object
            const request = {
                keyId: document.getElementById("keyId").value,
                keyName: document.getElementById("keyName").value,
                scope: scope,
                scopeName: scopeName,
                valueVolume: document.getElementById("valueVolume").value,
                measurementType: document.getElementById("valueVolume").value,
                currency: document.getElementById("currency").value,
                measurementUnit: document.getElementById("unit").value,
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
    syncRegionalScopeUi("scope", "scopeNameGroup", "regionalSection", "regionSelect", "regionCountries");
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
            <input type="text" class="segment-input form-control" placeholder="Segment name">
            <button type="button" class="btn-icon-green" onclick="addSubsegment(this)" title="Add Subsegment" aria-label="Add Subsegment">
                <i class="fa-solid fa-plus"></i>
            </button>
            <button type="button" class="btn-icon-red" onclick="removeSegment(this)" title="Remove Segment" aria-label="Remove Segment">
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
    companyRow.className = "company-row";

    companyRow.innerHTML = `
        <i class="fa-regular fa-building company-row-icon" aria-hidden="true"></i>
        <input type="text" class="company-input form-control" placeholder="Company name">
        <button type="button" class="btn-icon-red" onclick="removeCompany(this)" title="Remove Company" aria-label="Remove Company">
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
                    <button class="btn-action btn-action-word" onclick="downloadPdfReport(${report.id}, this)" title="Download Pdf (.pdf)">
                        <i class="fa-solid fa-file-pdf"></i>
                    </button>
                   
                    <button class="btn-action btn-action-delete" onclick="deleteReport(${report.id})" title="Delete Report">
                        <i class="fa-solid fa-trash"></i> Delete
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

async function deleteReport(reportId) {
    const statusEl = document.getElementById('reportActionStatus');
    statusEl.textContent = '';

    if (!confirm('Are you sure you want to delete this report?')) {
        return;
    }

    try {
        const response = await fetch(`/api/reports/${reportId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            statusEl.textContent = 'Report deleted successfully.';
            statusEl.style.color = 'var(--brand-success)';
            loadMyReports();
            loadDashboardStats();
            loadDashboardRecentReports();
        } else {
            let errorMessage = 'Failed to delete report.';
            try {
                const errorData = await response.json();
                if (errorData.message) {
                    errorMessage = errorData.message;
                }
            } catch (e) {
            }
            statusEl.textContent = errorMessage;
            statusEl.style.color = 'var(--brand-danger)';
        }
    } catch (error) {
        console.error('Error deleting report:', error);
        statusEl.textContent = 'Network error occurred while deleting report.';
        statusEl.style.color = 'var(--brand-danger)';
    }
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
            loadReportCustomization(reportId);

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
    currentReportConfig = null;

    // Refresh the list to show any updates
    loadMyReports();
}

function initRegionalScopeUi() {
    fillRegionSelect("regionSelect");
    fillRegionSelect("editRegionSelect");

    const createScope = document.getElementById("scope");
    const editScope = document.getElementById("editScope");
    const regionSelect = document.getElementById("regionSelect");
    const editRegionSelect = document.getElementById("editRegionSelect");

    if (createScope) {
        createScope.addEventListener("change", function () {
            syncRegionalScopeUi("scope", "scopeNameGroup", "regionalSection", "regionSelect", "regionCountries");
        });
    }
    if (editScope) {
        editScope.addEventListener("change", function () {
            syncRegionalScopeUi("editScope", "editScopeNameGroup", "editRegionalSection", "editRegionSelect", "editRegionCountries");
        });
    }
    if (regionSelect) {
        regionSelect.addEventListener("change", function () {
            renderRegionCountries("regionCountries", regionSelect.value);
        });
    }
    if (editRegionSelect) {
        editRegionSelect.addEventListener("change", function () {
            renderRegionCountries("editRegionCountries", editRegionSelect.value);
        });
    }

    syncRegionalScopeUi("scope", "scopeNameGroup", "regionalSection", "regionSelect", "regionCountries");
}

function fillRegionSelect(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    const current = select.value;
    select.innerHTML = '<option value="">Select Region</option>';
    Object.keys(REGIONAL_COUNTRIES).forEach(region => {
        const option = document.createElement("option");
        option.value = region;
        option.textContent = region;
        select.appendChild(option);
    });
    if (current && REGIONAL_COUNTRIES[current]) {
        select.value = current;
    }
}

function syncRegionalScopeUi(scopeId, scopeNameGroupId, regionalSectionId, regionSelectId, countriesId) {
    const scopeEl = document.getElementById(scopeId);
    const scopeNameGroup = document.getElementById(scopeNameGroupId);
    const regionalSection = document.getElementById(regionalSectionId);
    const regionSelect = document.getElementById(regionSelectId);
    const isRegional = scopeEl && scopeEl.value === "Regional";

    if (regionalSection) {
        regionalSection.hidden = !isRegional;
    }
    if (scopeNameGroup) {
        scopeNameGroup.style.display = isRegional ? "none" : "";
    }
    if (isRegional && regionSelect) {
        renderRegionCountries(countriesId, regionSelect.value);
    }
}

function renderRegionCountries(containerId, regionName) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = "";
    const countries = REGIONAL_COUNTRIES[regionName] || [];
    countries.forEach(country => {
        const chip = document.createElement("span");
        chip.className = "region-country-chip";
        chip.textContent = country;
        container.appendChild(chip);
    });
}

function resolveScopeName(scopeId, scopeNameId, regionSelectId) {
    const scope = document.getElementById(scopeId).value;
    if (scope === "Regional") {
        return document.getElementById(regionSelectId).value;
    }
    return document.getElementById(scopeNameId).value;
}

function populateEditForm(report) {
    // hidden ID
    document.getElementById('editingReportId').value = report.id;

    // Fields
    document.getElementById('editKeyId').value = report.keyId || '';
    document.getElementById('editKeyName').value = report.keyName || '';
    document.getElementById('editScope').value = report.scope || '';
    document.getElementById('editScopeName').value = report.scopeName || '';
    const editRegionSelect = document.getElementById('editRegionSelect');
    if (editRegionSelect) {
        const savedRegion = report.scopeName || '';
        editRegionSelect.value = REGIONAL_COUNTRIES[savedRegion] ? savedRegion : '';
        renderRegionCountries('editRegionCountries', editRegionSelect.value);
    }
    syncRegionalScopeUi('editScope', 'editScopeNameGroup', 'editRegionalSection', 'editRegionSelect', 'editRegionCountries');
    document.getElementById('editValueVolume').value = report.measurementType || report.valueVolume || 'Value';
    document.getElementById('editCurrency').value = report.currency || 'USD';
    document.getElementById('editUnit').value = report.measurementUnit || 'Million';
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
        const scope = document.getElementById("editScope").value;
        const scopeName = resolveScopeName("editScope", "editScopeName", "editRegionSelect");
        if (scope === "Regional" && !scopeName) {
            alert("Please select a region.");
            btn.innerHTML = originalText;
            btn.disabled = false;
            return;
        }

        const request = {
            keyId: document.getElementById("editKeyId").value,
            keyName: document.getElementById("editKeyName").value,
            scope: scope,
            scopeName: scopeName,
            valueVolume: document.getElementById("editValueVolume").value,
            measurementType: document.getElementById("editValueVolume").value,
            currency: document.getElementById("editCurrency").value,
            measurementUnit: document.getElementById("editUnit").value,
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
   7b. REPORT CUSTOMIZATION (EDIT VIEW)
   ======================================================== */

let currentReportConfig = null;

const THEME_COLOR_FIELDS = [
    { key: 'headerColor', textId: 'editHeaderColor', pickerId: 'editHeaderColorPicker', fallback: '#0070C0' },
    { key: 'footerColor', textId: 'editFooterColor', pickerId: 'editFooterColorPicker', fallback: '#FFB81C' },
    { key: 'primaryColor', textId: 'editPrimaryColor', pickerId: 'editPrimaryColorPicker', fallback: '#002060' },
    { key: 'secondaryColor', textId: 'editSecondaryColor', pickerId: 'editSecondaryColorPicker', fallback: '#2B6CB0' },
    { key: 'accentColor', textId: 'editAccentColor', pickerId: 'editAccentColorPicker', fallback: '#ECC94B' },
    { key: 'tableHeaderColor', textId: 'editTableHeaderColor', pickerId: 'editTableHeaderColorPicker', fallback: '#002060' },
    { key: 'chartPrimaryColor', textId: 'editChartPrimaryColor', pickerId: 'editChartPrimaryColorPicker', fallback: '#002060' },
    { key: 'chartSecondaryColor', textId: 'editChartSecondaryColor', pickerId: 'editChartSecondaryColorPicker', fallback: '#FFC107' }
];

function normalizeHexColor(value, fallback) {
    if (!value) return fallback;
    let hex = String(value).trim();
    if (!hex.startsWith('#')) hex = '#' + hex;
    if (/^#[0-9A-Fa-f]{6}$/.test(hex)) return hex.toUpperCase();
    if (/^#[0-9A-Fa-f]{3}$/.test(hex)) {
        return ('#' + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3]).toUpperCase();
    }
    return fallback;
}

function setThemeColorField(textId, pickerId, value, fallback) {
    const hex = normalizeHexColor(value, fallback);
    const text = document.getElementById(textId);
    const picker = document.getElementById(pickerId);
    if (text) text.value = hex;
    if (picker) picker.value = hex;
}

function coverImageFromConfig(config) {
    const theme = config && config.theme ? config.theme : {};
    const cover = config && config.cover ? config.cover : {};
    return cover.backgroundImage || theme.coverImage || '';
}

function previewSrcForCover(path) {
    if (!path) return '';
    if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('/')) {
        return path;
    }
    return '';
}

function applyCoverPreview(path) {
    const preview = document.getElementById('editCoverImagePreview');
    if (!preview) return;
    const src = previewSrcForCover(path);
    if (src) {
        preview.src = src;
        preview.style.display = 'block';
    } else {
        preview.removeAttribute('src');
        preview.style.display = 'none';
    }
}

function populateCustomizationForm(config) {
    currentReportConfig = config || {};
    const theme = currentReportConfig.theme || {};
    const coverPath = coverImageFromConfig(currentReportConfig);
    document.getElementById('editCoverImage').value = coverPath;
    applyCoverPreview(coverPath);

    const headerFallback = normalizeHexColor(theme.secondaryColor, '#0070C0');
    const footerFallback = normalizeHexColor(theme.accentColor, '#FFB81C');

    THEME_COLOR_FIELDS.forEach(field => {
        let fallback = field.fallback;
        if (field.key === 'headerColor') fallback = headerFallback;
        if (field.key === 'footerColor') fallback = footerFallback;
        setThemeColorField(field.textId, field.pickerId, theme[field.key], fallback);
    });
}

async function loadReportCustomization(reportId) {
    try {
        const response = await fetch(`/api/reports/${reportId}/export/json`);
        if (!response.ok) {
            currentReportConfig = null;
            return;
        }
        populateCustomizationForm(await response.json());
    } catch (error) {
        console.error("Error loading report customization:", error);
    }
}

function bindCustomizationInputs() {
    THEME_COLOR_FIELDS.forEach(field => {
        const text = document.getElementById(field.textId);
        const picker = document.getElementById(field.pickerId);
        if (picker) {
            picker.addEventListener('input', () => {
                if (text) text.value = picker.value.toUpperCase();
            });
        }
        if (text) {
            text.addEventListener('change', () => {
                const hex = normalizeHexColor(text.value, picker ? picker.value : field.fallback);
                text.value = hex;
                if (picker) picker.value = hex;
            });
        }
    });

    const coverInput = document.getElementById('editCoverImage');
    if (coverInput) {
        coverInput.addEventListener('change', () => applyCoverPreview(coverInput.value));
    }

    const coverFile = document.getElementById('editCoverImageFile');
    if (coverFile) {
        coverFile.addEventListener('change', async () => {
            if (!coverFile.files || !coverFile.files[0]) return;
            const formData = new FormData();
            formData.append('file', coverFile.files[0]);
            formData.append('category', document.getElementById('editCategory').value || 'General');
            try {
                const response = await fetch('/api/assets/upload', {
                    method: 'POST',
                    headers: { [csrfHeader]: csrfToken },
                    body: formData
                });
                if (!response.ok) {
                    alert('Failed to upload cover image.');
                    return;
                }
                const asset = await response.json();
                if (asset.filePath) {
                    document.getElementById('editCoverImage').value = asset.filePath;
                    applyCoverPreview(asset.filePath);
                }
            } catch (error) {
                console.error('Error uploading cover image:', error);
                alert('An error occurred uploading the cover image.');
            }
        });
    }
}

function buildCustomizationPayload() {
    const config = JSON.parse(JSON.stringify(currentReportConfig || {}));
    if (!config.theme || typeof config.theme !== 'object') {
        config.theme = {};
    }
    if (!config.cover || typeof config.cover !== 'object') {
        config.cover = {};
    }

    const coverPath = document.getElementById('editCoverImage').value.trim();
    config.theme.coverImage = coverPath;
    config.cover.backgroundImage = coverPath;

    THEME_COLOR_FIELDS.forEach(field => {
        const text = document.getElementById(field.textId);
        config.theme[field.key] = normalizeHexColor(text.value, field.fallback);
    });

    return config;
}

async function saveReportCustomization() {
    const reportId = document.getElementById('editingReportId').value;
    if (!reportId) return;

    const btn = document.getElementById('saveCustomizationButton');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    try {
        if (!currentReportConfig) {
            const loaded = await fetch(`/api/reports/${reportId}/export/json`);
            if (!loaded.ok) {
                alert('Failed to load current report configuration.');
                return;
            }
            currentReportConfig = await loaded.json();
        }

        const response = await fetch(`/api/reports/${reportId}/export/json`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(buildCustomizationPayload())
        });
        if (response.ok) {
            populateCustomizationForm(await response.json());
            alert('Report customization saved.');
        } else {
            alert('Failed to save report customization.');
        }
    } catch (error) {
        console.error('Error saving report customization:', error);
        alert('An error occurred saving customization.');
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

async function resetReportCustomization() {
    const reportId = document.getElementById('editingReportId').value;
    if (!reportId) return;
    if (!confirm('Reset customization to the original defaults for this report?')) {
        return;
    }

    try {
        const response = await fetch(`/api/reports/${reportId}/export/json/reset`, {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        });
        if (response.ok) {
            populateCustomizationForm(await response.json());
            alert('Customization reset to default.');
        } else {
            alert('Failed to reset customization.');
        }
    } catch (error) {
        console.error('Error resetting report customization:', error);
        alert('An error occurred resetting customization.');
    }
}

window.saveReportCustomization = saveReportCustomization;
window.resetReportCustomization = resetReportCustomization;


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
            <input type="text" class="segment-input form-control" placeholder="Segment name" value="${escapeHtml(segment.segmentName || '')}">
            <button type="button" class="btn-icon-green" onclick="addEditSubsegment(this)" title="Add Subsegment" aria-label="Add Subsegment">
                <i class="fa-solid fa-plus"></i>
            </button>
            <button type="button" class="btn-icon-red" onclick="removeEditSegment(this)" title="Remove Segment" aria-label="Remove Segment">
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
    companyRow.className = "company-row";

    companyRow.innerHTML = `
        <i class="fa-regular fa-building company-row-icon" aria-hidden="true"></i>
        <input type="text" class="company-input form-control" placeholder="Company name" value="${escapeHtml(name)}">
        <button type="button" class="btn-icon-red" onclick="removeEditCompany(this)" title="Remove Company" aria-label="Remove Company">
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

let pdfDownloadInProgress = false;

async function waitForPdfReady(id) {
    const started = Date.now();
    while (Date.now() - started < 180000) {
        const response = await fetch(`/reports/${id}/pdf-status`);
        if (!response.ok) {
            throw new Error(`PDF status check failed: ${response.status}`);
        }
        const payload = await response.json();
        if (payload.status === 'READY') {
            return;
        }
        if (payload.status === 'FAILED') {
            throw new Error(payload.message || 'PDF generation failed');
        }
        await new Promise((resolve) => setTimeout(resolve, 1500));
    }
    throw new Error('PDF is still being prepared. Please try again shortly.');
}

async function downloadPdfReport(id, triggerBtn) {
    const pdfBtn = triggerBtn || document.getElementById('downloadWordBtn');
    const pdfIcon = pdfBtn ? pdfBtn.querySelector('i') : document.getElementById('pdfDownloadIcon');
    const pdfText = pdfBtn ? pdfBtn.querySelector('span') : document.getElementById('pdfDownloadText');
    const pdfError = document.getElementById('pdfDownloadError');
    const idleIconClass = pdfIcon ? pdfIcon.className : 'fa-solid fa-file-pdf';
    const idleText = pdfText ? pdfText.textContent : 'PDF';

    const setPdfButtonVisualLoading = (loading) => {
        if (!pdfBtn) return;
        pdfBtn.classList.toggle('loading', loading);
        pdfBtn.setAttribute('aria-busy', loading ? 'true' : 'false');
        if (pdfIcon) {
            pdfIcon.className = loading ? 'fa-solid fa-spinner fa-spin' : idleIconClass;
        }
        if (pdfText) {
            pdfText.textContent = loading ? 'Preparing PDF...' : idleText;
        }
    };

    const showPdfError = (message) => {
        if (!pdfError) return;
        pdfError.textContent = message;
        pdfError.hidden = false;
    };

    const clearPdfError = () => {
        if (!pdfError) return;
        pdfError.textContent = '';
        pdfError.hidden = true;
    };

    if (!id) {
        console.error('PDF download failed: no report ID available in the current dashboard state.');
        showPdfError('No report is selected for PDF download.');
        return;
    }

    if (pdfDownloadInProgress) {
        return;
    }

    pdfDownloadInProgress = true;
    clearPdfError();

    // Chromium will not paint innerText/icon changes on the clicked button if
    // disabled is set in the same turn as the click. Update visuals first.
    setPdfButtonVisualLoading(true);
    void (pdfBtn && pdfBtn.offsetWidth);
    await new Promise((resolve) => requestAnimationFrame(resolve));
    await new Promise((resolve) => requestAnimationFrame(resolve));
    if (pdfBtn) {
        pdfBtn.disabled = true;
    }

    try {
        await waitForPdfReady(id);
        const response = await fetch(`/reports/${id}/download`);
        if (response.status === 202) {
            await waitForPdfReady(id);
            const retry = await fetch(`/reports/${id}/download`);
            if (!retry.ok) {
                throw new Error(`PDF download failed: ${retry.status}`);
            }
            const blob = await retry.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `report-${id}.pdf`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            return;
        }
        if (response.status === 503) {
            const payload = await response.json().catch(() => ({}));
            throw new Error(payload.message || 'PDF generation failed');
        }
        if (!response.ok) {
            throw new Error(`PDF download failed: ${response.status}`);
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `report-${id}.pdf`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    } catch (error) {
        console.error('PDF download error for report', id, error);
        showPdfError(error.message || 'Unable to download PDF right now.');
    } finally {
        if (pdfBtn) {
            pdfBtn.disabled = false;
        }
        setPdfButtonVisualLoading(false);
        pdfDownloadInProgress = false;
    }
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
function downloadPdfReportCurrent() {
    const idInput = document.getElementById('editingReportId');
    const id = idInput ? idInput.value : '';

    if (!id) {
        console.error('PDF download failed: no current report ID found in editingReportId.');
        const pdfError = document.getElementById('pdfDownloadError');
        if (pdfError) {
            pdfError.textContent = 'No report is selected for PDF download.';
            pdfError.hidden = false;
        }
        return;
    }

    downloadPdfReport(id, document.getElementById('downloadWordBtn'));
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