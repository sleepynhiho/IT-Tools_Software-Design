// src/main/java/kostovite/config/FirebaseTokenFilter.java
package kostovite.config;

// Keep Firestore imports
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
// Remove FirestoreOptions import

// Keep other imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore; // Keep the field

    // Constructor accepts injected Firestore
    public FirebaseTokenFilter(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        if (this.firestore != null) {
            log.info("FirebaseTokenFilter initialized with injected Firestore instance.");
        } else {
            log.error("FirebaseTokenFilter initialized WITHOUT an injected Firestore instance!");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idToken = extractToken(request);

        if (idToken != null) {
            if (this.firestore == null) {
                handleGenericException(response, "Firestore service not available in filter", new IllegalStateException("Firestore instance is null"));
                return;
            }
            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                log.debug("Firebase token verified for UID: {}", uid);

                // --- Fetch User Type from Firestore ---
                String userType = fetchUserTypeFromFirestore(uid);
                log.debug("Fetched user type for UID {}: {}", uid, userType);

                // Convert userType to Spring Security Authority (e.g., "ROLE_PREMIUM")
                List<GrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase())
                );
                // -------------------------------------

                UserDetails userDetails = new User(uid, "", authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set SecurityContext for user: {} with authorities: {}", uid, authorities);

            } catch (FirebaseAuthException e) {
                handleAuthException(response, "Invalid Firebase token", e); return;
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error fetching user data from Firestore for token verification", e);
                handleAuthException(response, "Error fetching user data", e);
                if (e instanceof InterruptedException) { Thread.currentThread().interrupt(); } return;
            } catch (Exception e) {
                handleGenericException(response, "Token verification failed", e); return;
            }
        } else {
            log.trace("No Firebase token found in request header.");
        }
        filterChain.doFilter(request, response);
    }

    // Helper method uses the injected 'this.firestore'
    private String fetchUserTypeFromFirestore(String uid) throws ExecutionException, InterruptedException {
        if (this.firestore == null) {
            log.error("Attempted to fetch user type, but Firestore instance is null for UID: {}", uid);
            return "normal"; // Default on config error
        }
        DocumentSnapshot document = this.firestore.collection("users").document(uid).get().get(); // Blocking get
        if (document.exists()) {
            String type = document.getString("userType"); // *** USE YOUR EXACT FIELD NAME ***
            if (type != null && !type.isEmpty()) {
                if ("normal".equalsIgnoreCase(type) || "premium".equalsIgnoreCase(type) || "admin".equalsIgnoreCase(type)) {
                    return type.toLowerCase();
                } else {
                    log.warn("Invalid userType '{}' found for user {}. Defaulting to 'normal'.", type, uid); return "normal";
                }
            } else {
                log.warn("userType field missing or empty for user {}. Defaulting to 'normal'.", uid); return "normal";
            }
        } else {
            log.warn("User document not found in Firestore for UID {}. Defaulting to 'normal'.", uid); return "normal";
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) { return bearerToken.substring(7); }
        return null;
    }
    private void handleAuthException(HttpServletResponse response, String errorSummary, Exception e) throws IOException {
        log.error("Authentication Exception: {} - {}", errorSummary, e.getMessage());
        SecurityContextHolder.clearContext(); response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType("application/json"); response.getWriter().write(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", errorSummary, e.getMessage().replace("\"", "\\\"")));
    }
    private void handleGenericException(HttpServletResponse response, String errorSummary, Exception e) throws IOException {
        log.error("Unexpected error during token processing: {} - {}", errorSummary, e.getMessage());
        SecurityContextHolder.clearContext(); response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); response.setContentType("application/json"); response.getWriter().write(String.format("{\"error\": \"%s\"}", errorSummary));
    }
}