package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.webresources.FileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.Optional;


@RestController
@CrossOrigin("*")
@RequestMapping("/twentyfiveserver")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/downloadkkk/{path}/**")
    public ResponseEntity<Resource> downloadFile(@PathVariable String path, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/downloadkkk/");
            String fileName = pathSegments[pathSegments.length-1];
            String[] dividedPath = fileName.split("/");
            String finalFileName = dividedPath[dividedPath.length-1];
            String[] dividedFileName = finalFileName.split("\\.");
            String extension = dividedFileName[dividedFileName.length-1];
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(finalFileName);
            MediaType resu = mediaType.orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(resu)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + finalFileName + "\"")
                    .body(resource);
        }catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @PostMapping("/uploadkkk/{path}/**")
    public ResponseEntity<String> getElements(@PathVariable String path, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/uploadkkk/");
            String[] allStrings = pathSegments[1].split("/");
            String fileName = fileStorageService.storeFile(allStrings, file);
            return ResponseEntity.ok().body(fileName);
        }
        catch (Exception exception){
            exception.printStackTrace();
            return null;
        }
    }
}

