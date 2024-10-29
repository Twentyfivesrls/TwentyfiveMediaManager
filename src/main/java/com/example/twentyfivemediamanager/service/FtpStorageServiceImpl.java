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
import java.nio.file.*;

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

        Path destinationFile = Paths.get(this.rootLocation.toString(), newString.toString());

        // Verifica se il file esiste già
        if (Files.exists(destinationFile)) {
            throw new FileAlreadyExistsException("File already exists: " + destinationFile.toString());
        }

        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        // Creazione della directory e copia del file
        Files.createDirectories(destinationFile.getParent());
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return "OK";
    }

    @Override
    public void deleteFile(String[] directory) throws IOException, URISyntaxException {
        StringBuilder path = new StringBuilder(rootLocation.toString());
        for (String dir : directory) {
            path.append("/").append(dir);
        }

        String transformedPath = new URI(path.toString()).getPath();
        Path file = rootLocation.resolve(transformedPath);
        if (!Files.exists(file)) {
            throw new FileNotFoundException("File not found: " + transformedPath.toString());
        }

        Files.delete(file);
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
}
