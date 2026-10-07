package com.astra.sirius;
import java.io.*; import java.net.*;
public class SiriusNode {
    private static final int PORT=8766; private ServerSocket serverSocket; private Thread serverThread;
    public void start(){if(serverThread!=null&&serverThread.isAlive())return; serverThread=new Thread(()->{try{serverSocket=new ServerSocket(PORT);while(!Thread.currentThread().isInterrupted()){Socket client=serverSocket.accept();handleClient(client);}}catch(Exception e){if(serverSocket!=null)System.out.println("[SIRIUS] NODE ERROR: "+e.getMessage());}});serverThread.start();}
    private void handleClient(Socket client){try(Socket s=client;BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream()));PrintWriter w=new PrintWriter(s.getOutputStream(),true)){String m=r.readLine();if(m!=null)w.println(processTask(m));}catch(Exception e){System.out.println("[SIRIUS] CLIENT ERROR: "+e.getMessage());}}
    private String processTask(String task){if("PING".equals(task))return "PONG";if("INFO".equals(task))return getDeviceInfo();if(task.startsWith("TASK:ADD:")){try{String[] n=task.substring(9).split(":");if(n.length!=2)return "ERROR:INVALID_TASK";return "RESULT:"+(Integer.parseInt(n[0])+Integer.parseInt(n[1]));}catch(Exception e){return "ERROR:INVALID_NUMBERS";}}return "ERROR:UNKNOWN_TASK";}
    private String getDeviceInfo(){return "INFO:RAM_TOTAL="+mem("MemTotal")+":RAM_AVAILABLE="+mem("MemAvailable")+":CPU_CORES="+Runtime.getRuntime().availableProcessors()+":ARCH="+System.getProperty("os.arch","UNKNOWN");}
    private long mem(String key){try(BufferedReader r=new BufferedReader(new FileReader("/proc/meminfo"))){String l;while((l=r.readLine())!=null){if(l.startsWith(key)){String[] p=l.trim().split("\\s+");return Long.parseLong(p[1])/1024;}}}catch(Exception ignored){}return -1;}
    public void stop(){try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}serverThread=null;}
}
