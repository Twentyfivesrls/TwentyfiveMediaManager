package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.exceptions.FileDeleteException;
import com.example.twentyfivemediamanager.exceptions.FileDownloadException;
import com.example.twentyfivemediamanager.exceptions.FileOperationException;
import com.example.twentyfivemediamanager.exceptions.FileUploadException;
import com.example.twentyfivemediamanager.security.PathOperationValidator;
import com.example.twentyfivemediamanager.security.SecurePathResolver;
import com.example.twentyfivemediamanager.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/twentyfiveserver")
public class FileController {

    private static final String DOWNLOAD_MARKER = "/downloadkkk/";
    private static final String UPLOAD_MARKER = "/uploadkkk/";
    private static final String DELETE_MARKER = "/deletekkk/";
    private static final String INFO_MARKER = "/infokkk/";

    private final FileStorageService fileStorageService;
    private final SecurePathResolver securePathResolver;
    private final PathOperationValidator pathOperationValidator;

    @GetMapping("/downloadkkk/{path}/**")
    public ResponseEntity<Resource> downloadFile(@PathVariable String path, HttpServletRequest request) throws FileNotFoundException {
        try {
            String relativePath = extractRelativePathFromUri(request, DOWNLOAD_MARKER);
            String finalFileName = extractFileName(relativePath);

            Resource resource = fileStorageService.loadFileAsResource(relativePath);
            MediaType mediaType = MediaTypeFactory.getMediaType(finalFileName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            log.info("File download accepted. uri={}, filename={}", request.getRequestURI(), finalFileName);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(finalFileName).build().toString()
                    )
                    .body(resource);

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileDownloadException("Failed to download file: " + path, ex);
        } catch (Exception ex) {
            throw new FileDownloadException("Unexpected error occurred while downloading file: " + path, ex);
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/uploadkkk/{path}/**")
    public ResponseEntity<String> uploadFile(@PathVariable String path,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(name = "strategy", defaultValue = "APPEND", required = false) String strategy,
                                             HttpServletRequest request) throws FileNotFoundException {
        try {
            String relativePath = extractRelativePathFromUri(request, UPLOAD_MARKER);
            String[] pathSegments = splitRelativePath(relativePath);

            String storedFileName = fileStorageService.storeFile(pathSegments, file, strategy);

            log.info("File upload accepted. uri={}, filename={}, strategy={}",
                    request.getRequestURI(), storedFileName, strategy);

            return ResponseEntity.ok(storedFileName);

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileUploadException("Failed to upload file: " + safeOriginalFilename(file), ex);
        } catch (Exception ex) {
            throw new FileUploadException("Unexpected error occurred while uploading file: " + safeOriginalFilename(file), ex);
        }
    }

    @DeleteMapping("/delete-folderkkk")
    public ResponseEntity<String> deleteFolder(@RequestParam("target") String target) throws FileNotFoundException {
        try {
            String normalizedTarget = normalizeInputPath(target);
            Path resolvedTarget = securePathResolver.resolveRelativePath(normalizedTarget);

            String root = pathOperationValidator.extractRoot(normalizedTarget);
            if (!StringUtils.hasText(root)) {
                throw new SecurityException("Invalid target path");
            }

            if (isRootDeletion(normalizedTarget, root)) {
                throw new SecurityException("Deleting a root folder is not allowed");
            }

            if (!Files.exists(resolvedTarget)) {
                throw new FileNotFoundException("Folder not found");
            }

            if (!Files.isDirectory(resolvedTarget)) {
                throw new IllegalArgumentException("Target is not a directory");
            }

            deleteDirectoryRecursively(resolvedTarget);

            log.info("Folder deleted successfully. target={}", normalizedTarget);
            return ResponseEntity.ok("Folder deleted successfully");

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileDeleteException("Failed to delete folder: " + target, ex);
        } catch (Exception ex) {
            throw new FileDeleteException("Unexpected error occurred while deleting folder: " + target, ex);
        }
    }

    @GetMapping("/copykkk")
    public ResponseEntity<String> copyFile(@RequestParam("source") String source,
                                           @RequestParam("target") String target) throws FileNotFoundException {
        try {
            String normalizedSource = normalizeInputPath(source);
            String normalizedTarget = normalizeInputPath(target);

            pathOperationValidator.validateSameAuthorizedRoot(normalizedSource, normalizedTarget);

            Path sourcePath = securePathResolver.resolveRelativePath(normalizedSource);
            Path targetPath = securePathResolver.resolveRelativePath(normalizedTarget);

            if (!Files.exists(sourcePath)) {
                throw new FileNotFoundException("Source file not found");
            }

            if (!Files.isRegularFile(sourcePath)) {
                throw new IllegalArgumentException("Source path is not a file");
            }

            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File copied successfully. source={}, target={}", normalizedSource, normalizedTarget);
            return ResponseEntity.ok("File copied successfully");

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileOperationException("Failed to copy file", ex);
        } catch (Exception ex) {
            throw new FileOperationException("Unexpected error occurred while copying file", ex);
        }
    }

    @GetMapping("/infokkk/{path}/**")
    public ResponseEntity<List<String>> getInfo(@PathVariable String path, HttpServletRequest request) throws FileNotFoundException {
        try {
            String relativePath = extractRelativePathFromUri(request, INFO_MARKER);
            String[] pathSegments = splitRelativePath(relativePath);

            List<String> files = fileStorageService.getFiles(pathSegments);

            log.info("Directory info retrieved. uri={}", request.getRequestURI());
            return ResponseEntity.ok(files);

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileOperationException("Failed to retrieve directory info", ex);
        } catch (Exception ex) {
            throw new FileOperationException("Unexpected error occurred while retrieving directory info", ex);
        }
    }

    @GetMapping("/renamekkk")
    public ResponseEntity<String> renameFile(@RequestParam("source") String source,
                                             @RequestParam("target") String target) throws FileNotFoundException {
        try {
            String normalizedSource = normalizeInputPath(source);
            String normalizedTarget = normalizeInputPath(target);

            pathOperationValidator.validateSameAuthorizedRoot(normalizedSource, normalizedTarget);

            Path sourcePath = securePathResolver.resolveRelativePath(normalizedSource);
            Path targetPath = securePathResolver.resolveRelativePath(normalizedTarget);

            if (!Files.exists(sourcePath)) {
                throw new FileNotFoundException("Source file not found");
            }

            if (!Files.isRegularFile(sourcePath)) {
                throw new IllegalArgumentException("Source path is not a file");
            }

            Files.createDirectories(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File renamed successfully. source={}, target={}", normalizedSource, normalizedTarget);
            return ResponseEntity.ok("File renamed successfully");

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileOperationException("Failed to rename file", ex);
        } catch (Exception ex) {
            throw new FileOperationException("Unexpected error occurred while renaming file", ex);
        }
    }

    @DeleteMapping("/deletekkk/{path}/**")
    public ResponseEntity<String> deleteFile(@PathVariable String path, HttpServletRequest request) throws FileNotFoundException {
        try {
            String relativePath = extractRelativePathFromUri(request, DELETE_MARKER);
            String[] pathSegments = splitRelativePath(relativePath);

            fileStorageService.deleteFile(pathSegments);

            log.info("File deleted successfully. uri={}", request.getRequestURI());
            return ResponseEntity.ok("File deleted successfully");

        } catch (FileNotFoundException | SecurityException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FileDeleteException("Error deleting file: " + path, ex);
        } catch (Exception ex) {
            throw new FileDeleteException("Unexpected error occurred while deleting file: " + path, ex);
        }
    }

    private String extractRelativePathFromUri(HttpServletRequest request, String marker) {
        String requestUri = request.getRequestURI();
        int markerIndex = requestUri.indexOf(marker);

        if (markerIndex < 0) {
            throw new IllegalArgumentException("Invalid request URI");
        }

        String relativePath = requestUri.substring(markerIndex + marker.length());
        relativePath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
        relativePath = normalizeInputPath(relativePath);

        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("Missing relative path");
        }

        return relativePath;
    }

    private String[] splitRelativePath(String relativePath) {
        String normalized = normalizeInputPath(relativePath);
        if (!StringUtils.hasText(normalized)) {
            return new String[0];
        }
        return normalized.split("/");
    }

    private String normalizeInputPath(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Path is required");
        }

        String normalized = URLDecoder.decode(value, StandardCharsets.UTF_8);
        normalized = normalized.replace("\\", "/").trim();

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Invalid path");
        }

        return normalized;
    }

    private String extractFileName(String relativePath) {
        String normalized = normalizeInputPath(relativePath);
        int lastSlashIndex = normalized.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    private boolean isRootDeletion(String normalizedTarget, String root) {
        return normalizedTarget.equals(root);
    }

    private void deleteDirectoryRecursively(Path targetPath) throws IOException {
        try (var walk = Files.walk(targetPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private String safeOriginalFilename(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "unknown";
    }
}