// src/main/java/kostovite/config/SecurityConfig.java
package kostovite.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.cloud.firestore.Firestore;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;
import java.util.Arrays;
import java.util.List; // Import List

@Configuration
@EnableWebSecurity
@EnableMethodSecurity() // Keep this enabled if you use @PreAuthorize elsewhere
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public SecurityConfig(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter() {
        // Initialize filter even if some endpoints are permitAll,
        // as it populates SecurityContext if a token *is* present,
        // which can be useful for logging or optional logic in controllers.
        return new FirebaseTokenFilter(firebaseAuth, firestore);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // Apply CORS bean
                .csrf(AbstractHttpConfigurer::disable) // Common for stateless APIs
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless session
                // Add filter BEFORE standard auth filters to process Firebase token
                .addFilterBefore(firebaseTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        // --- Pre-flight/Public ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // MUST allow OPTIONS preflight requests
                        .requestMatchers("/public/**", "/login", "/signup").permitAll() // Example public endpoints

                        // --- Plugin Discovery/Listing/Processing (Currently Open) ---
                        // Allow anyone to see/get plugin info (access controlled within endpoint if needed)
                        .requestMatchers(HttpMethod.GET, "/api/plugins", "/api/plugins/**").permitAll()
                        // Allow anyone to attempt processing (access controlled within endpoint if needed)
                        .requestMatchers(HttpMethod.POST, "/api/plugins/**").permitAll()

                        // --- CHANGE: Allow Debug Endpoints ---
                        .requestMatchers("/api/debug/**").permitAll() // <<< CHANGED from .hasRole("ADMIN")

                        // --- Admin ONLY Management Endpoints (Keep Restricted) ---
                        .requestMatchers(HttpMethod.POST, "/api/plugins/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/plugins/{pluginName}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/plugins/refresh").hasRole("ADMIN")
                        .requestMatchers("/api/plugins/unload/**").hasRole("ADMIN") // Assuming pattern covers specific names
                        .requestMatchers("/api/plugins/universal/unload/**").hasRole("ADMIN") // Assuming pattern covers specific names

                        // --- Other Specific Endpoints ---
                        .requestMatchers("/api/secure/**").authenticated() // Example: requires any logged-in user

                        // --- Fallback Rule for Unmatched /api/** ---
                        // Keep this if you want any other future /api endpoints to require login by default
                        .requestMatchers("/api/**").authenticated()

                        // --- Allow all other requests (e.g., serving frontend static files) ---
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = getCorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // Apply CORS to all API paths
        // You might register other paths if needed (e.g., "/" for static files if served by backend)
        return source;
    }

    @NotNull
    private static CorsConfiguration getCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Specify allowed origins explicitly - '*' is generally discouraged with credentials=true
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173")); // Your frontend URLs
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")); // Common methods
        // Allow specific headers needed by your frontend + standard ones + Authorization
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept", "Origin")); // Example common headers
        // configuration.setAllowedHeaders(List.of("*")); // Use '*' carefully with credentials
        configuration.setAllowCredentials(true); // Crucial for sending/receiving auth tokens/cookies
        // You might want to configure exposed headers if frontend needs to read custom ones
        // configuration.setExposedHeaders(Arrays.asList("header1", "header2"));
        // Optional: Set max age for preflight response caching
        // configuration.setMaxAge(3600L);
        return configuration;
    }
}