package com.example.twentyfivemediamanager.service;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
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
    public List<String> getFiles(String[] directories) throws URISyntaxException {
        List<String> fileList = new ArrayList<>();

        Path filePath = this.getPath(directories);
        File folder = new File(filePath.toString());

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {  // Ensure it's a file, not a directory
                        fileList.add(file.getName());
                    }
                }
            }
        }

        return fileList;

    }

    @Override
    public String storeFile(String[] directory, MultipartFile file, String strategy) throws IOException, URISyntaxException {
        // APPEND - Aggiunge il file alla directory, se esiste già li mantiene entrambi aggiungendo un suffisso _1, _2 e così via
        // REPLACE - Sostituisce il file esistente con il nuovo file
        // DISCARD - Non fa nulla se il file esiste già
        // comportamento di default è APPEND
        StringBuilder path = new StringBuilder();
        for (String s : directory) {
            path.append("/").append(s);
        }
        path.append("/");
        String transformedPath = new URI(path.toString()).getPath();
        StringBuilder newString = new StringBuilder();
        newString.append(transformedPath);
        newString.append(file.getOriginalFilename());

        Path destinationFile = Paths.get(this.rootLocation.toString(), newString.toString());

        // Verifica se il file esiste già
        if (Files.exists(destinationFile)) {
            if(StringUtils.isBlank(strategy)) {
                strategy = "APPEND";
            }
            switch (strategy) {
                case "APPEND":
                    int i = 1;
                    while (Files.exists(destinationFile)) {
                        newString = new StringBuilder();
                        newString.append(transformedPath);
                        newString.append(file.getOriginalFilename().split("\\.")[0]);
                        newString.append("_").append(i).append(".").append(file.getOriginalFilename().split("\\.")[1]);
                        destinationFile = Paths.get(this.rootLocation.toString(), newString.toString());
                        i++;
                    }
                    break;
                case "REPLACE":
                    //TODO to be tested
                    Files.delete(destinationFile);
                    break;
                case "DISCARD":
                    //TODO to be tested
                    return destinationFile.getFileName().toString();
                default:
                    throw new IllegalArgumentException("Invalid strategy: " + strategy);
            }
        }

        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        // Creazione della directory e copia del file
        Files.createDirectories(destinationFile.getParent());
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return destinationFile.getFileName().toString();
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
        log.info("File path: " + file.toString());
        Resource resource = new UrlResource(file.toUri());
        log.info("Resource exists: " + resource.exists());
        log.info("Resource is readable: " + resource.isReadable());
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Could not read file: " + path);
        }
        return resource;
    }

    private Path getPath(String[] directory) throws URISyntaxException {
        StringBuilder path = new StringBuilder();
        for (String s : directory) {
            path.append("/").append(s);
        }
        path.append("/");
        String transformedPath = new URI(path.toString()).getPath();
        return Paths.get(this.rootLocation.toString(), transformedPath);
    }
}
