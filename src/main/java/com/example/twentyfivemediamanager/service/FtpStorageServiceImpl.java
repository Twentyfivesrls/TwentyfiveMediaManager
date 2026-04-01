package com.example.twentyfivemediamanager.service;

import com.example.twentyfivemediamanager.config.FileSecurityProperties;
import com.example.twentyfivemediamanager.security.SecurePathResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class FtpStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;
    private final SecurePathResolver securePathResolver;
    private final FileSecurityProperties fileSecurityProperties;

    public FtpStorageServiceImpl(
            @Value("${file.storage.location}") String storageLocation,
            SecurePathResolver securePathResolver,
            FileSecurityProperties fileSecurityProperties
    ) {
        this.rootLocation = Paths.get(storageLocation).normalize().toAbsolutePath();
        this.securePathResolver = securePathResolver;
        this.fileSecurityProperties = fileSecurityProperties;
    }

    @Override
    public List<String> getFiles(String[] directories) throws IOException {
        Path folderPath = resolveDirectoryPath(directories);
        List<String> fileList = new ArrayList<>();

        if (!Files.exists(folderPath)) {
            return fileList;
        }

        if (!Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Requested path is not a directory");
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    fileList.add(entry.getFileName().toString());
                }
            }
        }

        return fileList;
    }

    @Override
    public String storeFile(String[] directory, MultipartFile file, String strategy) throws IOException {
        validateFile(file);

        Path directoryPath = resolveDirectoryPath(directory);
        Files.createDirectories(directoryPath);

        String safeFileName = cleanFileName(Objects.requireNonNull(file.getOriginalFilename(), "Original filename is required"));
        String normalizedStrategy = normalizeStrategy(strategy);

        Path initialDestination = directoryPath.resolve(safeFileName).normalize().toAbsolutePath();
        if (!initialDestination.startsWith(directoryPath)) {
            throw new SecurityException("Invalid destination path");
        }

        Path destinationFile = initialDestination;

        if (Files.exists(initialDestination)) {
            switch (normalizedStrategy) {
                case "APPEND" -> destinationFile = resolveAppendFileName(directoryPath, safeFileName);
                case "REPLACE" -> destinationFile = initialDestination;
                case "DISCARD" -> {
                    log.info("Discard strategy applied. Existing file kept. path={}", initialDestination);
                    return initialDestination.getFileName().toString();
                }
                default -> throw new IllegalArgumentException("Invalid strategy: " + normalizedStrategy);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved upload directory path. directorySegments={}, directoryPath={}",
                    java.util.Arrays.toString(directory), directoryPath);
        }

        log.info("Storing file. directory={}, filename={}, strategy={}",
                directoryPath, destinationFile.getFileName(), normalizedStrategy);

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return destinationFile.getFileName().toString();
    }

    @Override
    public void deleteFile(String[] directory) throws IOException {
        Path filePath = resolveFilePath(directory);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found");
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Requested path is not a file");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved file path for deletion. pathSegments={}, filePath={}",
                    java.util.Arrays.toString(directory), filePath);
        }

        Files.delete(filePath);
        log.info("Deleted file. path={}", filePath);
    }

    @Override
    public Resource loadFileAsResource(String path) throws IOException {
        Path filePath = securePathResolver.resolveRelativePath(path);

        if (!filePath.startsWith(rootLocation)) {
            throw new SecurityException("Resolved path is outside storage root");
        }

        log.info("Loading file resource. path={}", filePath);

        if (!Files.exists(filePath) || !Files.isReadable(filePath) || !Files.isRegularFile(filePath)) {
            throw new FileNotFoundException("Could not read file");
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Could not read file");
        }

        return resource;
    }

    private Path resolveDirectoryPath(String[] directorySegments) {
        Path directoryPath = securePathResolver.resolveRelativePath(directorySegments);

        if (!directoryPath.startsWith(rootLocation)) {
            throw new SecurityException("Resolved path is outside storage root");
        }

        return directoryPath;
    }

    private Path resolveFilePath(String[] pathSegments) {
        Path filePath = securePathResolver.resolveRelativePath(pathSegments);

        if (!filePath.startsWith(rootLocation)) {
            throw new SecurityException("Resolved path is outside storage root");
        }

        return filePath;
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            log.warn("Upload validation failed: file is null");
            throw new IllegalArgumentException("File is required");
        }

        if (file.isEmpty()) {
            log.warn("Upload validation failed: file is empty");
            throw new IllegalArgumentException("File is empty");
        }

        if (log.isDebugEnabled()) {
            log.debug("Validating uploaded file. originalFilename={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }

        if (file.getSize() > fileSecurityProperties.getMaxUploadSizeBytes()) {
            log.warn("Upload validation failed: file too large. filename={}, size={}, maxAllowed={}",
                    file.getOriginalFilename(), file.getSize(), fileSecurityProperties.getMaxUploadSizeBytes());
            throw new IllegalArgumentException("File too large");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            log.warn("Upload validation failed: missing content type. filename={}", file.getOriginalFilename());
            throw new IllegalArgumentException("Unsupported content type");
        }

        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        boolean allowed = fileSecurityProperties.getAllowedContentTypes().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(normalizedContentType));

        if (!allowed) {
            log.warn("Upload validation failed: unsupported content type. filename={}, contentType={}, allowedTypes={}",
                    file.getOriginalFilename(), normalizedContentType, fileSecurityProperties.getAllowedContentTypes());
            throw new IllegalArgumentException("Unsupported content type");
        }

        log.debug("Upload validation passed. filename={}, contentType={}, size={}",
                file.getOriginalFilename(), normalizedContentType, file.getSize());
    }

    private String cleanFileName(String originalFilename) {
        String cleaned = Paths.get(originalFilename).getFileName().toString().trim();

        if (cleaned.isBlank() || ".".equals(cleaned) || "..".equals(cleaned)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        return cleaned;
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return "APPEND";
        }
        return strategy.trim().toUpperCase(Locale.ROOT);
    }

    private Path resolveAppendFileName(Path directoryPath, String safeFileName) {
        String baseName = getBaseName(safeFileName);
        String extension = getExtension(safeFileName);

        int index = 1;
        Path candidate;

        do {
            String candidateName = extension.isBlank()
                    ? baseName + "_" + index
                    : baseName + "_" + index + "." + extension;

            candidate = directoryPath.resolve(candidateName).normalize().toAbsolutePath();

            if (!candidate.startsWith(directoryPath)) {
                throw new SecurityException("Invalid destination path");
            }

            index++;
        } while (Files.exists(candidate));

        return candidate;
    }

    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}