package kostovite.config;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User; // Spring Security User
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter; // Ensures filter runs once per request

import java.io.IOException;
import java.util.ArrayList; // For authorities list

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idToken = extractToken(request);

        if (idToken != null) {
            try {
                // Verify the token using Firebase Admin SDK
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail(); // Or other claims you need

                log.debug("Firebase token verified for UID: {}", uid);

                // Create Spring Security UserDetails (customize authorities as needed)
                // You might fetch roles/permissions from your own database based on the UID here
                UserDetails userDetails = new User(uid, "", new ArrayList<>()); // Using UID as username, no password needed

                // Create Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Set authentication in Spring Security Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set SecurityContext for user: {}", uid);

            } catch (FirebaseAuthException e) {
                log.error("Firebase Authentication Exception: {}", e.getMessage());
                // Clear context if token is invalid
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Send 401 Unauthorized
                response.getWriter().write("{\"error\": \"Invalid Firebase token\", \"message\": \"" + e.getMessage() + "\"}");
                response.setContentType("application/json");
                return; // Stop filter chain execution
            } catch (Exception e) {
                log.error("Unexpected error during token verification: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Token verification failed\"}");
                response.setContentType("application/json");
                return;
            }
        } else {
            log.trace("No Firebase token found in request header.");
            // No token found, proceed without setting authentication (let Spring Security handle authorization rules)
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}