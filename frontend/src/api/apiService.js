import axios from 'axios';

const apiClient = axios.create({
    baseURL: process.env.VUE_APP_API_URL,
    withCredentials: false,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('accessToken')
    }
});

apiClient.interceptors.response.use(undefined, async error => {
    if (error.config && error.response) {
        if (error.response.status === 401) {
            if (error.config.url === '/api/getRefreshToken') {
                return Promise.reject(error);
            }

            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
                try {
                    const response = await axios.get('/api/getRefreshToken', {
                        params: { refreshToken },
                        headers: { 'X-Refresh-Token-Request': true }
                    }).catch(error => {
                        if (error.response && error.response.status === 401) {
                            localStorage.removeItem('accessToken');
                            localStorage.removeItem('refreshToken');
                            location.reload();
                        }
                    });

                    const newAccessToken = response.data.access_token;
                    const newRefreshToken = response.data.refresh_token;
                    localStorage.setItem('accessToken', newAccessToken);
                    localStorage.setItem('refreshToken', newRefreshToken);

                    error.config.headers['Authorization'] = newAccessToken;

                    return apiClient.request(error.config);
                } catch (refreshError) {
                    console.error(refreshError);
                }
            }
        } else if (error.response.status === 429) {
            alert('Too many requests! Please try again later.');
        }
    }

    return Promise.reject(error);
});

export default {
    get(endpoint) {
        return apiClient.get(endpoint);
    },
    post(endpoint, body) {
        return apiClient.post(endpoint, body);
    },
};