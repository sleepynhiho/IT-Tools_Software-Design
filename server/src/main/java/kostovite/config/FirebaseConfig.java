package kostovite.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource; // Use Spring's Resource loading

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    // Inject the path to your service account key JSON file from application.properties
    @Value("classpath:firebase/firebase-service.json") // Example path in resources
    private Resource serviceAccountResource;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) { // Prevent re-initialization
            log.info("Initializing Firebase Application...");

            // Use try-with-resources for InputStream
            try (InputStream serviceAccountStream = serviceAccountResource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        // Optional: Add databaseURL if using Realtime Database
                        // .setDatabaseUrl("https://<YOUR_PROJECT_ID>.firebaseio.com")
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase Application Initialized: {}", app.getName());
                return app;
            } catch (IOException e) {
                log.error("Failed to initialize Firebase Application", e);
                throw e; // Re-throw to prevent application startup if Firebase fails
            }
        } else {
            log.warn("Firebase Application already initialized.");
            return FirebaseApp.getInstance(); // Return existing instance
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        // Get FirebaseAuth instance from the initialized app
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
