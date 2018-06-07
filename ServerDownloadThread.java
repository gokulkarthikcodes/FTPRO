package com.advse.team8.ftproserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

public class ServerDownloadThread
  extends Thread
{
  String fileName = "";
  Socket dataChannel = null;
  
  public ServerDownloadThread(String threadId, String fileName, Socket dataChannel)
  {
    super(threadId);
    this.fileName = fileName;
    this.dataChannel = dataChannel;
  }
  
  public void run()
  {
    try
    {
      InputStream dataChannelInputStream = this.dataChannel.getInputStream();
      System.out.println(Thread.currentThread().getName() + ": Downloading " + this.fileName);
      byte[] buffer = new byte[this.dataChannel.getReceiveBufferSize()];
      System.out.println(Thread.currentThread().getName() + ": length of receive buffer = " + buffer.length);
      File file = new File(this.fileName);
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      for (;;)
      {
        int bytesRead = 0;
        boolean fileRead = false;
        while ((bytesRead = dataChannelInputStream.read(buffer)) != -1)
        {
          System.out.println(Thread.currentThread().getName() + ": bytesRead = " + bytesRead);
          fileOutputStream.write(buffer, 0, bytesRead);
          fileRead = true;
        }
        fileOutputStream.close();
        if (fileRead) {
          break;
        }
      }
      System.out.println(Thread.currentThread().getName() + " finsihes!");
      dataChannelInputStream.close();
    }
    catch (IOException localIOException) {}
  }
}
