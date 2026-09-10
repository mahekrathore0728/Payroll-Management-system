// Centralized REST API client
const api = {
    async request(url, options = {}) {
        const defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };

        options.headers = { ...defaultHeaders, ...options.headers };

        try {
            const response = await fetch(url, options);

            if (response.status === 401) {
                localStorage.removeItem('currentUser');
                localStorage.removeItem('userRole');
                window.location.href = '/index.html';
                return null;
            }

            // Check if response is PDF or binary
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/pdf')) {
                return response.blob();
            }

            const data = await response.json();

            if (!response.ok) {
                const errorMsg = data.message || (data.data ? JSON.stringify(data.data) : 'An error occurred');
                throw new Error(errorMsg);
            }

            return data;
        } catch (error) {
            console.error('API Request Failed:', error);
            throw error;
        }
    },

    get(url) {
        return this.request(url, { method: 'GET' });
    },

    post(url, body) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    put(url, body) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    delete(url) {
        return this.request(url, { method: 'DELETE' });
    }
};

// Toast notification utility
function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : (type === 'warning' ? 'warning text-dark' : 'success')} border-0 show shadow-lg mb-2`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    const icon = type === 'error' ? 'bi-x-circle-fill' : (type === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill');

    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body d-flex align-items-center gap-2">
                <i class="bi ${icon} fs-5"></i>
                <div>${message}</div>
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close" onclick="this.closest('.toast').remove()"></button>
        </div>
    `;

    toastContainer.appendChild(toastEl);
    setTimeout(() => {
        toastEl.remove();
    }, 4500);
}