package org.hydra.gui.web;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageInformationEmpty;
import org.benf.cfr.reader.util.SetFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.*;

public class XrefDumper
        implements Dumper {
    private int outputCount = 0;
    private int lineNumber = 1;
    private int indent;
    private boolean atStart = true;
    private boolean pendingCR = false;
    private TypeUsageInformation typeUsageInformation = new TypeUsageInformationEmpty();
    private final Set<JavaTypeInstance> emitted = SetFactory.newSet();
    public String jarId;
    public String classFilename;
    public String methodName, methodSig;

    public HashMap<String, ArrayList<XrefResultItem>> xref = new HashMap<String, ArrayList<XrefResultItem>>();

    public void addXref(String to, XrefResultItem location) {
        if (!xref.containsKey(to)) {
            xref.put(to, new ArrayList<XrefResultItem>());
        }
        xref.get(to).add(location);
    }

    private XrefResultItem getXrefLocation(String usageType) {
        return new XrefResultItem(jarId, classFilename, methodName, methodSig, lineNumber, usageType);
    }

    public XrefDumper(){}
    public XrefDumper(TypeUsageInformation typeUsageInformation) {
        this.typeUsageInformation = typeUsageInformation;
    }

    public static String toString(Dumpable d)
    {
        return new CfrToHtmlDumper().dump(d).toString();
    }

    private void incrementLineNumber() {
        lineNumber++;
    }

    public void printLabel(String s)
    {
        processPendingCR();
        this.atStart = true;
    }

    public void enqueuePendingCarriageReturn()
    {
        this.pendingCR = true;
    }

    public Dumper removePendingCarriageReturn()
    {
        this.pendingCR = false;
        return this;
    }

    private void processPendingCR()
    {
        if (this.pendingCR)
        {
            incrementLineNumber();
            this.atStart = true;
            this.pendingCR = false;
        }
    }

    public Dumper identifier(String s, Expression expression, MethodPrototype method, Field field, JavaTypeInstance definedOn, Object caller)
    {
        String idType="",defType="";
        String sig="";
        if(method!=null){ idType="method"; sig = CfrTypeHelper.CfrMethodPrototypeToSignature(method); }
        if(field!=null){ idType="field"; sig = CfrTypeHelper.CfrTypeToSignature(field.getJavaTypeInstance()); }
        if(expression!=null)defType="expr";else defType="def";
        if (method!=null && expression == null) {
            // this is a method definition
            methodName = s; methodSig = sig;
        }

        String refTo=idType+" ";
        if (definedOn != null) {
            String classfilepath = ClassNameUtils.convertToPath(definedOn.getRawName());
            refTo+=classfilepath+" ";
        }
        refTo+=s+" "+sig;

        addXref(refTo, getXrefLocation(defType));


        return this;
    }


    public Dumper print(String s)
    {
        processPendingCR();
        this.atStart = s.endsWith("\n");
        this.outputCount += 1;
        if (s.endsWith("\n")) incrementLineNumber();

        return this;
    }

    public Dumper print(char c)
    {
        return print("" + c);
    }

    public Dumper newln()
    {
        incrementLineNumber();
        return this;
    }

    public Dumper endCodeln()
    {
        incrementLineNumber();
        return this;
    }

    private void doIndent()
    {
    }

    public int getIndent()
    {
        return this.indent;
    }

    public void indent(int diff)
    {
        this.indent += diff;
    }

    public void dump(List<? extends Dumpable> d)
    {
        for (Dumpable dumpable : d) {
            //dumpable.dump(this);
            this.dump(dumpable);
        }
    }

    public Dumper dump(Dumpable d)
    {
        d.dump(this);
        return this;
    }

    public TypeUsageInformation getTypeUsageInformation()
    {
        return this.typeUsageInformation;
    }

    public Dumper dump(JavaTypeInstance javaTypeInstance)
    {
        String classfilepath = ClassNameUtils.convertToPath(javaTypeInstance.getRawName());
        addXref("type "+classfilepath, getXrefLocation(""));

        javaTypeInstance.dumpInto(this, this.typeUsageInformation);

        return this;
    }

    public String toString()
    {
        return "";
    }

    public void addSummaryError(Method method, String s) {}

    public void close() {}

    public boolean canEmitClass(JavaTypeInstance type)
    {
        return this.emitted.add(type);
    }

    public int getOutputCount()
    {
        return this.outputCount;
    }
}
