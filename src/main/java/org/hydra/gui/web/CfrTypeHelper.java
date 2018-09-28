package org.hydra.gui.web;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;

public class CfrTypeHelper {
    public static String CfrTypeToSignature(JavaTypeInstance typ) {
        if (typ == RawJavaType.BYTE) return "B";
        if (typ == RawJavaType.CHAR) return "C";
        if (typ == RawJavaType.INT) return "I";
        if (typ == RawJavaType.SHORT) return "S";
        if (typ == RawJavaType.BOOLEAN) return "Z";
        if (typ == RawJavaType.FLOAT) return "F";
        if (typ == RawJavaType.DOUBLE) return "D";
        if (typ == RawJavaType.LONG) return "J";
        if (typ == RawJavaType.VOID) return "V";
        if (typ instanceof JavaRefTypeInstance) {
            return "L" + typ.getRawName().replace(".", "/") + ";";
        } else if (typ instanceof JavaArrayTypeInstance) {
            JavaArrayTypeInstance tt = (JavaArrayTypeInstance)typ;
            StringBuilder b = new StringBuilder();
            int n = tt.getNumArrayDimensions();
            while (n-->0) b.append("[");
            b.append(CfrTypeToSignature(tt.getArrayStrippedType()));
            return b.toString();
        } else if (typ instanceof JavaGenericRefTypeInstance) {
            JavaGenericRefTypeInstance tt = (JavaGenericRefTypeInstance)typ;
            return "L" + tt.getDeGenerifiedType().getRawName().replace(".", "/") + ";";
            //
        } else if (typ instanceof JavaGenericPlaceholderTypeInstance) {
            // ???
        } else if (typ instanceof JavaGenericPlaceholderTypeInstance) {
            // ???
        }
        return "XXXX";
    }
    public static String CfrMethodPrototypeToSignature(MethodPrototype proto){
        StringBuilder b = new StringBuilder();
        b.append("(");
        for(JavaTypeInstance para : proto.getArgs())
            b.append(CfrTypeToSignature(para));
        b.append(")");
        b.append(CfrTypeToSignature(proto.getReturnType()));
        return b.toString();
    }
}
/*

MethodTypeSignature:
    FormalTypeParametersopt (TypeSignature*) ReturnType ThrowsSignature*
TypeSignature:
    FieldTypeSignature
    BaseType
FieldTypeSignature:
    ClassTypeSignature
    ArrayTypeSignature
    TypeVariableSignature 
ArrayTypeSignature:
    [ TypeSignature

ClassTypeSignature:
    L PackageSpecifieropt SimpleClassTypeSignature ClassTypeSignatureSuffix* ;

*/
