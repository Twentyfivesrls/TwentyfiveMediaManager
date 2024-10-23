package com.example.twentyfivemediamanager.service;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import java.io.IOException;


public interface FileStorageService {
    String storeFile(String[] directory, MultipartFile file) throws IOException;
    Resource loadFileAsResource(String fileName) throws IOException;
    void init();

    void deleteFile(String[] allStrings) throws IOException;
}

