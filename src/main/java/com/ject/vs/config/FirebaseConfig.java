package com.ject.vs.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@ConditionalOnExpression("'${firebase.service-account-path:}' != ''")
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(
                ResourceUtils.getFile(serviceAccountPath));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
