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

    // TODO: Do we need to store all the ASTs? How do we combine them into one?
    // Stores AST nodes
    ArrayList<ASTNode> asts;


    // ================================= //
    // ========== CONSTRUCTOR ========== //
    // ================================= //

    // Constructs the parser and a lexer instance
    Parser(String input)
    {
        lexer = CompilerComponentFactory.getLexer(input);
        token = null;
        asts = new ArrayList<>();
    }


    // ========================================== //
    // ======ABSTRACT METHOD IMPLEMENTATIONS===== //
    // ========================================== //

    // TODO: Implement parse()
    // Returns the AST
    @Override public ASTNode parse() throws PLCException
    {
    	while(!isAtEnd())
            asts.add(expression());

        //i think we're only making one ast for now, so ill just return the first
        return asts.get(0);
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

    // Checks if current token matches any one of the parameters (kinds) AND THEN consumes it, if so
    // Returns false if not
    private boolean match(Kind... kinds) throws PLCException {
        for (Kind kind : kinds) 
        {
            if (check(kind)) 
            {
                advance();
                return true;
            }
        }
        
        return false;
    }

    // Checks if current token kind matches the parameter (kind)
    // Returns false if not
    private boolean check(Kind kind) throws PLCException {
        if (isAtEnd()) return false;
        return peek().getKind() == kind;
    }

    // Checks if current token is the last "real" token in the file
    // Returns false if not
    private boolean isAtEnd() throws PLCException {
        return peek().getKind() == Kind.EOF;
    }
    
    private IToken matchOrError(Kind kind, String message) throws PLCException {
        if (check(kind)) 
        	return advance();

        throw new SyntaxException(message, token.getSourceLocation());
      }
    
    // ========================================== //
    // ============= AST GENERATORS ============= //
    // ========================================== //
    
    // PrimaryExpr ::= BOOLEAN_LIT  |  STRING_LIT  |  INT_LIT  |  FLOAT_LIT  |  IDENT  |  '(' Expr ')'
    ASTNode primaryexpr() throws PLCException {

        IToken current = peek();
        switch(current.getKind())
        {
            case BOOLEAN_LIT : advance(); return new BooleanLitExpr(current);
            case STRING_LIT : advance(); return new StringLitExpr(current);
            case INT_LIT : advance(); return new IntLitExpr(current);
            case FLOAT_LIT : advance(); return new FloatLitExpr(current);
            case IDENT : advance(); return new IdentExpr(current);
            case LPAREN :
                ASTNode expr = expression();
                matchOrError(Kind.RPAREN, "Expected ')' after expression.");
                return expr;
            default :
                throw new SyntaxException("Expected primary expression", current.getSourceLocation());
        }
    }
    
 // PixelSelector ::= '[' Expr ',' Expr ']'
    ASTNode pixelselector() throws PLCException {
          matchOrError(Kind.LSQUARE, "Expected '[' after expression.");
          ASTNode expr =  expression();
          matchOrError(Kind.COMMA, "Expected ',' after expression.");
          ASTNode expr2 = expression();
          matchOrError(Kind.RSQUARE, "Expected ']' after expression.");
          return new PixelSelector(token, (Expr)expr, (Expr)expr2);
    }


    // UnaryExprPostfix ::= PrimaryExpr PixelSelector?
    ASTNode unaryexprpostfix() throws PLCException
    {
    	ASTNode expr = primaryexpr();

    	if(peek().getKind() == Kind.LSQUARE)
    	{
    		ASTNode expr2 = pixelselector();
    		expr = new UnaryExprPostfix(token, (Expr)expr, (PixelSelector)expr2);
    	}

    	return expr;
    }

    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    ASTNode unaryexpr() throws PLCException 
    {
        IToken operator = peek();
        if (match(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP)) 
        {
          ASTNode right = unaryexpr();
          return new UnaryExpr(token, operator, (Expr)right);
        }
       
        return unaryexprpostfix();
    }

    
    //should be correct structure 
    // MultiplicativeExpr ::= UnaryExpr (('*'|'/' | '%') UnaryExpr)*
    ASTNode multiplicativeexpr() throws PLCException
    {
    	ASTNode expr = unaryexpr();

        IToken operator = peek();
        while (match(Kind.TIMES, Kind.DIV, Kind.MOD)) 
        {
          ASTNode right = unaryexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    //should be correct structure 
    // AdditiveExpr ::= MultiplicativeExpr ( ('+'|'-') MultiplicativeExpr )*
    ASTNode additiveexpr() throws PLCException 
    {
    	ASTNode expr = multiplicativeexpr();

        IToken operator = peek();
        while (match(Kind.PLUS, Kind.MINUS)) 
        {
          ASTNode right = multiplicativeexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    //should be correct structure 
    // ComparisonExpr ::= AdditiveExpr ( ('<' | '>' | '==' | '!=' | '<=' | '>=') AdditiveExpr)*
    ASTNode comparisonexpr() throws PLCException 
    {
    	ASTNode expr = additiveexpr();

        IToken operator = peek();
        while (match(Kind.GT, Kind.LT, Kind.EQUALS, Kind.NOT_EQUALS, Kind.LE, Kind.GE)) 
        {
          ASTNode right = additiveexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    
    //LogicalAndExpr ::= ComparisonExpr ( '&'  ComparisonExpr)*
    ASTNode logicalandexpr() throws PLCException 
    {
    	ASTNode expr = comparisonexpr();

        IToken operator = peek();
        while (match(Kind.AND)) 
        {
          ASTNode right = comparisonexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    	
    }

    // LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    ASTNode logicalorexpr() throws PLCException 
    {
    	ASTNode expr = logicalandexpr();

        IToken operator = peek();
        while (match(Kind.OR)) 
        {
          ASTNode right = logicalandexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    ASTNode conditionalexpr() throws PLCException 
    {
        matchOrError(Kind.KW_IF, "Expected 'if' after expression.");
        matchOrError(Kind.LPAREN, "Expected '(' after expression.");
        ASTNode expr = expression();
        matchOrError(Kind.RPAREN, "Expected ')' after expression.");
        ASTNode expr2 = expression();  
        matchOrError(Kind.KW_ELSE, "Expected 'else' after expression.");
        ASTNode expr3 = expression();  
        matchOrError(Kind.KW_FI, "Expected 'fi' after expression.");
        return new ConditionalExpr(token, (Expr)expr, (Expr)expr2, (Expr)expr3);
    }

    // Expr::= ConditionalExpr | LogicalOrExpr
    ASTNode expression() throws PLCException
    {
        token = peek();  //Peek next token, which will be the first token of expression
    	switch(token.getKind())
        {
            case KW_IF : return conditionalexpr();
            case BANG, MINUS, COLOR_OP, IMAGE_OP, BOOLEAN_LIT, STRING_LIT, INT_LIT , FLOAT_LIT ,IDENT , LPAREN : return logicalorexpr();
            default : throw new SyntaxException("Expected expression start", token.getSourceLocation());
        }
    }
}
