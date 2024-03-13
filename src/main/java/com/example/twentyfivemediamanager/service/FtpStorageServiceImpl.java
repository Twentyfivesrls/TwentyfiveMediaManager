package com.example.twentyfivemediamanager.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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


    @Override
    public String storeFile(String appId, String userId, String category, MultipartFile file) {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String fileName = appId + "/" + userId + "/" + category + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String directory = "/" + appId + "/" + userId + "/" + category;

            System.out.println("Directory :" + directory);

            if (!ftpClient.changeWorkingDirectory(directory)) {
                System.out.println("Directory not found, creating: " + directory);
                if (!ftpClient.makeDirectory(directory)) {
                    System.out.println("Failed to create directory: " + directory);
                    return "false";
                }
            }

            // Leggi i dati dal MultipartFile in un ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(file.getInputStream(), outputStream);
            outputStream.close();

            // Crea un ByteArrayInputStream dai dati letti
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

            // Salva il file sul server FTP
            boolean success = ftpClient.storeFile(fileName, inputStream);
            if (success) {
                System.out.println("File stored successfully: " + fileName);
            } else {
                System.out.println("Failed to store file: " + fileName);
            }

            // Chiudi il ByteArrayInputStream
            inputStream.close();

            return "success";
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on FTP server", e);
        } finally {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Resource loadFileAsResource(String appId, String userId, String category, String fileName) {
        FTPClient ftpClient = new FTPClient();
        try  {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("APPID :" + userId + " " + category + " " + fileName);


            String filePath = "/" + appId + "/" + userId + "/" + category + "/" + fileName;

            InputStream inputStream = ftpClient.retrieveFileStream(filePath);
            try {
                if (inputStream == null) {
                    throw new FileNotFoundException("File not found: " + fileName);
                }
                return new InputStreamResource(inputStream);
            } finally {
                ftpClient.logout();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file from FTP server", e);
        }
    }
}

