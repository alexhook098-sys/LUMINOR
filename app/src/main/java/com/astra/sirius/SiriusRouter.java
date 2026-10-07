package com.astra.sirius;
import java.io.*;import java.net.*;import java.util.*;
public class SiriusRouter {
    private final List<SiriusNodeInfo> nodes=new ArrayList<>();
    public SiriusRouter(String host,int port){nodes.add(new SiriusNodeInfo("Vivo",host,port));}
    public void addNode(String name,String host,int port){nodes.add(new SiriusNodeInfo(name,host,port));} public List<SiriusNodeInfo> getNodes(){return nodes;}
    public void checkNodes(){for(SiriusNodeInfo n:nodes){if(checkNode(n))requestNodeInfo(n);}}
    private boolean checkNode(SiriusNodeInfo n){try(Socket s=new Socket()){long t=System.nanoTime();s.connect(new InetSocketAddress(n.getHost(),n.getPort()),1000);n.setLatencyMs((System.nanoTime()-t)/1000000);n.setOnline(true);return true;}catch(Exception e){n.setOnline(false);n.setLatencyMs(-1);return false;}}
    private void requestNodeInfo(SiriusNodeInfo n){try(Socket s=new Socket(n.getHost(),n.getPort());BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream()));PrintWriter w=new PrintWriter(s.getOutputStream(),true)){w.println("INFO");parseNodeInfo(n,r.readLine());}catch(Exception ignored){}}
    private void parseNodeInfo(SiriusNodeInfo n,String x){if(x==null||!x.startsWith("INFO:"))return;for(String p:x.split(":")){try{if(p.startsWith("RAM_TOTAL="))n.setTotalRamMb(Long.parseLong(p.substring(10)));else if(p.startsWith("RAM_AVAILABLE="))n.setAvailableRamMb(Long.parseLong(p.substring(14)));else if(p.startsWith("CPU_CORES="))n.setCpuCores(Integer.parseInt(p.substring(10)));else if(p.startsWith("ARCH="))n.setCpuArchitecture(p.substring(5));}catch(Exception ignored){}}}
    public SiriusTask createAddTask(int a,int b){return new SiriusTask("ADD",a+":"+b);} public String getNodesStatus(){checkNodes();StringBuilder b=new StringBuilder();for(SiriusNodeInfo n:nodes)b.append(n.getName()).append(n.isOnline()?"  ● ONLINE  • ":"  ● OFFLINE").append(n.isOnline()?n.getLatencyMs()+" ms  • "+n.getHealthStatus():"").append('\n');return b.toString().trim();}
}
