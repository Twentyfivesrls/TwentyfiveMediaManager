package com.example.twentyfivemediamanager.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.file-security")
public class FileSecurityProperties {

    @NotBlank
    private String storageRoot;

    private long maxUploadSizeBytes = 10 * 1024 * 1024;

    private List<String> allowedContentTypes = new ArrayList<>();

    private List<String> allowedOrigins = new ArrayList<>();

    private List<RootPolicy> rootPolicies = new ArrayList<>();

    public Path storageRootPath() {
        return Path.of(storageRoot).normalize().toAbsolutePath();
    }

    @Getter
    @Setter
    public static class RootPolicy {
        private String root;
        private List<String> origins = new ArrayList<>();
    }
}