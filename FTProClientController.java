/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.advse.team8.ftproclient.controller;

import com.advse.team8.ftproclient.ClientUploadThread;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author gokulkarthik
 */
public class FTProClientController {
    private final int dataPort = 1235;
    private DataOutputStream outputStream = null;
    // To get FTP commands from the client
    Scanner scanner = new Scanner(System.in);
    // Socket for transferring files 
    Socket dataChannel = null;
    String currentDirectory = "/home/ganesh/ftpclientDownloads/";
    String fileType = "I";
    //String host = "ec2-13-59-60-64.us-east-2.compute.amazonaws.com";
    String host = "localhost";
    List<ClientUploadThread> clientUploadThreadsPool = new ArrayList<>();
    
    public FTProClientController(DataOutputStream outputStream, String host){
        this.outputStream = outputStream;
        this.host = host;
    }
    
    public String processServerReply(String commandToServer, String replyFromServer) throws IOException{
        String remoteFileName = "";
        System.out.println("commandToServer = "+commandToServer);
        System.out.println("replyFromServer = "+replyFromServer);
      
        if (replyFromServer==null || replyFromServer.isEmpty()){
            return "";
        } 
        if(!commandToServer.isEmpty() && commandToServer.contains("RETR")){
            String[] retrWords = commandToServer.split(" ");
            if (retrWords.length == 2) {
                remoteFileName = retrWords[1];
            }
        }
        if (replyFromServer.contains("200")) {
            if (replyFromServer.contains("TYPE")) {
                System.out.println("PASV or PORT ??");
                commandToServer = scanner.nextLine();
                outputStream.writeUTF(commandToServer);
                return commandToServer;
            } else if (replyFromServer.contains("MPUT")){
                String fileNames[] = commandToServer.split(" ");
                int i = 1;
                for (i=1; i<fileNames.length;i++){
                    clientUploadThreadsPool.add(new ClientUploadThread("UploadThread "+i, fileNames[i], host, dataPort));
                }
                for (ClientUploadThread clientUploadThread : clientUploadThreadsPool) {
                    if(clientUploadThread != null)
                        clientUploadThread.start();
                }
                System.out.println("All threads started "+clientUploadThreadsPool.size());
                System.out.println("Type STATUS frequently to get updates about the current transfers");
                // This command should be STATUS
                int completed = 0;
                while(completed<5){
                    // This is to see the status once all transfers are completed
                    if (completed == 4) completed+=1;
                    commandToServer = scanner.nextLine();
                    if(commandToServer.contains("STATUS")) {
                        for (ClientUploadThread clientUploadThread : clientUploadThreadsPool) {
                            if(clientUploadThread != null){
                                 System.out.println(clientUploadThread.getStatus());
                                 if(clientUploadThread.getStatus().contains("Completed")) completed+=1;
                                 System.out.println("--------------------------------------------------------------------");
                            }
                        }
                    }
                }
                System.out.println("All files uploaded successfully to the server.");
                System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
                commandToServer = scanner.nextLine();
                outputStream.writeUTF(commandToServer);
            } else {
                System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
                // This command should be USER
                commandToServer = scanner.nextLine();
                outputStream.writeUTF(commandToServer);
            }
           
        } else if (replyFromServer.contains("227")) {
            System.out.println("STOR or RETR ??");
            commandToServer = scanner.nextLine();
            outputStream.writeUTF(commandToServer);
            // Connect to the server's data channel to intiate the file transfer
            dataChannel = new Socket(host, dataPort);
        } else if (replyFromServer.contains("125")) {
            if (dataChannel!=null) {
                System.out.println("Server is ready to receive the file. Enter the local file path (E.g:/localpath/file) to send");
                String localFileName = scanner.nextLine();
                
                if (!localFileName.isEmpty()) {
                    FileInputStream fileInputStream = null;
                    OutputStream dataChannelOutputStream = null;
                    DataOutputStream doutputStream = null;
                    try{
                        File fileobj = null;
                        while (true) {
                            fileobj = new File(localFileName);
                            if (fileobj.isDirectory()) {
                                System.err.println("Directory can't be uploaded!!");
                                System.out.println("Enter valid local file path (E.g:/localpath/file)");
                                localFileName = scanner.nextLine();
                            } else if(!fileobj.exists()){
                                System.err.println("File doesn't exist!");
                                System.out.println("Enter valid local file path (E.g:/localpath/file)");
                                localFileName = scanner.nextLine();
                            } else break;
                        }
                        if (fileType.equals("I")) {
                            fileInputStream = new FileInputStream(fileobj);
                            dataChannelOutputStream = dataChannel.getOutputStream();
                            byte[] buffer = new byte[dataChannel.getSendBufferSize()];
                            System.out.println("length of send buffer = "+buffer.length);
                            int bytesRead = 0;
                            System.out.println(localFileName + " is uploading...");
                            System.out.println("");
                            long filesize = fileobj.length();
                            System.out.println("Size of "+localFileName+" in bytes = "+filesize);
                            double progressPercentage = 0;
                            double totalBytesRead = 0;
                            while((bytesRead = fileInputStream.read(buffer))>0)
                            {
                                progressPercentage += ((double)bytesRead/filesize)*100; 
                                totalBytesRead += bytesRead;
                                System.out.print(Math.round(progressPercentage)+"% ("+totalBytesRead+" bytes) \r");
                                dataChannelOutputStream.write(buffer,0,bytesRead);
                            }
                            System.out.println("");
                        } else {
                            byte[] encoded = Files.readAllBytes(Paths.get(localFileName));
                            String fileContents = new String(encoded, Charset.forName("US-ASCII"));
                            doutputStream = new DataOutputStream(dataChannel.getOutputStream());
                            doutputStream.writeUTF(fileContents);
                        }
                        
                    } catch(FileNotFoundException fnotex){
                         System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
                         commandToServer = scanner.nextLine();
                         outputStream.writeUTF(commandToServer);
                         
                    } finally{
                        if (fileInputStream!=null) fileInputStream.close();
                        if (dataChannelOutputStream!=null) dataChannelOutputStream.close();
                        if (doutputStream!=null) doutputStream.close();
                        if (dataChannel!=null) dataChannel.close();
                    }
                    
                }
            }   
        } else if (!commandToServer.isEmpty() && commandToServer.contains("STOR") && replyFromServer.contains("226")) {
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            if (commandToServer.contains("TYPE")) {
                this.fileType = commandToServer.split(" ")[1];
            }
            outputStream.writeUTF(commandToServer);
        } else if (!commandToServer.isEmpty() && commandToServer.contains("RETR") && replyFromServer.contains("226")) {
            System.out.println("Starting to read file contents from data channel");
            InputStream dataChannelInputStream = dataChannel.getInputStream();
            File file = new File(currentDirectory + remoteFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[dataChannel.getReceiveBufferSize()];
            System.out.println("length of receive buffer = "+buffer.length);
            System.out.println("Downloading file from server");
            int bytesRead = 0;
            while ((bytesRead = dataChannelInputStream.read(buffer)) != -1) {
                System.out.println("bytesRead = "+bytesRead);
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            dataChannelInputStream.close();
            dataChannel.close();
            System.out.println("File downloaded successfully");
            commandToServer = "Download completed";
            outputStream.writeUTF(commandToServer);
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            outputStream.writeUTF(commandToServer);
        } else if (!commandToServer.isEmpty() && commandToServer.contains("MPUT")){
            System.out.println("Waiting for confirmation from the server... ");
        } else {
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            if (commandToServer.contains("TYPE")) {
                this.fileType = commandToServer.split(" ")[1];
            }
            outputStream.writeUTF(commandToServer);
        }
        return commandToServer;
    }
}
