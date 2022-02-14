package edu.ufl.cise.plc;

// TODO: Clean up these imports. Might be able to do .*
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.Expr;

import static edu.ufl.cise.plc.CompilerComponentFactory.getLexer;

import java.util.List;

public class Parser implements IParser {

    // ================================= //
    // =========== VARIABLES =========== //
    // ================================= //

    // Stores lexer object (to access token list)
    ILexer lexer;

    // Stores the current token we're working on
    IToken token;


    // ================================= //
    // ========== CONSTRUCTOR ========== //
    // ================================= //

    // Constructs the parser and a lexer instance
    Parser(String input) {
        lexer = CompilerComponentFactory.getLexer(input);
    }


    // ========================================== //
    // ======ABSTRACT METHOD IMPLEMENTATIONS===== //
    // ========================================== //

    // TODO: Implement parse()
    // Returns the AST
    @Override
    public ASTNode parse() throws PLCException {

    }

    // ========================================== //
    // ============ SCANNING HELPERS ============ //
    // ========================================== //

    // TODO: Decide whether it should throw PLCException or LexicalException (since .next() throws)
    // TODO: Decide if we even need this method, since it's just a wrapper for lexer's next() method
    // Consumes current token
    // Returns the next token
    private IToken advance() throws PLCException {
        return lexer.next();
    }

    // Peeks at next token
    // Returns the next token
    // TODO: Decide if we even need this method, since it's just a wrapper for lexer's peek() method
    private IToken peek() throws PLCException {
        return lexer.peek();
    }

    // Checks if current token matches the parameter AND THEN consumes it, if so
    // Returns false if not
    private boolean match(Kind kind) throws PLCException {
        if (kind == peek().getKind()) {
            advance();
            return true;
        }
        return false;
    }

    //TODO: Might need this previous function; to implement it, might need to add a previous() function to Lexer. See if you can do stuff without it for now?

    // Checks if current token is the last "real" token in the file
    // Returns false if not
    private boolean isAtEnd() throws PLCException {
        return peek().getKind() == Kind.EOF;
    }

    // ========================================== //
    // ============= AST GENERATORS ============= //
    // ========================================== //
    // TODO: Implement these methods to return the AST for the construct it represents, working from bottom of doc to top
    // TODO: Follow the structure of textbook's implementation for these (in Chapter 6)

    // PixelSelector ::= '[' Expr ',' Expr ']'
    Expr pixelselector() {

    }

    // PrimaryExpr ::= BOOLEAN_LIT  |  STRING_LIT  |  INT_LIT  |  FLOAT_LIT  |  IDENT  |  '(' Expr ')'
    Expr primaryexpr() {

    }

    // UnaryExprPostfix ::= PrimaryExpr PixelSelector?
    Expr unaryexprpostfix()
    {

    }

    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    Expr unaryexpr()
    {

    }

    // MultiplicativeExpr ::= UnaryExpr (('*'|'/' | '%') UnaryExpr)*
    Expr multiplicativeexpr()
    {

    }

    // AdditiveExpr ::= MultiplicativeExpr ( ('+'|'-') MultiplicativeExpr )*
    Expr additiveexpr()
    {

    }

    // ComparisonExpr ::= AdditiveExpr ( ('<' | '>' | '==' | '!=' | '<=' | '>=') AdditiveExpr)*
    Expr comparisonexpr()
    {

    }

    //LogicalAndExpr ::= ComparisonExpr ( '&'  ComparisonExpr)*
    Expr logicalandexpr()
    {

    }

    // LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    Expr logicalorexpr()
    {

    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    Expr conditionalexpr()
    {

    }

    // Expr::= ConditionalExpr | LogicalOrExpr
    Expr expr()
    {

    }
}
