package kostovite.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal; // Another way to get user info

@RestController
@RequestMapping("/api/secure") // Base path for secured endpoints
public class SecureController {

    private static final Logger log = LoggerFactory.getLogger(SecureController.class);

    @GetMapping("/data")
    public ResponseEntity<String> getSecureData(Authentication authentication, Principal principal) {

        // Option 1: Get from Authentication object (injected)
        String uidFromAuth = "N/A";
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            uidFromAuth = userDetails.getUsername(); // We set UID as username in the filter
            log.info("Accessing secure data via Authentication for UID: {}", uidFromAuth);
        }

        // Option 2: Get from Principal (injected)
        String uidFromPrincipal = (principal != null) ? principal.getName() : "N/A";
        log.info("Accessing secure data via Principal for UID: {}", uidFromPrincipal);


        // Option 3: Get directly from SecurityContextHolder (less preferred in controllers)
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        String uidFromContext = "N/A";
        if (contextAuth != null && contextAuth.getPrincipal() instanceof UserDetails) {
            uidFromContext = ((UserDetails) contextAuth.getPrincipal()).getUsername();
        }

        // Use the UID to fetch user-specific data from your database, etc.
        return ResponseEntity.ok("Hello user UID: " + uidFromAuth + "! This is secure data.");
    }
}