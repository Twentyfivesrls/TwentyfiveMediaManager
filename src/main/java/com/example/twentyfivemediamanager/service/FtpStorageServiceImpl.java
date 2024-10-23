package com.example.twentyfivemediamanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FtpStorageServiceImpl implements FileStorageService {

    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    private final Path rootLocation;

    public FtpStorageServiceImpl(@Value("${file.storage.location}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
    }

    // WITHOUT FTP
    @Override
    public void init() throws IOException {
        Files.createDirectories(rootLocation);
    }

    @Override
    public String storeFile(String[] directory, MultipartFile file) throws IOException, URISyntaxException {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < directory.length; i++) {
            path.append("/").append(directory[i]);
        }
        path.append("/");
        String transformedPath = new URI(path.toString()).getPath();
        StringBuilder newString = new StringBuilder();
        newString.append(transformedPath);
        newString.append(file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        Path destinationFile = Paths.get(this.rootLocation.toString(), newString.toString());
        Files.createDirectories(destinationFile.getParent());
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return "OK";
    }

    @Override
    public Resource loadFileAsResource(String path) throws IOException, URISyntaxException {
        String transformedPath = new URI(path).getPath();
        Path file = rootLocation.resolve(transformedPath);
        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Could not read file: " + path);
        }
        return resource;
    }

    @Override
    public void deleteFile(String[] directory) throws IOException {
        StringBuilder path = new StringBuilder(rootLocation.toString());
        for (String dir : directory) {
            path.append("/").append(dir);
        }

        Path filePath = Paths.get(path.toString());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath.toString());
        }

        Files.delete(filePath);
    }
}
