package kostovite.config; // Use your actual package name

import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import HttpMethod if needed for specific method rules
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Bean
    public FirebaseTokenFilter firebaseTokenFilter() {
        // Assuming FirebaseTokenFilter only tries to verify if a token exists
        // and doesn't block requests without a token.
        return new FirebaseTokenFilter(firebaseAuth);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 1. Public Endpoints
                        .requestMatchers("/api/public/**", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/plugins/universal/manual-load").permitAll() // List is public
                        .requestMatchers(HttpMethod.POST, "/api/debug/*/process").permitAll()

                        // *** ADD THIS RULE FOR INDIVIDUAL METADATA ***
                        .requestMatchers(HttpMethod.GET, "/api/plugins/universal/*/metadata").permitAll() // Allow GET metadata for any plugin

                        // 2. Endpoints Accessible by BOTH Anonymous and Authenticated (Examples)
                        .requestMatchers(HttpMethod.GET, "/api/tools/**").permitAll() // Example path
                        // Remove this if covered by the rule above: .requestMatchers("/api/plugins/metadata").permitAll()

                        // 3. Endpoints Requiring Authentication
                        .requestMatchers("/api/secure/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tools/favorites").authenticated()
                        .requestMatchers("/api/users/profile").authenticated()
//                        .requestMatchers(HttpMethod.POST, "/api/plugins/universal/*/process").authenticated() // Processing requires auth

                        // 4. Default Authenticated (Fallback)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Make sure your frontend origin(s) are listed
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept")); // Added Accept just in case
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}