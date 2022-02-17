package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;


public class Parser implements IParser {

    // ================================= //
    // =========== VARIABLES =========== //
    // ================================= //

    // Stores lexer object (to access token list)
    ILexer lexer;

    // Stores the first token of the current phrase we're working on
    IToken token;

    // TODO: Storage for all AST nodes?


    // ================================= //
    // ========== CONSTRUCTOR ========== //
    // ================================= //

    // Constructs the parser and a lexer instance
    public Parser(String input)
    {
        lexer = CompilerComponentFactory.getLexer(input);
        token = null;
    }


    // ========================================== //
    // ======== INTERFACE IMPLEMENTATIONS ======= //
    // ========================================== //

    // Returns the (single expression's) AST TODO: parse() should eventually be able to handle more than one expression in the input
    @Override public ASTNode parse() throws PLCException
    {
    	return expression();
    }

    // ========================================== //
    // ============ CONSUMING TOKENS ============ //
    // ========================================== //

    // Consumes current token, advancing to next token
    // Returns the next token
    private IToken advance() throws PLCException {
        return lexer.next();
    }

    // Consumes current token, advancing to next token
    // Used for terminals that MUST be there (hence the "void" return type & exception throw)
    private void advanceIfMatches(Kind kind, String message) throws PLCException {
        if (peekMatches(kind)) advance();
        else throw new SyntaxException(message, token.getSourceLocation());
    }

    // ========================================== //
    // ============ PEEKING AT TOKENS =========== //
    // ========================================== //

    // Peeks at next token
    // Returns the next token
    private IToken peek() throws PLCException {
        return lexer.peek();
    }

    // Checks if current token matches any one of the parameters (kinds)
    // Returns false if not
    private boolean peekMatches(Kind... kinds) throws PLCException {
        if(isAtEnd()) return false;

        for (Kind kind : kinds)
        {
            if (peek().getKind() == kind) return true;
        }

        return false;
    }

    // Checks if current token is the last "real" token in the file
    // Returns false if not
    private boolean isAtEnd() throws PLCException {
        return peek().getKind() == Kind.EOF;
    }

    
    // ========================================== //
    // ============= AST GENERATORS ============= //
    // ========================================== //
    
    // PrimaryExpr ::= BOOLEAN_LIT  |  STRING_LIT  |  INT_LIT  |  FLOAT_LIT  |  IDENT  |  '(' Expr ')'
    private ASTNode primaryexpr() throws PLCException
    {
        // Token MUST be one of these terminals
        if (peekMatches(Kind.BOOLEAN_LIT)) return new BooleanLitExpr(advance());
        if (peekMatches(Kind.STRING_LIT)) return new StringLitExpr(advance());
        if (peekMatches(Kind.INT_LIT)) return new IntLitExpr(advance());
        if (peekMatches(Kind.FLOAT_LIT)) return new FloatLitExpr(advance());
        if (peekMatches(Kind.IDENT)) return new IdentExpr(advance());

        advanceIfMatches(Kind.LPAREN, "Expected '(' or primary expression.");
        ASTNode expr = expression();
        advanceIfMatches(Kind.RPAREN, "Expected ')' after expression.");
        return expr;
    }
    
    // PixelSelector ::= '[' Expr ',' Expr ']'
    private ASTNode pixelselector() throws PLCException
    {
        advanceIfMatches(Kind.LSQUARE, "Expected '[' before expression.");
        ASTNode expr =  expression();
        advanceIfMatches(Kind.COMMA, "Expected ',' between expressions.");
        ASTNode expr2 = expression();
        advanceIfMatches(Kind.RSQUARE, "Expected ']' after expression.");
        return new PixelSelector(token, (Expr)expr, (Expr)expr2);
    }

    // UnaryExprPostfix ::= PrimaryExpr PixelSelector?
    private ASTNode unaryexprpostfix() throws PLCException
    {
    	ASTNode expr = primaryexpr();

    	if(peekMatches(Kind.LSQUARE))
    	{
    		ASTNode expr2 = pixelselector();
    		expr = new UnaryExprPostfix(token, (Expr)expr, (PixelSelector)expr2);
    	}

    	return expr;
    }

    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    private ASTNode unaryexpr() throws PLCException
    {
        if (peekMatches(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP))
        {
            IToken operator = advance(); // Advance and store ! - COLOR_OP or IMAGE_OP
            ASTNode right = unaryexpr();
            return new UnaryExpr(token, operator, (Expr)right);
        }
       
        return unaryexprpostfix();
    }

    // MultiplicativeExpr ::= UnaryExpr (('*'|'/' | '%') UnaryExpr)*
    private ASTNode multiplicativeexpr() throws PLCException
    {
    	ASTNode expr = unaryexpr();

        while (peekMatches(Kind.TIMES, Kind.DIV, Kind.MOD))
        {
            IToken operator = advance(); // Advance and store the * / or %
            ASTNode right = unaryexpr();
            expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // AdditiveExpr ::= MultiplicativeExpr ( ('+'|'-') MultiplicativeExpr )*
    private ASTNode additiveexpr() throws PLCException
    {
    	ASTNode expr = multiplicativeexpr();

        while (peekMatches(Kind.PLUS, Kind.MINUS))
        {
            IToken operator = advance(); // Advance and store the + or -
            ASTNode right = multiplicativeexpr();
            expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ComparisonExpr ::= AdditiveExpr ( ('<' | '>' | '==' | '!=' | '<=' | '>=') AdditiveExpr)*
    private ASTNode comparisonexpr() throws PLCException
    {
    	ASTNode expr = additiveexpr();

        while (peekMatches(Kind.GT, Kind.LT, Kind.EQUALS, Kind.NOT_EQUALS, Kind.LE, Kind.GE))
        {
            IToken operator = advance(); // Advance and store the > < == != <= or >=
            ASTNode right = additiveexpr();
            expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    //LogicalAndExpr ::= ComparisonExpr ( '&'  ComparisonExpr)*
    private ASTNode logicalandexpr() throws PLCException
    {
    	ASTNode expr = comparisonexpr();

        while (peekMatches(Kind.AND))
        {
            IToken operator = advance(); // Advance and store the & (this version didn't need changing, but i did for consistency)
            ASTNode right = comparisonexpr();
            expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    private ASTNode logicalorexpr() throws PLCException
    {
    	ASTNode expr = logicalandexpr();

        while (peekMatches(Kind.OR))
        {
            IToken operator = advance(); // Advance and store the | (this version didn't need changing, but i did for consistency)
            ASTNode right = logicalandexpr();
            expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    private ASTNode conditionalexpr() throws PLCException
    {
        advanceIfMatches(Kind.KW_IF, "Expected 'if' after expression.");
        advanceIfMatches(Kind.LPAREN, "Expected '(' after expression.");
        ASTNode expr = expression();
        advanceIfMatches(Kind.RPAREN, "Expected ')' after expression.");
        ASTNode expr2 = expression();
        advanceIfMatches(Kind.KW_ELSE, "Expected 'else' after expression.");
        ASTNode expr3 = expression();
        advanceIfMatches(Kind.KW_FI, "Expected 'fi' after expression.");
        return new ConditionalExpr(token, (Expr)expr, (Expr)expr2, (Expr)expr3);
    }

    // Expr::= ConditionalExpr | LogicalOrExpr
    private ASTNode expression() throws PLCException
    {
        token = peek();
        if(peekMatches(Kind.KW_IF)) return conditionalexpr();
        else if(peekMatches(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP, Kind.BOOLEAN_LIT, Kind.STRING_LIT, Kind.INT_LIT, Kind.FLOAT_LIT, Kind.IDENT, Kind.LPAREN)) return logicalorexpr();
        else throw new SyntaxException("Expected expression", token.getSourceLocation());
    }
}
