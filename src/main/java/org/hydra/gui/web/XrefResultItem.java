package org.hydra.gui.web;

public class XrefResultItem {
    public String jarFile;
    public String className;
    public String methodName;
    public String methodSig;
    public int lineNumber;
    public String usageType;

    public XrefResultItem(String jarFile, String className, String methodName, String methodSig, int lineNumber, String usageType) {
        this.jarFile = jarFile;
        this.className = className;
        this.methodName = methodName;
        this.methodSig = methodSig;
        this.lineNumber = lineNumber;
        this.usageType = usageType;
    }
}
