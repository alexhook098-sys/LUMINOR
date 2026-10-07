package com.astra.sirius;
import java.io.*; import java.net.Socket;
public class SiriusClient {
    public String sendTask(String host,int port,SiriusTask task){
        try(Socket socket=new Socket(host,port); BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer=new PrintWriter(socket.getOutputStream(),true)){
            writer.println(task.encode()); String response=reader.readLine(); return response==null?"ERROR:NO_RESPONSE":response;
        }catch(Exception e){ return "ERROR:"+e.getMessage(); }
    }
}
