package com.example.twentyfivemediamanager.controller;

import com.example.twentyfivemediamanager.exceptions.FileDownloadException;
import com.example.twentyfivemediamanager.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin("*")
@Slf4j
@RequestMapping("/twentyfiveserver")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${file.storage.location}")
    private String fileStorageLocation;

    @GetMapping("/downloadkkk/{path}/**")
    public ResponseEntity<Resource> downloadFile(@PathVariable String path, HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            log.info("Downloading Full path: " + fullPath);
            String[] pathSegments = fullPath.split("/downloadkkk/");
            log.info("Downloading Path segments: " + Arrays.toString(pathSegments));
            String fileName = pathSegments[pathSegments.length - 1];
            String[] dividedPath = fileName.split("/");
            String finalFileName = dividedPath[dividedPath.length - 1];
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            log.info("Resource loaded!");
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(finalFileName);
            log.info("Media type is Present? " + mediaType.isPresent());
            MediaType resu = mediaType.orElse(MediaType.APPLICATION_OCTET_STREAM);
            log.info("Media type: " + resu);
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

    @GetMapping("/copykkk")
    public ResponseEntity<String> moveFile(@RequestParam("source") String source, @RequestParam("target") String target, HttpServletRequest request) {
        try {

            String[] sourceSplit = source.split("/");
            String[] targetSplit = target.split("/");

            Path sourcePath = getPath(sourceSplit);
            Path targetPath = getPath(targetSplit);

            // Verifica se il file esiste
            if (!Files.exists(sourcePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source file not found: " + source);
            }

            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("File copied successfully from " + source + " to " + target);
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source file does not exist: " + source);
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Target file already exists: " + target);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("I/O error while moving file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/infokkk/{path}/**")
    public ResponseEntity<List<String>> getInfo(@PathVariable String path, HttpServletRequest request) throws URISyntaxException {
        //return all the files in the directory
        String fullPath = request.getRequestURI();
        String[] pathSegments = fullPath.split("/infokkk/");
        String[] allStrings = pathSegments[1].split("/");
        return ResponseEntity.ok(fileStorageService.getFiles(allStrings));
    }

    @GetMapping("/renamekkk")
    public ResponseEntity<String> renameFile(@RequestParam("source") String source, @RequestParam("target") String target, HttpServletRequest request) {
        try {
            source = URLDecoder.decode(source, StandardCharsets.UTF_8);
            String[] sourceSplit = source.split("/");
            String[] targetSplit = target.split("/");

            Path sourcePath = getPath(sourceSplit);
            Path targetPath = getPath(targetSplit);

            // Verifica se il file esiste
            if (!Files.exists(sourcePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source file not found: " + source);
            }

            Files.createDirectories(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("File renamed successfully from " + source + " to " + target);
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source file does not exist: " + source);
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Target file already exists: " + target);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("I/O error while renaming file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
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


    private Path getPath(String[] sourceSplit) throws URISyntaxException {
        Path rootLocation = Paths.get(this.fileStorageLocation);

        StringBuilder sourcePath = new StringBuilder(rootLocation.toString());
        for (String dir : sourceSplit) {
            sourcePath.append("/").append(dir);
        }

        String transformedSourcePath = new URI(sourcePath.toString()).getPath();
        return rootLocation.resolve(transformedSourcePath);
    }


}

