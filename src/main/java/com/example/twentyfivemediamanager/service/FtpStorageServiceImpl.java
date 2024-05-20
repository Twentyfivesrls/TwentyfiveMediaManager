package com.example.twentyfivemediamanager.service;

import org.apache.commons.net.ftp.FTP;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
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
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }
    @Override
    public String storeFile(String[] directory, MultipartFile file) throws IOException {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < directory.length; i ++){
            path.append("/").append(directory[i]);
        }
        path.append("/");
        path.append(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            Path destinationFile = Paths.get(this.rootLocation.toString(), path.toString());
            Files.createDirectories(destinationFile);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return "OK";
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String path) throws IOException {
        try {
            Path file = rootLocation.resolve(path);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + path);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + path, e);
        }
    }


    //FTP
 /*   @Override
    public String storeFile(String[] directory, MultipartFile file) {
        FTPClient ftpClient = new FTPClient();

        try {

            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.changeWorkingDirectory("ftp");
            ftpClient.changeWorkingDirectory("user");
            String fileName =  file.getOriginalFilename();
            String fileNameSenzaSpazi = fileName.replaceAll("\\s", "");
            String path = "/" + "ftp" + "/" + "user";

            for (int i = 0; i < directory.length; i ++){
                path += "/" + directory[i];
                ftpClient.makeDirectory(path);
            }


            boolean directoryCreated = ftpClient.makeDirectory(path);
            System.out.println("Directory creata: " + directoryCreated);
            if (directoryCreated) {
                System.out.println("Nuova directory creata con successo: nuovaDirectory");
            } else {
                System.out.println("Directory esistente");
            }

            if (!ftpClient.changeWorkingDirectory(path)) {
                System.out.println("Directory not found, creating: " + directory);
                if (!ftpClient.makeDirectory(path)) {
                    System.out.println("Failed to create directory: " + directory);
                    return "false";
                }
            }

            // Salva il file sul server FTP
            boolean success = ftpClient.storeFile(fileNameSenzaSpazi, file.getInputStream());
            if (success) {
                System.out.println("File stored successfully: " + fileNameSenzaSpazi);
            } else {
                System.out.println("Failed to store file: " + fileNameSenzaSpazi);
            }

            return "success";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "false";
    }


    @Override
    public Resource loadFileAsResource(String fileName) {
        FTPClient ftpClient = new FTPClient();

        try  {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String path = "/" + "ftp" + "/" + "user" + "/" + fileName;

            InputStream inputStream = ftpClient.retrieveFileStream(path);
            try {
                if (inputStream == null) {
                    throw new FileNotFoundException("File not found: " + fileName);
                }
                InputStreamResource result = new InputStreamResource(inputStream);
                return new InputStreamResource(inputStream);
            } finally {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; 
    }
 */


    //FTPS
   /* @Override
    public String storeFile(String[] directory, MultipartFile file) throws IOException {
        FTPSClient ftpsClient = new FTPSClient(); // Usa FTPSClient invece di FTPClient

        try {
            ftpsClient.connect(server, port);
            ftpsClient.login(username, password);
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpsClient.execPBSZ(0); // Aggiungi questo per supportare la sicurezza
            ftpsClient.execPROT("P"); // Aggiungi questo per supportare la sicurezza
            ftpsClient.changeWorkingDirectory("ftp");
            ftpsClient.changeWorkingDirectory("user");
            String fileName = "_" + file.getOriginalFilename();
            String path = "/" + "ftp" + "/" + "user";

            System.out.println("SONO QUI 1");

            for (int i = 0; i < directory.length; i++) {
                path += "/" + directory[i];
                ftpsClient.makeDirectory(path);
            }

            System.out.println("SONO QUI 2  " + path);


            boolean directoryCreated = ftpsClient.makeDirectory(path);
            System.out.println("Directory creata: " + directoryCreated);
            if (directoryCreated) {
                System.out.println("Nuova directory creata con successo: nuovaDirectory");
            } else {
                System.out.println("Directory esistente");
            }

            if (!ftpsClient.changeWorkingDirectory(path)) {
                System.out.println("Directory not found, creating: " + directory);
                if (!ftpsClient.makeDirectory(path)) {
                    System.out.println("Failed to create directory: " + directory);
                    return "false";
                }
            }

            // Salva il file sul server FTPS
            boolean success = ftpsClient.storeFile(fileName, file.getInputStream());
            if (success) {
                System.out.println("File stored successfully: " + fileName);
            } else {
                System.out.println("Failed to store file: " + fileName);
            }

            return "success";
        } catch (IOException e) {
            //throw new RuntimeException("Failed to store file on FTPS server", e);
            throw e;
        } finally {
            try {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Resource loadFileAsResource(String fileName) throws IOException {
        FTPSClient ftpsClient = new FTPSClient(); // Usa FTPSClient invece di FTPClient

        try {
            ftpsClient.connect(server, port);
            ftpsClient.login(username, password);
            ftpsClient.enterLocalPassiveMode();
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpsClient.execPBSZ(0); // Aggiungi questo per supportare la sicurezza
            ftpsClient.execPROT("P"); // Aggiungi questo per supportare la sicurezza

            String path = "/" + "ftp" + "/" + "user" + "/" + fileName;

            InputStream inputStream = ftpsClient.retrieveFileStream(path);
            try {
                if (inputStream == null) {
                    throw new FileNotFoundException("File not found: " + fileName);
                }
                return new InputStreamResource(inputStream);
            } finally {
                ftpsClient.logout();
            }
        } catch (IOException e) {
            //throw new RuntimeException("Failed to load file from FTPS server", e);
            throw e;
        }
    }*/

}

