// src/utils/fetchWithAuth.ts (assuming this is the path)

import { auth } from '../firebaseConfig'; // Correct: for getting current user's token by default

/**
 * Utility function to fetch with authentication
 * @param url The URL to fetch
 * @param options Fetch options
 * @param getIdTokenOverride Optional function to get a custom ID token (renamed for clarity)
 * @returns Promise with the fetch response
 */
export const fetchWithAuth = async (
  url: string,
  options: RequestInit = {},
  // Renamed parameter for clarity: this is an override for the default token fetching
  getIdTokenOverride?: () => Promise<string | null> // Changed to allow null for consistency with useAuth
) => {
  try {
    let token: string | null | undefined; // Allow null

    // Get token:
    // 1. Use the override function if provided
    // 2. Else, try to get from the current Firebase user
    if (getIdTokenOverride) {
      console.log("[fetchWithAuth] Using provided getIdTokenOverride function.");
      token = await getIdTokenOverride();
    } else if (auth.currentUser) {
      console.log("[fetchWithAuth] Attempting to get token from auth.currentUser.");
      try {
        token = await auth.currentUser.getIdToken(true); // forceRefresh = true
      } catch (tokenError) {
        console.error("[fetchWithAuth] Error getting token from auth.currentUser:", tokenError);
        // Decide how to handle: throw, return null, or proceed without token?
        // For now, let it proceed without a token if this fails,
        // but the server will likely reject if auth is required.
        token = null;
      }
    } else {
      console.log("[fetchWithAuth] No getIdTokenOverride and no auth.currentUser. Proceeding without token.");
    }

    if (token) {
        console.log("[fetchWithAuth] Token acquired.");
    } else {
        console.warn("[fetchWithAuth] No token available for the request.");
    }

    // Build headers
    const headers = new Headers(options.headers || {}); // Use Headers object for easier manipulation

    // Set Content-Type if not already set and body is not FormData
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    // Set Accept if not already set
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }

    // Add Authorization header if token exists
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    // Perform the fetch call
    // Ensure the URL is complete (e.g., includes API base if url is relative)
    // const completeUrl = url.startsWith('http') ? url : `${YOUR_API_BASE_URL_IF_NEEDED}${url}`;
    // For now, assuming 'url' is already the complete URL or a relative path handled by dev server proxy

    console.log(`[fetchWithAuth] Fetching URL: ${url}, Method: ${options.method || 'GET'}`);
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include', // Correct for sending cookies/auth headers with CORS
      mode: 'cors',         // Correct for cross-origin requests
    });
    console.log(`[fetchWithAuth] Response Status for ${url}: ${response.status}`);
    return response;

  } catch (error) {
    // This catch block will mainly catch network errors or errors thrown *before* the fetch call
    console.error('[fetchWithAuth] Unhandled error during fetch setup or network error:', error);
    throw error; // Re-throw the error to be handled by the caller
  }
};

// Typically, you might not need a default export if you're exporting named functions.
// But if you prefer it for this utility:
export default fetchWithAuth;