package edu.ufl.cise.plc;

import java.util.ArrayList;

public class Lexer implements ILexer {

    // ====== VARIABLES ====== //

    // The "source file"; the full, multi-line string named 'input' that she uses in her test cases
    String input;

    // Our storage structure for all tokens :)
    ArrayList<IToken> tokens;

    // The position of the next character to read when next() or peek() is called; incremented with each char we read
    int index;
    // The line of the next character to read when next() or peek() called; incremented with each \n char we read
    int line;
    // The col of the next char to read when next() or peek() called; reset to 0 with each \n we read, incremented with any other char we read
    int col;

    // Constructor
    public Lexer(String input)
    {
        this.input = input;
        this.index = 0;
        this.line = 0;
        this.col = 0;
    }

    // Reads next token; creates a Token out of them; adds this token to our storage structure; updates positions in file
    @Override public IToken next() throws LexicalException
    {
        /// TO-DO: DFA implementation
    }

    // Reads next token
    @Override public IToken peek() throws LexicalException
    {
        // save current position so that we can reset it later
        int tempindex = this.index;
        int templine = this.line;
        int tempcol = this.col;

        // call next()-- this will change index, line, & col, as well as append the new token to our storage
        IToken nexttoken = this.next();

        // reset the changes we made
        this.index = tempindex;
        this.line = templine;
        this.col = tempcol;
        tokens.remove(nexttoken);

        return nexttoken;
    }

}
