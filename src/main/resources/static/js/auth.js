// Authentication & Session Management
const auth = {
    getUser() {
        try {
            return JSON.parse(localStorage.getItem('currentUser'));
        } catch (e) {
            return null;
        }
    },

    getRole() {
        return localStorage.getItem('userRole') || '';
    },

    isAdmin() {
        return this.getRole() === 'ROLE_ADMIN';
    },

    isHR() {
        return this.getRole() === 'ROLE_HR';
    },

    isAdminOrHR() {
        return this.isAdmin() || this.isHR();
    },

    isManager() {
        return this.getRole() === 'ROLE_MANAGER';
    },

    isEmployee() {
        return this.getRole() === 'ROLE_EMPLOYEE';
    },

    async checkSession() {
        try {
            const res = await api.get('/api/auth/current-user');
            if (res && res.success && res.data) {
                localStorage.setItem('currentUser', JSON.stringify(res.data));
                localStorage.setItem('userRole', res.data.role);
                return res.data;
            }
        } catch (e) {
            console.warn('Session verification failed, redirecting to login:', e);
            window.location.href = '/index.html';
        }
        return null;
    },

    async logout() {
        try {
            await fetch('/api/auth/logout', { method: 'POST' });
        } catch (e) {
            console.error('Logout error:', e);
        } finally {
            localStorage.clear();
            window.location.href = '/index.html';
        }
    }
};