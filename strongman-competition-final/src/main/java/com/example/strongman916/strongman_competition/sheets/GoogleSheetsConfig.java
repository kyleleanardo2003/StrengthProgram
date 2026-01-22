package com.example.strongman916.strongman_competition.sheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Configuration
@EnableConfigurationProperties(GoogleSheetsProperties.class)
public class GoogleSheetsConfig {

    private static final String APPLICATION_NAME = "Strongman Sheets Integration";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/spreadsheets.readonly");

    @Bean
    public Sheets sheetsClient(GoogleSheetsProperties props) throws Exception {
        GoogleCredentials credentials = loadCredentials(props).createScoped(SCOPES);

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build();
    }

    private GoogleCredentials loadCredentials(GoogleSheetsProperties props) throws Exception {
        // Prefer an explicit credentials file path if configured (kept OUTSIDE the repo).
        if (props.getCredentialsFile() != null && !props.getCredentialsFile().isBlank()) {
            try (InputStream in = new FileInputStream(props.getCredentialsFile())) {
                return GoogleCredentials.fromStream(in);
            }
        }

        // Default: Application Default Credentials (recommended).
        // Locally: `gcloud auth application-default login`
        // In prod (Cloud Run/VM): attach a service account (no key file).
        return GoogleCredentials.getApplicationDefault();
    }
}
