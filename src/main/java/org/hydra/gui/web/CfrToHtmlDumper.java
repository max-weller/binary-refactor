package org.hydra.gui.web;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageInformationEmpty;
import org.benf.cfr.reader.util.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import java.lang.StackTraceElement;
import java.lang.Thread;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;

public class CfrToHtmlDumper
  implements Dumper
{
  private int outputCount = 0;
  private int indent;
  private boolean atStart = true;
  private boolean pendingCR = false;
  private final StringBuilder sb = new StringBuilder();
  private TypeUsageInformation typeUsageInformation = new TypeUsageInformationEmpty();
  private final Set<JavaTypeInstance> emitted = SetFactory.newSet();
  public String urlPrefix;

  public CfrToHtmlDumper(){}
  public CfrToHtmlDumper(TypeUsageInformation typeUsageInformation) {
    this.typeUsageInformation = typeUsageInformation;
  }

  public static String toString(Dumpable d)
  {
    return new CfrToHtmlDumper().dump(d).toString();
  }
  
  public void printLabel(String s)
  {
    processPendingCR();
    this.sb.append(s).append(":\n");
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
      this.sb.append('\n');
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
    if(expression!=null)defType=" expression";else defType=" definition";
    String href="";
    this.sb.append("<a ");
    if (definedOn != null) {
      String classfilepath = ClassNameUtils.convertToPath(definedOn.getRawName());
      href+=urlPrefix+classfilepath;
      this.sb.append(" data-classfile='"+classfilepath+"'");
    }
    href+="#"+s+"__"+sig;
    this.sb.append(" class=\"identifierRef "+idType+defType+" "+caller.getClass().getSimpleName()+"\" href=\""+href+"\" data-identifier=\""+s+"\" data-identifiertype=\""+idType+"\" data-signature=\""+sig+"\" title='");
    if(expression!=null)this.sb.append("\n * expression "+expression.toString());
    if(method!=null)this.sb.append("\n * method "+method.toString());
    if(field!=null)this.sb.append("\n * field "+field.toString());
    if(definedOn!=null)this.sb.append("\n * on "+definedOn.getRawName());
    this.sb.append("\n * signature "+sig+"");
    this.sb.append("\n * caller "+caller.getClass().getName()+"'");
    if(expression==null)sb.append(" name='"+s+"__"+sig+"'");
    //if (definedOn != null) this.sb.append(" data-classfile=\""+classfilepath+"\"");
    /*StackTraceElement[] cause = Thread.currentThread().getStackTrace();
    for(int i=0;i<3;i++)
      this.sb.append(" data-stack").append(i).append("='").append(cause[i].getClassName()).append("::").append(cause[i].getMethodName()+"'");*/
    
    this.sb.append(">");
    
    print(s);

    this.sb.append("</a>");
    return this;
  }
  
  public Dumper print(String s)
  {
    processPendingCR();
    doIndent();
    this.sb.append(s.replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"));
    this.atStart = s.endsWith("\n");
    this.outputCount += 1;
    return this;
  }
  
  public Dumper print(char c)
  {
    return print("" + c);
  }
  
  public Dumper newln()
  {
    this.sb.append("\n");
    this.atStart = true;
    this.outputCount += 1;
    return this;
  }
  
  public Dumper endCodeln()
  {
    this.sb.append(";\n");
    this.atStart = true;
    this.outputCount += 1;
    return this;
  }
  
  private void doIndent()
  {
    if (!this.atStart) {
      return;
    }
    String indents = "    ";
    for (int x = 0; x < this.indent; x++) {
      this.sb.append(indents);
    }
    this.atStart = false;
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
    if (d == null)
    {
      print("null");
      return this;
    }
    /*if (d instanceof AbstractMemberFunctionInvokation) {
      AbstractMemberFunctionInvokation inv = (AbstractMemberFunctionInvokation) d;
      this.sb.append("<small class='minilabel red'>"+inv.getObject().getInferredJavaType().getJavaTypeInstance().getRawName()+"</small>");
    }
    if (d instanceof FieldVariable) {
      FieldVariable inv = (FieldVariable) d;
      this.sb.append("<small class='minilabel blue'>"+inv.getObject().getInferredJavaType().getJavaTypeInstance().getRawName()+"</small>");
    }*/
    this.sb.append("<span title='"+d.getClass().getName()+"'>");
    d.dump(this);
    this.sb.append("</span>");
    return this;
  }
  
  public TypeUsageInformation getTypeUsageInformation()
  {
    return this.typeUsageInformation;
  }
  
  boolean isInDumpTypeRef = false;
  public Dumper dump(JavaTypeInstance javaTypeInstance)
  {
    boolean localIsRecursive = isInDumpTypeRef;
    isInDumpTypeRef = true;
    String classfilepath = ClassNameUtils.convertToPath(javaTypeInstance.getRawName());
    if(!localIsRecursive) this.sb.append("<a href='"+urlPrefix+classfilepath+"' title='type: "+javaTypeInstance.getRawName()+"' class='typeRef' data-classfile='"+classfilepath+"'>");
    javaTypeInstance.dumpInto(this, this.typeUsageInformation);
    if(!localIsRecursive) this.sb.append("</a>");
    isInDumpTypeRef = false;
    return this;
  }
  
  public String toString()
  {
    return this.sb.toString();
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
