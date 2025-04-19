// src/utils/apiClient.ts
import { auth } from '../firebaseConfig'; // Assuming your auth instance is exported here

// Function to get the current ID token
const getCurrentIdToken = async (): Promise<string | null> => {
    const currentUser = auth.currentUser;
    if (currentUser) {
        try {
            // Consider forceRefresh based on your token expiration strategy
            // const token = await currentUser.getIdToken(/* forceRefresh */ false);
            const token = await currentUser.getIdToken();
            return token;
        } catch (error) {
            console.error("Error getting ID token:", error);
            // Handle error, maybe sign out user or trigger re-login
            return null;
        }
    }
    return null;
};

// Base API URL from environment variable
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8081"; // Fallback for safety

interface RequestOptions extends RequestInit {
    useAuth?: boolean; // Flag to indicate if auth header should be added
    body?: any; // Allow any body type initially
}

const apiClient = async <T = any>(
    endpoint: string,
    options: RequestOptions = {}
): Promise<T> => {
    const { useAuth = true, headers: customHeaders, body, ...restOptions } = options;

    const defaultHeaders: HeadersInit = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...customHeaders,
    };

    let token: string | null = null;
    if (useAuth) {
        token = await getCurrentIdToken();
        if (token) {
            defaultHeaders['Authorization'] = `Bearer ${token}`;
        } else if (useAuth) {
            // If auth is required but no token found, throw an error early
            // Or handle redirection to login
             console.warn(`Auth required for ${endpoint}, but no token found.`);
             // Depending on strategy, you might throw or let the backend return 401
             // throw new Error("User not authenticated.");
        }
    }

    const config: RequestInit = {
        ...restOptions,
        headers: defaultHeaders,
    };

    // Stringify body if it's an object and Content-Type is JSON
    if (body && typeof body === 'object' && defaultHeaders['Content-Type'] === 'application/json') {
        config.body = JSON.stringify(body);
    } else if (body) {
        config.body = body; // Use body as is (e.g., FormData)
    }


    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
    console.debug(`API Request: ${config.method || 'GET'} ${url}`, config.useAuth ? ' (Auth)' : ''); // Added logging


    try {
        const response = await fetch(url, config);

        // Special handling for 204 No Content
        if (response.status === 204) {
            return Promise.resolve(undefined as T); // Or return null, depending on expected type
        }

        // Attempt to parse JSON by default, handle non-JSON responses gracefully
        let responseData: any;
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
             responseData = await response.json();
        } else {
             responseData = await response.text(); // Fallback to text
        }


        if (!response.ok) {
            // Throw an error object that includes status and potential body
            const error: any = new Error(
                `API request failed: ${response.status} ${response.statusText}`
            );
            error.status = response.status;
            error.response = response; // Attach full response
            error.data = responseData; // Attach parsed data (JSON or text)
            console.error(`API Error Response (${url}):`, error.data);
            throw error;
        }

        return responseData as T;

    } catch (error) {
        console.error(`API Fetch Error (${url}):`, error);
         // Re-throw the error so callers can handle it
        throw error;
    }
};

// Convenience methods
export const api = {
    get: <T = any>(endpoint: string, options: Omit<RequestOptions, 'method' | 'body'> = {}) =>
        apiClient<T>(endpoint, { ...options, method: 'GET' }),
    post: <T = any>(endpoint: string, body: any, options: Omit<RequestOptions, 'method' | 'body'> = {}) =>
        apiClient<T>(endpoint, { ...options, method: 'POST', body }),
    put: <T = any>(endpoint: string, body: any, options: Omit<RequestOptions, 'method' | 'body'> = {}) =>
        apiClient<T>(endpoint, { ...options, method: 'PUT', body }),
    delete: <T = any>(endpoint: string, options: Omit<RequestOptions, 'method' | 'body'> = {}) =>
        apiClient<T>(endpoint, { ...options, method: 'DELETE' }),
     patch: <T = any>(endpoint: string, body: any, options: Omit<RequestOptions, 'method' | 'body'> = {}) =>
        apiClient<T>(endpoint, { ...options, method: 'PATCH', body }),
};

export default api;