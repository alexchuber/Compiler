package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.Types.Type;

public class CodeGenStringBuilder {
    StringBuilder delegate = new StringBuilder();

    public StringBuilder getStringBuilder() {
        return delegate;
    }

    //methods reimplemented—just call the delegates method
    public CodeGenStringBuilder append(String st) {
        delegate.append(st);
        return this;
    }

    public CodeGenStringBuilder append(CodeGenStringBuilder st) {
        delegate.append(st.getStringBuilder().toString());
        return this;
    }

    public CodeGenStringBuilder append(Type type) {
        String st = switch(type) {
            case STRING -> "String";
            default -> type.toString().toLowerCase();
        };
        delegate.append(st);
        return this;
    }

    public CodeGenStringBuilder append(Object st) {
        delegate.append(st);
        return this;
    }


    public CodeGenStringBuilder comma(){
        delegate.append(",");
        return this;
    }

    public CodeGenStringBuilder lparen(){
        delegate.append("(");
        return this;
    }

    public CodeGenStringBuilder rparen(){
        delegate.append(")");
        return this;
    }
    public CodeGenStringBuilder clparen(){
        delegate.append("{");
        return this;
    }

    public CodeGenStringBuilder crparen(){
        delegate.append("}");
        return this;
    }

    public CodeGenStringBuilder semi(){
        delegate.append(";");
        return this;
    }

    public CodeGenStringBuilder newline() {
        delegate.append("\n");
        return this;
    }

    public CodeGenStringBuilder assign(){
        delegate.append("=");
        return this;
    }
    
    public CodeGenStringBuilder question(){
        delegate.append("?");
        return this;
    }
    
    public CodeGenStringBuilder colon(){
        delegate.append(":");
        return this;
    }

    public CodeGenStringBuilder dblquote(){
        delegate.append("\"");
        return this;
    }

    public CodeGenStringBuilder space(){
        delegate.append(" ");
        return this;
    }

    public CodeGenStringBuilder bang(){
        delegate.append("!");
        return this;
    }



    //add more as needed :)
}
