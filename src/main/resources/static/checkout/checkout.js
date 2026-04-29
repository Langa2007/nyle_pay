/**
 * NylePay Checkout Page — checkout.js
 *
 * Handles:
 *  - Session loading from /api/merchant/pay/{reference}
 *  - M-Pesa STK Push initiation + polling
 *  - Paystack card popup
 *  - NylePay Wallet login + direct payment
 *  - Success / failure state management
 */

const API_BASE = '/api/merchant/pay';
const AUTH_API = '/api/auth';

// ── Extract reference from URL ─────────────────────────────────────────────
// Merchant payment URLs follow: /checkout/index.html?ref=NPY-LNK-...
// or the Spring Boot route serves it as: /checkout/{ref}
const urlParams = new URLSearchParams(window.location.search);
const REFERENCE = urlParams.get('ref') || window.location.pathname.split('/').pop();

// ── State ──────────────────────────────────────────────────────────────────
let SESSION      = null;   // Full session object from API
let POLL_TIMER   = null;   // M-Pesa polling interval handle
let WALLET_TOKEN = null;   // JWT for NylePay wallet payment
let WALLET_USER  = null;   // { userId, email, balance }
let PAYSTACK_KEY = null;   // Pulled from meta or env (set by server via JS global)

// The server can optionally inject PAYSTACK_PUBLIC_KEY via a <script> tag:
// <script>window.PAYSTACK_PUBLIC_KEY = 'pk_live_...';</script>
// We fall back to the global if set.
if (typeof window.PAYSTACK_PUBLIC_KEY !== 'undefined') {
    PAYSTACK_KEY = window.PAYSTACK_PUBLIC_KEY;
}

// ── Initialise ─────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', loadSession);

async function loadSession() {
    if (!REFERENCE || REFERENCE === 'index.html') {
        showPageError('No payment reference found. Please use the full payment link provided by the merchant.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/${REFERENCE}`);
        const data = await res.json();

        if (!res.ok || !data.success) {
            showPageError(data.message || 'This payment link is unavailable.');
            return;
        }

        SESSION = data.data;

        if (SESSION.status === 'COMPLETED') {
            showSuccess(SESSION.reference);
            return;
        }

        renderSession();
        show('checkout-main');
        hide('page-loader');

    } catch (err) {
        showPageError('Unable to load payment details. Please check your internet connection and try again.');
        console.error('Session load error:', err);
    }
}

function renderSession() {
    const fmt = (amount, currency) =>
        new Intl.NumberFormat('en-KE', { style: 'currency', currency: currency || 'KES', minimumFractionDigits: 2 })
            .format(amount);

    const amountFmt = fmt(SESSION.amount, SESSION.currency);

    setText('merchant-name',  SESSION.merchantName);
    setText('merchant-avatar', SESSION.merchantName ? SESSION.merchantName[0].toUpperCase() : 'N');
    setText('summary-desc',   SESSION.description || 'Payment');
    setText('summary-amount', amountFmt);
    setText('summary-total',  amountFmt);
    setText('tx-ref-code',    SESSION.reference);
    document.title = `Pay ${amountFmt} — NylePay`;
}

// ── Tab Switching ──────────────────────────────────────────────────────────
function switchTab(tab) {
    ['mpesa', 'card', 'wallet'].forEach(t => {
        document.getElementById(`tab-${t}`).classList.toggle('active', t === tab);
        document.getElementById(`content-${t}`).classList.toggle('hidden', t !== tab);
    });
}

// ── M-Pesa Payment ─────────────────────────────────────────────────────────
async function payMpesa() {
    const rawPhone = document.getElementById('mpesa-phone').value.trim();
    const email    = document.getElementById('mpesa-email').value.trim();
    const phone    = '254' + rawPhone.replace(/^0/, '').replace(/\s/g, '');

    if (!/^2547[0-9]{8}$/.test(phone)) {
        alert('Please enter a valid Safaricom number. Example: 712345678');
        return;
    }

    setLoading('mpesa-pay-btn', true);
    try {
        const res = await fetch(`${API_BASE}/${REFERENCE}/initiate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ method: 'MPESA', phone, email })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Failed to send M-Pesa request. Please try again.');
            setLoading('mpesa-pay-btn', false);
            return;
        }

        // Show polling state
        hide('mpesa-pay-btn');
        show('mpesa-pending');
        startPolling();

    } catch (err) {
        alert('Network error. Please check your connection and try again.');
        setLoading('mpesa-pay-btn', false);
        console.error('M-Pesa initiation error:', err);
    }
}

function startPolling() {
    let attempts = 0;
    const MAX_ATTEMPTS = 24; // 2 minutes (24 × 5s)

    POLL_TIMER = setInterval(async () => {
        attempts++;
        try {
            const res  = await fetch(`${API_BASE}/${REFERENCE}/status`);
            const data = await res.json();
            const status = data.data?.status;

            if (status === 'COMPLETED') {
                clearInterval(POLL_TIMER);
                showSuccess(REFERENCE);
            } else if (status === 'FAILED') {
                clearInterval(POLL_TIMER);
                showFailed('Your M-Pesa payment was not completed. Please try again.');
            } else if (attempts >= MAX_ATTEMPTS) {
                clearInterval(POLL_TIMER);
                showFailed('M-Pesa confirmation timed out. If you completed the payment, please contact support with your reference: ' + REFERENCE);
            }
        } catch (e) {
            console.warn('Polling error:', e);
        }
    }, 5000);
}

function cancelMpesaPoll() {
    if (POLL_TIMER) clearInterval(POLL_TIMER);
    hide('mpesa-pending');
    show('mpesa-pay-btn');
    setLoading('mpesa-pay-btn', false);
}

// ── Card Payment (Paystack) ────────────────────────────────────────────────
async function payCard() {
    const email = document.getElementById('card-email').value.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        alert('Please enter a valid email address for your receipt.');
        return;
    }

    setLoading('card-pay-btn', true);

    try {
        // Tell backend we're using card so it sets provider = PAYSTACK on the session
        const res = await fetch(`${API_BASE}/${REFERENCE}/initiate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ method: 'CARD', email })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Card payment could not be initiated.');
            setLoading('card-pay-btn', false);
            return;
        }

        // Open Paystack inline popup
        const key = PAYSTACK_KEY || 'pk_test_mock';
        const handler = PaystackPop.setup({
            key,
            email,
            amount: data.data.amount,   // in kobo/cents
            currency: data.data.currency || 'KES',
            ref: REFERENCE,
            onClose: () => {
                setLoading('card-pay-btn', false);
            },
            callback: (response) => {
                // Paystack calls this on success — start polling to confirm on our end
                startPolling();
                hide('card-pay-btn');
            }
        });
        handler.openIframe();

    } catch (err) {
        alert('Network error. Please try again.');
        setLoading('card-pay-btn', false);
        console.error('Card payment error:', err);
    }
}

// ── NylePay Wallet ─────────────────────────────────────────────────────────
async function walletLogin() {
    const email    = document.getElementById('wallet-email').value.trim();
    const password = document.getElementById('wallet-password').value;

    if (!email || !password) {
        alert('Please enter your NylePay email and password.');
        return;
    }

    try {
        const res = await fetch(`${AUTH_API}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Invalid credentials. Please try again.');
            return;
        }

        WALLET_TOKEN = data.data.token;
        WALLET_USER  = { userId: data.data.userId, email: data.data.email };

        setText('wallet-user-email', WALLET_USER.email);
        setText('wallet-balance', `Loading…`);

        hide('wallet-login-form');
        show('wallet-pay-form');

        // TODO: load actual wallet balance from /api/user/{id}/wallet
        setText('wallet-balance', `${SESSION.currency} (available)`);

    } catch (err) {
        alert('Login failed. Please check your connection and try again.');
        console.error('Wallet login error:', err);
    }
}

async function payWallet() {
    if (!WALLET_TOKEN || !WALLET_USER) {
        alert('Please log in to your NylePay account first.');
        return;
    }

    setLoading('wallet-pay-btn', true);

    try {
        const res = await fetch(`${API_BASE}/${REFERENCE}/initiate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${WALLET_TOKEN}`
            },
            body: JSON.stringify({
                method: 'NYLEPAY_WALLET',
                userId: WALLET_USER.userId
            })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Payment failed. Check your wallet balance and try again.');
            setLoading('wallet-pay-btn', false);
            return;
        }

        showSuccess(REFERENCE);

    } catch (err) {
        alert('Payment failed. Please try again.');
        setLoading('wallet-pay-btn', false);
        console.error('Wallet payment error:', err);
    }
}

function walletLogout() {
    WALLET_TOKEN = null;
    WALLET_USER  = null;
    show('wallet-login-form');
    hide('wallet-pay-form');
}

// ── Result Screens ─────────────────────────────────────────────────────────
function showSuccess(reference) {
    hide('checkout-main');
    hide('page-loader');
    hide('failed-screen');

    setText('success-tx-code', reference);

    if (SESSION?.redirectUrl) {
        setText('redirect-note', 'Redirecting you back to the merchant in 5 seconds…');
        setTimeout(doRedirect, 5000);
    } else {
        hide('redirect-note');
    }

    show('success-screen');
}

function showFailed(reason) {
    hide('checkout-main');
    hide('page-loader');
    hide('success-screen');

    setText('fail-reason', reason || 'Payment was unsuccessful. Please try again.');
    show('failed-screen');
}

function doRedirect() {
    if (SESSION?.redirectUrl) {
        window.location.href = SESSION.redirectUrl + '?ref=' + REFERENCE + '&status=success';
    }
}

function retryPayment() {
    hide('failed-screen');
    show('checkout-main');
    cancelMpesaPoll();
    switchTab('mpesa');
}

// ── Page Error ─────────────────────────────────────────────────────────────
function showPageError(message) {
    hide('page-loader');
    hide('checkout-main');
    setText('page-error-message', message);
    show('page-error');
}

// ── Helpers ────────────────────────────────────────────────────────────────
function show(id)  { document.getElementById(id)?.classList.remove('hidden'); }
function hide(id)  { document.getElementById(id)?.classList.add('hidden'); }
function setText(id, text) { const el = document.getElementById(id); if (el) el.textContent = text; }

function setLoading(btnId, loading) {
    const btn     = document.getElementById(btnId);
    const text    = btn?.querySelector('.btn-text');
    const spinner = btn?.querySelector('.btn-spinner');
    if (!btn) return;
    btn.disabled = loading;
    text?.classList.toggle('hidden', loading);
    spinner?.classList.toggle('hidden', !loading);
}
