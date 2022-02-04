package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {

    // ================================= //
    // =========== VARIABLES =========== //
    // ================================= //

    // The "source file" to scan
    private String input;
    // The structure to store all tokens we scanned
    private ArrayList<IToken> tokens;
    // Our current index in the storage structure above
    private int index;

    // The position of the first char in the current lexeme being analyzed
    private int startPos, startLine, startCol;
    // The position of the current character to read when next() or peek() is called; incremented with each char we read
    private int currentPos, currentLine, currentCol;

    // The set of reserved words
    private static final Map<String, IToken.Kind> reservedwords;
    static
    {
        // Initialize map
        reservedwords = new HashMap<>();
        // Keywords
        reservedwords.put("if", IToken.Kind.KW_IF);
        reservedwords.put("fi", IToken.Kind.KW_FI);
        reservedwords.put("else", IToken.Kind.KW_ELSE);
        reservedwords.put("write", IToken.Kind.KW_WRITE);
        reservedwords.put("console", IToken.Kind.KW_CONSOLE);
        reservedwords.put("void", IToken.Kind.KW_VOID);
        // Types
        reservedwords.put("int", IToken.Kind.TYPE);
        reservedwords.put("float", IToken.Kind.TYPE);
        reservedwords.put("string", IToken.Kind.TYPE);
        reservedwords.put("boolean", IToken.Kind.TYPE);
        reservedwords.put("color", IToken.Kind.TYPE);
        reservedwords.put("image", IToken.Kind.TYPE);
        // Boolean values
        reservedwords.put("true", IToken.Kind.BOOLEAN_LIT);
        reservedwords.put("false", IToken.Kind.BOOLEAN_LIT);
        // Operators
        reservedwords.put("getRed", IToken.Kind.COLOR_OP);
        reservedwords.put("getGreen", IToken.Kind.COLOR_OP);
        reservedwords.put("getBlue", IToken.Kind.COLOR_OP);
        reservedwords.put("getWidth", IToken.Kind.IMAGE_OP);
        reservedwords.put("getHeight", IToken.Kind.IMAGE_OP);
        // Constants
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

    // ================================= //
    // ========== CONSTRUCTOR ========== //
    // ================================= //

    public Lexer(String input)
    {
        this.input = input;
        this.tokens = new ArrayList<>();
        this.index = 0;

        this.startPos = 0;
        this.startLine = 0;
        this.startCol = 0;
        this.currentPos = 0;
        this.currentLine = 0;
        this.currentCol = 0;

        // Starts all-at-once scanning
        scanFile();
    }


    // ========================================== //
    // ======ABSTRACT METHOD IMPLEMENTATIONS===== //
    // ========================================== //


    // Purpose: Consume next token, if valid
    @Override public IToken next() throws LexicalException
    {
        // Edge case: If we have already "consumed" EOF
        if(index == tokens.size())
        {
            // There are no other tokens to possibly read
            throw new LexicalException("ACCESSING OUT OF RANGE");
        }

        // Consume next token
        IToken next = tokens.get(index);
        index++;

        // Edge case: If its an error token
        if(next.getKind() == IToken.Kind.ERROR)
            // Throw an exception
            throw new LexicalException("ERROR", next.getSourceLocation());

        // Otherwise, it's a valid token...

        return next;
    }

    // Purpose: Read next token, if valid
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

        // Otherwise, it's a valid token...

        return next;
    }


    // ========================================== //
    // ========= SCANNING FUNCTIONALITY ========= //
    // ========================================== //

    // Purpose: Controller for the all-at-once scanning
    private void scanFile()
    {
        // While there is still another lexeme to analyze
        while(!isAtEnd())
            // Construct the appropriate token for the lexeme
            scanForToken();

        // Construct and append an EOF token
        tokens.add(new Token(IToken.Kind.EOF, "", new IToken.SourceLocation(currentLine, currentCol)));
    }

    // Purpose: Construct an appropriate token for the current lexeme (like State.START)
    private void scanForToken()
    {
        // Set start positions to track where lexeme begins
        startPos = currentPos;
        startLine = currentLine;
        startCol = currentCol;

        // Get first character of lexeme
        char c = scanNextChar();

        // Determine token kind based on first char(s) of lexeme
        switch (c)
        {
            // Case: Whitespace
            case '\r', '\t', ' ' : break;
            case '\n' : currentLine++; currentCol = 0; break;

            // Case: Comment
            case '#' : while(!isAtEnd() && peekNextChar() != '\n') scanNextChar(); break;

            // Case: Single-char lexeme that corresponds to exactly 1 token
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

            // Case: Single- or possibly double-char lexeme that corresponds to exactly 1 token
            case '-' :
                if(scanNextCharIfEquals('>')) createToken(IToken.Kind.RARROW);
                else createToken(IToken.Kind.MINUS); break;
            case '=' :
                if(scanNextCharIfEquals('=')) createToken(IToken.Kind.EQUALS);
                else createToken(IToken.Kind.ASSIGN); break;
            case '!' :
                if(scanNextCharIfEquals('=')) createToken(IToken.Kind.NOT_EQUALS);
                else createToken(IToken.Kind.BANG); break;
            case '>' :
                if(scanNextCharIfEquals('=')) createToken(IToken.Kind.GE);
                else if(scanNextCharIfEquals('>')) createToken(IToken.Kind.RANGLE);
                else createToken(IToken.Kind.GT); break;
            case '<':
                if(scanNextCharIfEquals('=')) createToken(IToken.Kind.LE);
                else if(scanNextCharIfEquals('<')) createToken(IToken.Kind.LANGLE);
                else if(scanNextCharIfEquals('-')) createToken(IToken.Kind.LARROW);
                else createToken(IToken.Kind.LT); break;

            // Case: String literal
            case '"' : inString(); break;

            // Case: Integer literal/Float literal
            case '1','2','3','4','5','6','7','8','9' : inNumber(); break;
            case '0' : if(peekNextChar() == '.') inNumber(); else createToken(IToken.Kind.INT_LIT); break;

            // Case: Identifier/Reserved Word or Error
            default :
                if (isAlpha(c)) inIdentifier();
                else createToken(IToken.Kind.ERROR); break;

        }
    }

    // Purpose: Consume next char
    private char scanNextChar()
    {
        char c = input.charAt(currentPos);
        currentPos++;
        currentCol++;
        return c;
    }

    // Purpose: Peek at next char
    private char peekNextChar()
    {
        if(isAtEnd()) return '\0';
        return input.charAt(currentPos);
    }

    // Purpose: Peek at next-next char
    private char peekNextNextChar()
    {
        if(currentPos + 1 >= input.length()) return '\0';
        return input.charAt(currentPos + 1);
    }

    // Purpose: Consume next char *if* it matches expected (kinda a combination of peeking and scan)
    private boolean scanNextCharIfEquals(char expected)
    {
        // If the next char is anything BUT expected
        // Stop and don't consume char
        if (isAtEnd()) return false;
        if (input.charAt(currentPos) != expected) return false;

        // Otherwise, consume char
        currentPos++; currentCol++;
        return true;
    }

    // Purpose: Create and add token
    private void createToken(IToken.Kind kind)
    {
        String text = input.substring(startPos, currentPos);
        IToken.SourceLocation startsrcloc = new IToken.SourceLocation(startLine, startCol);
        tokens.add(new Token(kind, text, startsrcloc));
    }


    // ========================================== //
    // ==========STATE-HANDLING HELPERS========== //
    // ========================================== //

    // Purpose: Handle IN_STRINGLIT state
    private void inString()
    {
        // Consume all chars that aren't "
        while(peekNextChar() != '\"' && !isAtEnd())
        {
            // Edge case: If we find the escape sequence \"
            if(peekNextChar() == '\\' && peekNextNextChar() == '\"')
                // Consume an extra character to make sure it's ignored
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
            // Token is an unterminated (we never found the closing ")
            createToken(IToken.Kind.ERROR);
            return;
        }

        // Otherwise, it's a string...

        // Consume closing "
        scanNextChar();
        createToken(IToken.Kind.STRING_LIT);
    }

    // Purpose: Handle IN_NUM state
    private void inNumber()
    {
        // Consume all numeric chars
        while(isDigit(peekNextChar()))
            scanNextChar();

        // If we find a following . then it's a float...
        if(peekNextChar() == '.' && isDigit(peekNextNextChar()))
        {
            // Consume .
            scanNextChar();

            // Consume all numeric chars in front of .
            while(isDigit(peekNextChar()))
                scanNextChar();

            // Edge Case: If float is too large (I can't be bothered to think of another way)
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

            createToken(IToken.Kind.FLOAT_LIT);
            return;
        }

        // Otherwise, it's an integer...

        // Edge Case: If integer is too large (also couldn't be bothered to think of a better way)
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

        createToken(IToken.Kind.INT_LIT);
    }

    // Purpose: Handle IN_IDENT state
    private void inIdentifier()
    {
        // Consume all alphanumeric chars
        while(isAlphaNumeric(peekNextChar()))
            scanNextChar();

        // Map lexeme to a reserved word
        String text = input.substring(startPos, currentPos);
        IToken.Kind kind = reservedwords.get(text);

        // If lexeme didn't map to a reserved word,
        if(kind == null)
            // The token is an identifier
            kind = IToken.Kind.IDENT;

        createToken(kind);
    }


    // ================================= //
    // ======== HELPER FUNCTIONS ======= //
    // ================================= //


    // Returns whether we've read through all characters in the input file
    private boolean isAtEnd()
    {
        return currentPos >= input.length();
    }

    // Returns whether a char is 0..9
    private boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    // Returns whether a char is a..z, A..Z, _, or $
    private boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_') ||
                (c == '$');
    }

    // Returns whether a char is 0..9, a..z, A..Z, _, or $
    private boolean isAlphaNumeric(char c)
    {
        return isAlpha(c) || isDigit(c);
    }
}
