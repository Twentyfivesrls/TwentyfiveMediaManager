package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.exceptions.FileDeleteException;
import com.example.twentyfivemediamanager.exceptions.FileDownloadException;
import com.example.twentyfivemediamanager.exceptions.FileUploadException;
import com.example.twentyfivemediamanager.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.webresources.FileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
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
            String fileName = pathSegments[pathSegments.length - 1];
            String[] dividedPath = fileName.split("/");
            String finalFileName = dividedPath[dividedPath.length - 1];
            String[] dividedFileName = finalFileName.split("\\.");
            String extension = dividedFileName[dividedFileName.length - 1];
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(finalFileName);
            MediaType resu = mediaType.orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(resu)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + finalFileName + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new FileDownloadException("Failed to download file: " + path, e);
        } catch (Exception exception) {
            throw new FileDownloadException("Unexpected error occurred while downloading file: " + path, exception);
        }
    }


    @PostMapping("/uploadkkk/{path}/**")
    public ResponseEntity<String> uploadFile(@PathVariable String path, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/uploadkkk/");
            String[] allStrings = pathSegments[1].split("/");
            String fileName = fileStorageService.storeFile(allStrings, file);
            return ResponseEntity.ok().body(fileName);
        } catch (FileAlreadyExistsException e) {
            // Gestione specifica del caso in cui il file esiste già
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IOException | URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while uploading file: " + file.getOriginalFilename());
        }
    }





    @DeleteMapping("/deletekkk/{path}/**")
    public ResponseEntity<String> deleteFile(@PathVariable String path, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String[] pathSegments = fullPath.split("/deletekkk/");
            String[] allStrings = pathSegments[1].split("/");

            // Chiamata al servizio per eliminare il file
            fileStorageService.deleteFile(allStrings);
            return ResponseEntity.ok().body("File deleted successfully");
        } catch (FileNotFoundException e) {
            // Restituisce un errore 404 se il file non è stato trovato
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found: " + path);
        } catch (IOException e) {
            // Restituisce un errore 500 per problemi legati al filesystem
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting file: " + path);
        } catch (Exception e) {
            // Restituisce un errore generico per altre eccezioni inattese
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while deleting file: " + path);
        }
    }



}

