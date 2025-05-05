// Create this new utility file if it doesn't exist

import { auth } from '../firebaseConfig';

/**
 * Utility function to fetch with authentication
 * @param url The URL to fetch
 * @param options Fetch options
 * @param getIdToken Optional function to get a custom ID token
 * @returns Promise with the fetch response
 */
export const fetchWithAuth = async (
  url: string,
  options: RequestInit = {},
  getIdToken?: () => Promise<string>
) => {
  try {
    // Get token either from provided function or from Firebase auth
    let token: string | undefined;
    if (getIdToken) {
      token = await getIdToken();
    } else if (auth.currentUser) {
      token = await auth.currentUser.getIdToken(true);
    }

    // Build headers with token if available
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...options.headers,
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };

    // Return the fetch call with credentials included
    return fetch(url, {
      ...options,
      headers,
      credentials: 'include',
      mode: 'cors'
    });
  } catch (error) {
    console.error('Error in fetchWithAuth:', error);
    throw error;
  }
};

export default fetchWithAuth;