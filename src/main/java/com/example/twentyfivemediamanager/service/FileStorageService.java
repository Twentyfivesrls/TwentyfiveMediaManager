package com.example.twentyfivemediamanager.service;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;


public interface FileStorageService {
    String storeFile(String[] directory, MultipartFile file);
    Resource loadFileAsResource(String fileName);
}

