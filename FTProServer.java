package com.advse.team8.ftproserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FTProServer
{
  public static void main(String[] args)
  {
    int PORT_NUMBER = 1234;
    
    ServerSocket controlConnection = null;
    
    Socket controlChannel = null;
    
    ServerSocket dataConnection = null;
    try
    {
      controlConnection = new ServerSocket(1234);
      dataConnection = new ServerSocket(1235);
      System.out.println(controlConnection);
      for (;;)
      {
        controlChannel = controlConnection.accept();
        System.out.println("Received connection request from client : " + controlChannel);
        System.out.println("Connection established to client [" + controlChannel.getInetAddress() + "]");
        
        new FTProServerThread(controlChannel, dataConnection).start();
      }
    }
    catch (IOException ioException)
    {
      System.err.println(ioException.getMessage());
    }
  }
}
