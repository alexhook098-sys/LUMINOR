package com.astra.sirius;
public class SiriusTask {
    private final String type,data; private final long requiredRamMb;
    public SiriusTask(String type,String data){this.type=type;this.data=data;requiredRamMb=type.equals("HEAVY")?700:type.equals("MEDIUM")?400:100;}
    public String getType(){return type;} public String getData(){return data;} public long getRequiredRamMb(){return requiredRamMb;} public String encode(){return "TASK:"+type+":"+data;}
}
