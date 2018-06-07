/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.advse.team8.ftproclient;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gokulkarthik
 */
public class ClientUploadThread extends Thread {
    String fileName = "";
    Socket dataChannel = null;
    boolean status = true;
    String statusString = "No status available!";
            
    public ClientUploadThread(String threadId, String fileName, String host, int dataPort){
        super(threadId);
        this.fileName = fileName;
        try {
            this.dataChannel = new Socket(host, dataPort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public String getStatus(){
        return statusString;
    }
    
    public void run(){
        if(status){
            statusString = Thread.currentThread().getName()+": Ready (0% | 0 bytes)";
        }
        FileInputStream fileInputStream = null;
        OutputStream dataChannelOutputStream = null;
        DataOutputStream doutputStream = null;
        try{
            File fileobj = new File(fileName);
            fileInputStream = new FileInputStream(fileobj);
            dataChannelOutputStream = dataChannel.getOutputStream();
            byte[] buffer = new byte[dataChannel.getSendBufferSize()];
            System.out.println(Thread.currentThread().getName()+": length of send buffer = "+buffer.length);
            int bytesRead = 0;
            System.out.println(Thread.currentThread().getName()+": "+fileName + " is uploading...");
            System.out.println("");
            long filesize = fileobj.length();
            System.out.println(Thread.currentThread().getName()+": Size of "+fileName+" in bytes = "+filesize);
            double progressPercentage = 0;
            double totalBytesRead = 0;
            while((bytesRead = fileInputStream.read(buffer))>0)
            {
                progressPercentage += ((double)bytesRead/filesize)*100; 
                totalBytesRead += bytesRead;
                System.out.print(Thread.currentThread().getName()+ ": "+Math.round(progressPercentage)+"% ("+totalBytesRead+" bytes) \r");
                if (status && progressPercentage < 100) {
                    statusString = Thread.currentThread().getName()+": In Progress ("+Math.round(progressPercentage)+"% | "+totalBytesRead+" bytes)";
                } else if (status && progressPercentage == 100) {
                    statusString = Thread.currentThread().getName()+": Completed ("+Math.round(progressPercentage)+"% | "+totalBytesRead+" bytes)";
                }
                dataChannelOutputStream.write(buffer,0,bytesRead);
            }
            System.out.println(Thread.currentThread().getName()+" finsihes!");
        } catch(IOException ioex){
            System.err.println(ioex.getMessage());
       } finally{
           try {
                if (fileInputStream!=null) fileInputStream.close();
                if (dataChannelOutputStream!=null) dataChannelOutputStream.close();
                if (doutputStream!=null) doutputStream.close();
                if (dataChannel!=null) dataChannel.close();
           } catch (IOException ioex){
               System.err.println(ioex.getMessage());
           }
           
       }
    }
}
