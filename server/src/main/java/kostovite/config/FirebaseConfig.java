// src/main/java/kostovite/config/FirebaseConfig.java
package kostovite.config;

import com.google.auth.oauth2.GoogleCredentials;
// *** Import Firestore and FirestoreClient ***
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
// *****************************************
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("classpath:firebase/firebase-service.json")
    private Resource serviceAccountResource;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Initializing Firebase Application...");
            try (InputStream serviceAccountStream = serviceAccountResource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        // Project ID is usually inferred from credentials, but uncomment if needed
                        // .setProjectId("your-project-id")
                        .build();
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase Application Initialized: {}", app.getName());
                return app;
            } catch (IOException e) {
                log.error("Failed to initialize Firebase Application", e);
                throw e;
            }
        } else {
            log.warn("Firebase Application already initialized.");
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    // --- ADD Firestore Bean ---
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        // Get the Firestore instance associated with the initialized FirebaseApp
        // This ensures it uses the same project and credentials
        Firestore db = FirestoreClient.getFirestore(firebaseApp);
        log.info("Providing Firestore instance associated with FirebaseApp: {}", firebaseApp.getName());
        return db;
    }
    // --------------------------
}