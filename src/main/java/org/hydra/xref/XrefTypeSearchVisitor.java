package org.hydra.xref;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.util.ArrayList;

public class XrefTypeSearchVisitor extends EmptyVisitor {


    private ArrayList<String> result;
    private String searchType;
    private String searchName;
    private String searchDesc;
    private int lineNumber;

    public XrefTypeSearchVisitor(ArrayList<String> result, String searchType, String searchName, String searchDesc) {
        this.result = result;
        this.searchType = searchType;
        this.searchName = searchName;
        this.searchDesc = searchDesc;
    }

    public void setFilename(String filename) {

    }

    @Override
    public void visitLineNumber(int i, Label label) {
        lineNumber = i;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (owner.equals(searchType) && name.equals(searchName) && desc.equals(searchDesc)) {
            //TODO result.add()
        }
    }


}
