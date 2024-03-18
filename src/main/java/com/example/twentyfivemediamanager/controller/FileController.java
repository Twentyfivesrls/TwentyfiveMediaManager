package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.Arrays;

@RestController
@RequestMapping("/twentyfiveserver")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{path}/**")
    public ResponseEntity<Resource> downloadFile(@PathVariable String path, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/");
            String[] additionalPathSegments = Arrays.copyOfRange(pathSegments, 3, pathSegments.length);
            String fileName = pathSegments[pathSegments.length-1];
            Resource resource = fileStorageService.loadFileAsResource(additionalPathSegments, fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @PostMapping("/upload/{path}/**")
    public ResponseEntity<String> getElements(@PathVariable String path, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/");
            String[] additionalPathSegments = Arrays.copyOfRange(pathSegments, 3, pathSegments.length);
            String fileName = fileStorageService.storeFile(additionalPathSegments, file);
            return ResponseEntity.ok().body(fileName);
        }
        catch (Exception exception){
            exception.printStackTrace();
            return null;
        }
    }
}

