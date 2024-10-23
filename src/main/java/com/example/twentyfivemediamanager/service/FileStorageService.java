package com.example.twentyfivemediamanager.service;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URISyntaxException;


public interface FileStorageService {
    String storeFile(String[] directory, MultipartFile file) throws IOException, URISyntaxException;
    Resource loadFileAsResource(String fileName) throws IOException, URISyntaxException;
    void deleteFile(String[] allStrings) throws IOException;

    void init() throws IOException;

}

