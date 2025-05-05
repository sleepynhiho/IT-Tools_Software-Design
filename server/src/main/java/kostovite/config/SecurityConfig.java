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
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public SecurityConfig(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter() {
        return new FirebaseTokenFilter(firebaseAuth, firestore);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // Apply CORS bean
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(firebaseTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow OPTIONS preflight
                        .requestMatchers("/public/**", "/login", "/signup").permitAll()
                        // Allow anonymous access to list/process (checked in service)
                        .requestMatchers(HttpMethod.GET, "/api/plugins", "/api/plugins/**").permitAll() // Broadened GET under /api/plugins
                        .requestMatchers(HttpMethod.POST, "/api/plugins/**").permitAll() // Broadened POST under /api/plugins
                        // Admin ONLY endpoints
                        .requestMatchers(HttpMethod.POST, "/api/plugins/upload").hasRole("ADMIN") // Keep specific admin higher
                        .requestMatchers(HttpMethod.DELETE, "/api/plugins/{pluginName}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/plugins/refresh").hasRole("ADMIN")
                        .requestMatchers("/api/plugins/unload/**").hasRole("ADMIN")
                        .requestMatchers("/api/plugins/universal/unload/**").hasRole("ADMIN")
                        .requestMatchers("/api/debug/**").hasRole("ADMIN")
                        // Other Authenticated endpoints
                        .requestMatchers("/api/secure/**").authenticated()
                        // Fallback for any other /api endpoint
                        .requestMatchers("/api/**").authenticated() // Secure others by default
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = getCorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // Apply config to all /api/** paths
        return source;
    }

    @NotNull
    private static CorsConfiguration getCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers for simplicity during debug - RESTRICT IN PRODUCTION
        // --- FIX: Explicitly allow credentials ---
        configuration.setAllowCredentials(true); // <<<--- ADD THIS LINE
        // -----------------------------------------
        return configuration;
    }
}