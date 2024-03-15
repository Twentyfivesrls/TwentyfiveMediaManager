package com.example.twentyfivemediamanager.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;

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
    public String storeFile(String[] directory, MultipartFile file) {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.changeWorkingDirectory("ftp");
            ftpClient.changeWorkingDirectory("user");
            String fileName =  "_" + file.getOriginalFilename();
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
            boolean success = ftpClient.storeFile(fileName, file.getInputStream());
            if (success) {
                System.out.println("File stored successfully: " + fileName);
            } else {
                System.out.println("Failed to store file: " + fileName);
            }

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
    public Resource loadFileAsResource(String[] directory, String fileName) {
        FTPClient ftpClient = new FTPClient();

        try  {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String path = "/" + "ftp" + "/" + "user";

            for (int i = 0; i < directory.length; i ++){

                path += "/" + directory[i];
            }

            InputStream inputStream = ftpClient.retrieveFileStream(path);
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

