package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {

    // VARIABLES

    // The "source file"; the full, multi-line string named 'input' that she uses in her test cases
    String input;
    // Our storage structure for all tokens :)
    ArrayList<IToken> tokens;
    // Our current index in the storage structure; used in next() and peek()
    int index;

    // VARIABLES: ONLY FOR RUNTIME, WHEN LEXER IS MADE
    // The position of the first char in the current lexeme being analyzed
    int startPos;
    int startLine;
    int startCol;
    // The current position of the next character to read when next() or peek() is called; incremented with each char we read
    int currentPos;
    int currentLine;
    int currentCol;

    // The set of reserved words
    private static final Map<String, IToken.Kind> reservedwords;
    static
    {
        // Initialize map
        reservedwords = new HashMap<>();
        // Keywords:
        reservedwords.put("if", IToken.Kind.KW_IF);
        reservedwords.put("fi", IToken.Kind.KW_FI);
        reservedwords.put("else", IToken.Kind.KW_ELSE);
        reservedwords.put("write", IToken.Kind.KW_WRITE);
        reservedwords.put("console", IToken.Kind.KW_CONSOLE);
        reservedwords.put("void", IToken.Kind.KW_VOID);
        // Types:
        reservedwords.put("int", IToken.Kind.TYPE);
        reservedwords.put("float", IToken.Kind.TYPE);
        reservedwords.put("string", IToken.Kind.TYPE);
        reservedwords.put("boolean", IToken.Kind.TYPE);
        reservedwords.put("color", IToken.Kind.TYPE);
        reservedwords.put("image", IToken.Kind.TYPE);
        // Boolean values:
        reservedwords.put("true", IToken.Kind.BOOLEAN_LIT);
        reservedwords.put("false", IToken.Kind.BOOLEAN_LIT);
        // Operators:
        reservedwords.put("getRed", IToken.Kind.COLOR_OP);
        reservedwords.put("getGreen", IToken.Kind.COLOR_OP);
        reservedwords.put("getBlue", IToken.Kind.COLOR_OP);
        reservedwords.put("getWidth", IToken.Kind.IMAGE_OP);
        reservedwords.put("getHeight", IToken.Kind.IMAGE_OP);
        // Constants:
        reservedwords.put("BLACK", IToken.Kind.COLOR_CONST);
        reservedwords.put("BLUE", IToken.Kind.COLOR_CONST);
        reservedwords.put("CYAN", IToken.Kind.COLOR_CONST);
        reservedwords.put("DARK_GRAY", IToken.Kind.COLOR_CONST);
        reservedwords.put("GRAY", IToken.Kind.COLOR_CONST);
        reservedwords.put("GREEN", IToken.Kind.COLOR_CONST);
        reservedwords.put("LIGHT_GRAY", IToken.Kind.COLOR_CONST);
        reservedwords.put("MAGENTA", IToken.Kind.COLOR_CONST);
        reservedwords.put("ORANGE", IToken.Kind.COLOR_CONST);
        reservedwords.put("PINK", IToken.Kind.COLOR_CONST);
        reservedwords.put("RED", IToken.Kind.COLOR_CONST);
        reservedwords.put("WHITE", IToken.Kind.COLOR_CONST);
        reservedwords.put("YELLOW", IToken.Kind.COLOR_CONST);
    }

    // Constructor
    public Lexer(String input)
    {
        this.input = input;
        this.tokens = new ArrayList<IToken>();
        this.index = 0;

        this.startPos = 0;
        this.startLine = 0;
        this.startCol = 0;
        this.currentPos = 0;
        this.currentLine = 0;
        this.currentCol = 0;

        scanFile();
    }

    // Random helper function
    // Returns whether we've read through all characters in the input file
    private boolean isAtEnd()
    {
        return currentPos >= input.length();
    }

    // Random helper function
    // Returns whether a char is 0..9
    private boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    // Random helper function
    // Returns whether a char is a..z, A..Z, _, or $
    private boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_') ||
                (c == '$');
    }

    // Random helper function
    // Retuns whether a char is 0..9, a..z, A..Z, _, or $
    private boolean isAlphaNumeric(char c)
    {
        return isAlpha(c) || isDigit(c);
    }

    // Purpose: Handles the actual scanning, all at once
    // Should be called only in Lexer constructor after this.input has been set
    public void scanFile()
    {
        // While there is still more chars to analyze
        while(!isAtEnd())
        {
            // We begin a new lexeme
            // Save the position
            scanToken();
        }
        // Manually create & add EOF token at end
        tokens.add(new Token(IToken.Kind.EOF, "", new IToken.SourceLocation(currentLine, currentCol)));
    }

    // Creates a new lexeme that starts at currentPos
    public void scanToken()
    {
        // We begin a new lexeme
        // Set start position to know where lexeme begins
        startPos = currentPos;
        startLine = currentLine;
        startCol = currentCol;

        char c = scanNextChar();
        switch (c)
        {
            // SPECIAL CASE: COMMENT
            case '#' :
                while(!isAtEnd() && peekNextChar() != '\n')
                    scanNextChar();             // return type unused-- we just want to make sure pos keeps updating
                break;

            // SPECIAL CASE: END OF LINE
            case '\n' :
                currentLine++;
                currentCol = 0;
                break;

            // WHITESPACE CHARS TO BE IGNORED
            case ' ' :
            case '\t' :
            case '\r' :
                break;

            // CHARS THAT REPRESENT BEGINNING OF
            // EXACTLY ONE (NON-LITERAL) TOKEN

            case '&' : createToken(IToken.Kind.AND); break;
            case ',' : createToken(IToken.Kind.COMMA); break;
            case '/' : createToken(IToken.Kind.DIV); break;
            case '%' : createToken(IToken.Kind.MOD); break;
            case '*' : createToken(IToken.Kind.TIMES); break;
            case '^' : createToken(IToken.Kind.RETURN); break;
            case '+' : createToken(IToken.Kind.PLUS); break;
            case '|' : createToken(IToken.Kind.OR); break;
            case ';' : createToken(IToken.Kind.SEMI); break;
            case '(' : createToken(IToken.Kind.LPAREN); break;
            case ')' : createToken(IToken.Kind.RPAREN); break;
            case '[' : createToken(IToken.Kind.LSQUARE); break;
            case ']' : createToken(IToken.Kind.RSQUARE); break;

            // CHARS THAT REPRESENT BEGINNING OF
            // MORE THAN ONE (NON-LITERAL) TOKEN

            // Either -> or -
            case '-' :
                if(scanNextCharIfEquals('>'))
                    createToken(IToken.Kind.RARROW);
                else
                    createToken(IToken.Kind.MINUS);
                break;
            //Either == or =
            case '=' :
                if(scanNextCharIfEquals('='))
                    createToken(IToken.Kind.EQUALS);
                else
                    createToken(IToken.Kind.ASSIGN);
                break;
            //Either != or !
            case '!' :
                if(scanNextCharIfEquals('='))
                    createToken(IToken.Kind.NOT_EQUALS);
                else
                    createToken(IToken.Kind.BANG);
                break;
            // Either >= or >> or >
            case '>' :
                if(scanNextCharIfEquals('='))
                    createToken(IToken.Kind.GE);
                else if(scanNextCharIfEquals('>'))
                    createToken(IToken.Kind.RANGLE);
                else
                    createToken(IToken.Kind.GT);
                break;
            // Either <= or << or <- or <
            case '<':
                if(scanNextCharIfEquals('='))
                    createToken(IToken.Kind.LE);
                else if(scanNextCharIfEquals('<'))
                    createToken(IToken.Kind.LANGLE);
                else if(scanNextCharIfEquals('-'))
                    createToken(IToken.Kind.LARROW);
                else
                    createToken(IToken.Kind.LT);
                break;

            // CHARS THAT REPRESENT BEGINNING OF LITERALS

            // STRINGS
            case '"' :
                inString();
                break;

            // NUMBERS
            /*
            case '0' :
                // Edge case: If a 0 is NOT followed by a .
                if(peekNextChar() != '.')
                    // Individual 0 becomes an integer
                    createToken(IToken.Kind.INT_LIT);
                // Otherwise, enter inNumber
                else
                    inNumber();
                break;
            case '1' :
            case '2' :
            case '3' :
            case '4' :
            case '5' :
            case '6' :
            case '7' :
            case '8' :
            case '9' :
                inNumber();
                break;
            */

            // Uh oh! This character doesn't fit *any* criteria!
            default :
                if(isDigit(c))
                {
                    // Edge case: If we have 0 without . immediately after
                    if(c == '0' && peekNextChar() != '.')
                        // The 0 is an integer
                        createToken(IToken.Kind.INT_LIT);
                    else
                        inNumber();
                }
                else if (isAlpha(c))
                    inIdentifier();
                else
                    createToken(IToken.Kind.ERROR);
                break;

        }
    }

    // Purpose: CONSUMES NEXT CHAR
    private char scanNextChar()
    {
        char c = input.charAt(currentPos);
        currentPos++;
        currentCol++;
        return c;
    }

    // Purpose: LOOKAHEAD AT UPCOMING CHARACTER
    private char peekNextChar()
    {
        if(isAtEnd()) return '\0';
        return input.charAt(currentPos);
    }

    // Purpose: LOOKAHEAD AT UPCOMING CHARACTER + 1
    private char peekNextNextChar()
    {
        if(currentPos + 1 >= input.length()) return '\0';
        return input.charAt(currentPos + 1);
    }

    // Purpose: CONSUMES NEXT CHAR **ONLY IF** ITS EXPECTED
    // (Kinda a combination of peek and scan)
    private boolean scanNextCharIfEquals(char expected)
    {
        // If the next char is anything BUT expected
        // Fail to consume
        if (isAtEnd()) return false;
        if (input.charAt(currentPos) != expected) return false;

        // Otherwise, consume char
        currentPos++;
        currentCol++;
        return true;
    }

    // Creates and adds token
    // Reads startPos and currentPos to know what text to assign
    // Reads startLine and startCol to know where lexeme begins/SourceLocation
    private void createToken(IToken.Kind kind)
    {
        String text = input.substring(startPos, currentPos);
        IToken.SourceLocation startsrcloc = new IToken.SourceLocation(startLine, startCol);
        tokens.add(new Token(kind, text, startsrcloc));
    }


    // ========================================== //
    // ==========STATE-HANDLING HELPERS========== //
    // ========================================== //

    // Purpose: Represents IN_STRINGLIT state and its handling
    private void inString()
    {
        // Consume all chars that aren't "
        while(peekNextChar() != '\"' && !isAtEnd())
        {
            // Edge case: If we find the escape sequence \"
            if(peekNextChar() == '\\' && peekNextNextChar() == '\"')
                // Consume an extra character
                scanNextChar();
            // Edge case: If we find a newline
            if(peekNextChar() == '\n')
                // Increase line count
                currentLine++;

            scanNextChar();
        }

        // Edge case: If we eventually hit the EOF
        if(isAtEnd())
        {
            // The resulting token is an error-- we never found the closing "
            createToken(IToken.Kind.ERROR);
            return;
        }

        // Otherwise, it's a string
        scanNextChar();  // Consume closing "
        createToken(IToken.Kind.STRING_LIT);
    }

    // Purpose: Represents IN_NUM state and its handling
    private void inNumber()
    {
        // Consume all numeric chars
        while(isDigit(peekNextChar()))
            scanNextChar();

        // Edge case: If we find a following .
        if(peekNextChar() == '.' && isDigit(peekNextNextChar()))
        {
            // Consume .
            scanNextChar();

            // Consume all numeric chars infront of .
            while(isDigit(peekNextChar()))
                scanNextChar();

            // Edge Case: If float is too large
            String text = input.substring(startPos, currentPos);
            try
            {
                // Throws exception if text > MAX_FLOAT
                Float.valueOf(text);
            }
            catch (Exception e)
            {
                // The resulting token is an error
                createToken(IToken.Kind.ERROR);
                return;
            }

            // Otherwise, the resulting token is a float
            createToken(IToken.Kind.FLOAT_LIT);
            return;
        }
        //It's an integer

        // Edge Case: If integer is too large
        String text = input.substring(startPos, currentPos);
        try
        {
            // Throws exception if text > MAX_FLOAT
            Integer.valueOf(text);
        }
        catch (Exception e)
        {
            // The resulting token is an error
            createToken(IToken.Kind.ERROR);
            return;
        }

        // Otherwise, it's an integer
        createToken(IToken.Kind.INT_LIT);
    }

    // Purpose: Represents IN_IDENT state and its handling
    private void inIdentifier()
    {
        // Consume all alphanumeric chars
        while(isAlphaNumeric(peekNextChar()))
            scanNextChar();

        // Map token text to a reserved word
        String text = input.substring(startPos, currentPos);
        IToken.Kind kind = reservedwords.get(text);

        // If token text didn't map to a reserved word,
        if(kind == null)
            // The token is an identifier
            kind = IToken.Kind.IDENT;

        createToken(kind);
    }


    // ========================================== //
    // ======ABSTRACT METHOD IMPLEMENTATIONS===== //
    // ========================================== //


    // Consumes next token
    @Override public IToken next() throws LexicalException
    {
        // Edge case: If we have already "consumed" EOF
        if(index == tokens.size())
        {
            // There are no other tokens to possibly read
            throw new LexicalException("ACCESSING OUT OF RANGE");
        }

        IToken next = tokens.get(index);
        index++;

        // Edge case: If we found an error token
        if(next.getKind() == IToken.Kind.ERROR)
            // Throw an exception
            throw new LexicalException("ERROR", next.getSourceLocation());

        return next;
    }

    // Reads next token
    @Override public IToken peek() throws LexicalException
    {
        // Edge case: If we have already "consumed" EOF
        if(index == tokens.size())
        {
            // There are no other tokens to possibly read
            throw new LexicalException("ACCESSING OUT OF RANGE");
        }

        IToken next = tokens.get(index);

        // Edge case: If we found an error token
        if(next.getKind() == IToken.Kind.ERROR)
            // Throw an exception
            throw new LexicalException("ERROR", next.getSourceLocation());

        return next;
    }
}
