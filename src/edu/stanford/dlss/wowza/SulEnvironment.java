package edu.stanford.dlss.wowza;

public class SulEnvironment
{
    public String getEnvironmentVariable(String var)
    {
        return System.getenv(var);
    }
}
