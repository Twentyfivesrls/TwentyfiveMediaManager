package com.example.twentyfivemediamanager.service;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import java.io.IOException;


public interface FileStorageService {
    String storeFile(String appId, String userId, String category, MultipartFile file);
    Resource loadFileAsResource(String appId, String userId, String category, String fileName);
}

