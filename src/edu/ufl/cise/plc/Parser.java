package edu.ufl.cise.plc;

// TODO: Clean up these imports. Might be able to do .*
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.Expr;

import static edu.ufl.cise.plc.CompilerComponentFactory.getLexer;

import java.util.List;
import edu.ufl.cise.plc.ast.*;


public class Parser implements IParser {

    // ================================= //
    // =========== VARIABLES =========== //
    // ================================= //

    // Stores lexer object (to access token list)
    ILexer lexer;

    // Stores the current token we're working on
    IToken token;
   // Stores the current token position we're working on
    private int current = 0;

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
    	
    	//handles the first token 
    	
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

    //TODO: Might need a previous() function; to implement it, might need to add a previous() function to Lexer. See if you can do stuff without it for now?

    private Token previous() throws PLCException {
    	//it would peek the previous token 
        return lexer.get(current - 1);
     }
    
    private Token matchOrError(Kind kind, String message) throws PLCException {
        if (check(kind)) 
        	return advance();

        throw error(peek(), message);
      }
    
    // ========================================== //
    // ============= AST GENERATORS ============= //
    // ========================================== //
    // TODO: Implement these methods to return the AST for the construct it represents, working from bottom of doc to top
    // TODO: Follow the structure of textbook's implementation for these (in Chapter 6)

 
 
    
    
    /*
     * private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
  }
     */
  
    
     private ASTNode expression() throws PLCException  {
        return primaryexpr();
      }
    
    // PrimaryExpr ::= BOOLEAN_LIT  |  STRING_LIT  |  INT_LIT  |  FLOAT_LIT  |  IDENT  |  '(' Expr ')'
    ASTNode primaryexpr() throws PLCException {
    	
    	if (match(Kind.BOOLEAN_LIT)) return new BooleanLitExpr(token);
        if (match(Kind.STRING_LIT)) return new StringLitExpr(token);
        if (match(Kind.INT_LIT)) return new IntLitExpr(token);
        if (match(Kind.FLOAT_LIT)) return new FloatLitExpr(token);
        if (match(Kind.IDENT)) return new IdentExpr(token);
        
        
        		
        if (match(Kind.LPAREN)) 
        {
        	ASTNode expr = expression();
          //check if not match(Kind.RPAREN) ... then throw exception
         
          //kinda confused on how this works but this is the correct stuructue on the text book 
          matchOrError(Kind.RPAREN, "Expect ')' after expression.");
          
          return expr;
        }
        
        //else -> error 
        
        
        return null;
        
        	

    }
    
 // PixelSelector ::= '[' Expr ',' Expr ']'
    ASTNode pixelselector() throws PLCException {
    	
    	//should probably follow same strucute as text book 
    	
          matchOrError(Kind.LSQUARE, "Expect '[' after expression.");
          ASTNode expr =  expression();
          matchOrError(Kind.COMMA, "Expect ',' after expression.");
          ASTNode expr2 = expression();
          matchOrError(Kind.RSQUARE, "Expect ']' after expression.");
          
          
          return new PixelSelector(token, (Expr)expr, (Expr)expr2);
        
    	
    }


    // UnaryExprPostfix ::= PrimaryExpr PixelSelector?
    //
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

    //should be correct structure 
    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    ASTNode unaryexpr() throws PLCException 
    {
    
        if (match(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP)) 
        {
          IToken operator = previous();
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

        while (match(Kind.TIMES, Kind.DIV, Kind.MOD)) 
        {
          IToken operator = previous();
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

        while (match(Kind.PLUS, Kind.MINUS)) 
        {
          Token operator = previous();
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

        while (match(Kind.GT, Kind.LT, Kind.EQUALS, Kind.NOT_EQUALS, Kind.LE, Kind.GE)) 
        {
          Token operator = previous();
          ASTNode right = additiveexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    
    //LogicalAndExpr ::= ComparisonExpr ( '&'  ComparisonExpr)*
    ASTNode logicalandexpr() throws PLCException 
    {
    	ASTNode expr = comparisonexpr();

        while (match(Kind.AND)) 
        {
          Token operator = previous();
          ASTNode right = comparisonexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    	
    }

    // LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    ASTNode logicalorexpr() throws PLCException 
    {

    	ASTNode expr = logicalandexpr();

        while (match(Kind.OR)) 
        {
          Token operator = previous();
          ASTNode right = logicalandexpr();
          expr = new BinaryExpr(token, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    ASTNode conditionalexpr() throws PLCException 
    {
    	
        matchOrError(Kind.KW_IF, "Expect 'if' after expression.");
        matchOrError(Kind.LPAREN, "Expect '(' after expression.");
        ASTNode expr = expression();
        matchOrError(Kind.RPAREN, "Expect ')' after expression.");
        ASTNode expr2 = expression();  
        matchOrError(Kind.KW_ELSE, "Expect 'else' after expression.");
        ASTNode expr3 = expression();  
        matchOrError(Kind.KW_FI, "Expect 'fi' after expression.");
        
        return new ConditionalExpr(token, (Expr)expr, (Expr)expr2, (Expr)expr3);
    }

    //?
    // Expr::= ConditionalExpr | LogicalOrExpr
    ASTNode expr() throws PLCException 
    {
    	ASTNode expr = conditionalexpr;
    	
    	expr = logicalorexpr;

    }
}
