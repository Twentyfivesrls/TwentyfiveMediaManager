package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

@RestController
@RequestMapping("/{appId}/{userId}/{category}")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@PathVariable String appId, @PathVariable String userId,
                                             @PathVariable String category, @RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(appId, userId, category, file);
        return ResponseEntity.ok().body(fileName);
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String appId, @PathVariable String userId,
                                                 @PathVariable String category, @PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(appId, userId, category, fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}

