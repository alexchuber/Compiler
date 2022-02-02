package edu.ufl.cise.plc;

public class Token implements IToken {

    // Variables
    final Kind kind;
    final String text;
    final SourceLocation sourcelocation;

    // Default constructor-- IntelliJ made it for me.
    // I don't know if we'll stick with it-- we might not want to pass in a SourceLocation, but instead int line & int col?
    public Token(Kind kind, String text, SourceLocation sourcelocation) {
        this.kind = kind;
        this.text = text;
        this.sourcelocation = sourcelocation;
    }

    //returns the token kind
    @Override public Kind getKind()
    {
        return kind;
    }

    //returns the characters in the source code that correspond to this token
    //if the token is a STRING_LIT, this returns the raw characters, including delimiting "s and unhandled escape sequences.
    @Override public String getText()
    {
        return text;
    }

    //returns the location in the source code of the first character of the token.
    @Override public SourceLocation getSourceLocation()
    {
        return sourcelocation;
    }

    //returns the int value represented by the characters of this token if kind is INT_LIT
    @Override public int getIntValue()
    {
        if(kind == Kind.INT_LIT)
            return Integer.parseInt(getText());
        //else this is a bug!
    }

    //returns the float value represented by the characters of this token if kind is FLOAT_LIT
    @Override public float getFloatValue()
    {
        if(kind == Kind.FLOAT_LIT)
            return Float.parseFloat(getText());
        //else this is a bug!
    }

    //returns the boolean value represented by the characters of this token if kind is BOOLEAN_LIT
    @Override public boolean getBooleanValue()
    {
        if(kind == Kind.BOOLEAN_LIT)
            return Boolean.parseBoolean(getText());
        //else this is a bug!
    }

    //returns the String represented by the characters of this token if kind is STRING_LIT
    //The delimiters should be removed and escape sequences replaced by the characters they represent.
    @Override public String getStringValue()
    {
        //if(kind == Kind.STRING_LIT)
            //return stuff
        //else this is a bug!
    }
}
