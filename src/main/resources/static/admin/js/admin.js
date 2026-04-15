const API_BASE_URL = window.location.origin + '/api';

// DOM Elements
const authModal = document.getElementById('auth-modal');
const appContainer = document.getElementById('app-container');
const loginForm = document.getElementById('adminLoginForm');

// State
let authToken = localStorage.getItem('adminToken');

document.addEventListener('DOMContentLoaded', () => {
    if (authToken) {
        showDashboard();
    } else {
        showLogin();
    }
});

// Authentication
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('adminEmail').value;
    const password = document.getElementById('adminPassword').value;
    const errorDiv = document.getElementById('authError');

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            authToken = data.data.token;
            // Decode token manually or optionally verify role in backend
            localStorage.setItem('adminToken', authToken);
            showDashboard();
        } else {
            errorDiv.textContent = data.message || 'Authentication failed. Please check credentials.';
        }
    } catch (err) {
        errorDiv.textContent = 'Server error. Please try again.';
    }
});

function logout() {
    localStorage.removeItem('adminToken');
    authToken = null;
    showLogin();
}

function showLogin() {
    authModal.classList.add('show');
    appContainer.style.display = 'none';
}

function showDashboard() {
    authModal.classList.remove('show');
    appContainer.style.display = 'flex';
    fetchMetrics();
    fetchTransactions();
    fetchUsers();
}

// Layout Handling
function switchTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    
    document.getElementById(`tab-${tabId}`).classList.add('active');
    event.currentTarget.classList.add('active');
}

// Fetchers
async function apiCall(endpoint) {
    const res = await fetch(`${API_BASE_URL}${endpoint}`, {
        headers: { 'Authorization': `Bearer ${authToken}` }
    });
    
    if (res.status === 401 || res.status === 403) {
        logout();
        throw new Error("Unauthorized");
    }
    
    return res.json();
}

async function fetchMetrics() {
    try {
        const res = await apiCall('/admin/metrics');
        if (res.success) {
            const metrics = res.data;
            document.getElementById('statTotalUsers').textContent = metrics.totalUsers;
            document.getElementById('statTotalTrx').textContent = metrics.totalTransactions;
            document.getElementById('statSuccessRate').textContent = metrics.successRate + '%';
            
            let totalVol = 0;
            const currencies = Object.keys(metrics.volumeByCurrency || {});
            
            // Just sum KES primarily or show raw
            const kshKey = currencies.find(c => c === 'KSH' || c === 'KES');
            if (kshKey) {
                totalVol = Number(metrics.volumeByCurrency[kshKey]).toLocaleString();
            } else if (currencies.length > 0) {
                totalVol = Number(metrics.volumeByCurrency[currencies[0]]).toLocaleString() + ' ' + currencies[0];
            }
            document.getElementById('statTotalVolume').textContent = totalVol;

            // Paint status summary
            const stSummary = document.getElementById('statusSummary');
            stSummary.innerHTML = `
                <div class="status-item"><span>Completed</span> <span class="badge success">${metrics.completedTransactions}</span></div>
                <div class="status-item"><span>Pending</span> <span class="badge warning">${metrics.pendingTransactions}</span></div>
                <div class="status-item"><span>Failed</span> <span class="badge danger">${metrics.failedTransactions}</span></div>
            `;
            
            // Paint currency volume
            const currSummary = document.getElementById('currencySummary');
            currSummary.innerHTML = currencies.map(c => `
                <div class="status-item"><span>${c}</span> <span style="font-weight: bold;">${metrics.volumeByCurrency[c].toLocaleString()}</span></div>
            `).join('') || '<p style="color: var(--text-secondary)">No data available</p>';
        }
    } catch (e) { console.error("Error fetching metrics", e);}
}

async function fetchTransactions() {
    try {
        const res = await apiCall('/admin/transactions?size=10');
        if (res.success && res.data.content) {
            const tbody = document.getElementById('transactionsTableBody');
            tbody.innerHTML = res.data.content.map(t => {
                let badgeClass = 'warning';
                if(t.status === 'COMPLETED') badgeClass = 'success';
                if(t.status === 'FAILED') badgeClass = 'danger';
                return `
                <tr>
                    <td>#${t.id}</td>
                    <td>${t.type}</td>
                    <td>${Number(t.amount).toLocaleString()} ${t.currency}</td>
                    <td><span class="badge ${badgeClass}">${t.status}</span></td>
                    <td>${new Date(t.timestamp).toLocaleDateString()}</td>
                </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error("Transactions fetch error", e); }
}

async function fetchUsers() {
    try {
        const res = await apiCall('/admin/users?size=15');
        if (res.success && res.data.content) {
            const tbody = document.getElementById('usersTableBody');
            tbody.innerHTML = res.data.content.map(u => `
                <tr>
                    <td>#${u.id}</td>
                    <td>${u.fullName}</td>
                    <td>${u.email}</td>
                    <td>${u.role || 'USER'}</td>
                    <td>
                        <button class="btn primary-btn" style="padding: 4px 12px; font-size: 12px; width: auto;">View</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error("Users fetch error", e); }
}
