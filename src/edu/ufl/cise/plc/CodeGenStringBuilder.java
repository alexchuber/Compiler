package edu.ufl.cise.plc;

public class CodeGenStringBuilder {
    StringBuilder delegate;

    //methods reimplemented—just call the delegates method
    public CodeGenStringBuilder append(String st) {
        delegate.append(st);
        return this;
    }

    //TODO: this might not work
    public CodeGenStringBuilder append(CodeGenStringBuilder st) {
        delegate.append(st);
        return this;
    }

    //new methods, for readability :)
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

    public CodeGenStringBuilder semi(){
        delegate.append(";");
        return this;
    }

    public CodeGenStringBuilder newline() {
        delegate.append("\n");
        return this;
    }

    public CodeGenStringBuilder equal(){
        delegate.append("=");
        return this;
    }

    //add more as needed :)
}
