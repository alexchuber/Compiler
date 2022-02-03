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
        // This would result in a bug
        if (kind != Kind.INT_LIT)
            return -1;
        return Integer.parseInt(getText());
    }

    //returns the float value represented by the characters of this token if kind is FLOAT_LIT
    @Override public float getFloatValue()
    {
        // This would result in a bug
        if(kind != Kind.FLOAT_LIT)
            return -1;
        return Float.parseFloat(getText());
    }

    //returns the boolean value represented by the characters of this token if kind is BOOLEAN_LIT
    @Override public boolean getBooleanValue()
    {
        // This would result in a bug
        if(kind != Kind.BOOLEAN_LIT)
            return false;
        return Boolean.parseBoolean(getText());
    }

    //returns the String represented by the characters of this token if kind is STRING_LIT
    //The delimiters should be removed and escape sequences replaced by the characters they represent.
    @Override public String getStringValue()
    {
        // This would result in a bug
        if(kind != Kind.STRING_LIT)
            return "";

        String stringval = "";
        int i = 1;
        while (i < getText().length() - 1)
        {
            char current = getText().charAt(i);
            // If there's a possible unhandled \
            if(current == '\\' && i < getText().length() - 2)
            {
                // Look at char that follows \
                i++;
                char next = getText().charAt(i);
                switch (next)
                {
                    case 'b' : stringval += '\b'; break;
                    case 't' : stringval += '\t'; break;
                    case 'n' : stringval += '\n'; break;
                    case 'f' : stringval += '\f'; break;
                    case 'r' : stringval += '\r'; break;
                    case '\"' : stringval += '\"'; break;    //maybe
                    case '\'' : stringval += '\''; break;    //maybe
                    case '\\' : stringval += '\\'; break;    //maybe
                    default : stringval += (current + next); break;
                }
            }
            // If just a regular ol' char
            else
            {
                stringval += current;
            }
            i++;
        }
        return stringval;
    }
}
