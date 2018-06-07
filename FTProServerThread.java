package com.advse.team8.ftproserver;

import com.advse.team8.ftproserver.controller.FTProServerController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FTProServerThread
  extends Thread
{
  private Socket controlChannel = null;
  private Socket dataChannel = null;
  private ServerSocket dataConnection = null;
  private String clientName = "";
  
  public FTProServerThread(Socket controlChannel, ServerSocket dataConnection)
  {
    this.controlChannel = controlChannel;
    this.dataConnection = dataConnection;
    this.clientName = (controlChannel.getInetAddress() + ":" + controlChannel.getPort());
  }
  
  public void run()
  {
    DataInputStream inputStream = null;
    
    DataOutputStream outputStream = null;
    boolean notQuit = true;
    try
    {
      inputStream = new DataInputStream(this.controlChannel.getInputStream());
      outputStream = new DataOutputStream(this.controlChannel.getOutputStream());
      
      String msgToClient = "200 Connected Successfully";
      outputStream.writeUTF(msgToClient);
      
      System.out.println("Listening for commands from the client [" + this.clientName + "]");
      
      FTProServerController ftproServerController = new FTProServerController(this.dataConnection, outputStream, this.clientName);
      
      String commandFromClient = "";
      while (notQuit)
      {
        commandFromClient = inputStream.readUTF();
        notQuit = ftproServerController.processCommand(commandFromClient);
      }
      ftproServerController = null; return;
    }
    catch (EOFException eofException)
    {
      System.err.println(this.clientName + " disconnected!");
    }
    catch (IOException ioException)
    {
      System.err.println(ioException.getMessage());
    }
    finally
    {
      try
      {
        if (inputStream != null) {
          inputStream.close();
        }
        if (outputStream != null) {
          outputStream.close();
        }
        if (this.controlChannel != null) {
          this.controlChannel.close();
        }
        if (!notQuit) {
          System.err.println(this.clientName + " disconnected!");
        }
      }
      catch (IOException localIOException4) {}
    }
  }
}
