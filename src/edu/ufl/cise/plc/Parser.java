package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import java.util.ArrayList;

public class Parser implements IParser {

    // ================================= //
    // =========== VARIABLES =========== //
    // ================================= //

    // Stores lexer object (to access token list)
    ILexer lexer;


    // ================================= //
    // ========== CONSTRUCTOR ========== //
    // ================================= //

    // Constructs the parser and a lexer instance
    public Parser(String input)
    {
        lexer = CompilerComponentFactory.getLexer(input);
    }


    // ========================================== //
    // ======== INTERFACE IMPLEMENTATIONS ======= //
    // ========================================== //

    // Returns the program's AST
    @Override public ASTNode parse() throws PLCException
    {
        // Parse the program
    	ASTNode program = program();
        // If the next token isn't EOF, then there are rogue, trailing tokens
        if(!isAtEnd()) throw new SyntaxException("Trailing tokens", peek().getSourceLocation());
        return program;
    }

    // ========================================== //
    // ============ CONSUMING TOKENS ============ //
    // ========================================== //

    // Consumes current token, advancing to next token
    // Returns the next token
    private IToken advance() throws PLCException {
        // If the next token isn't EOF, we continue
        if(!isAtEnd()) return lexer.next();
        // Else, our phrase is incomplete
        throw new SyntaxException("Incomplete phrase", lexer.peek().getSourceLocation());
    }

    // Consumes current token, advancing to next token
    // Used for terminals that MUST be there (hence the "void" return type & exception throw)
    private IToken advanceIfMatches(Kind kind, String message) throws PLCException {
        // If the next token matches, then we continue
        if (!isAtEnd() && peekMatches(kind)) return lexer.next();
        // Else, our phrase is missing the needed terminal and/or is incomplete
        throw new SyntaxException(message, peek().getSourceLocation());
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
            if (lexer.peek().getKind() == kind) return true;
        }
        return false;
    }

    // Checks if current token is the last "real" token in the file
    // Returns false if not
    private boolean isAtEnd() throws PLCException {
        return lexer.peek().getKind() == Kind.EOF;
    }

    
    // ========================================== //
    // ============= AST GENERATORS ============= //
    // ========================================== //
    

    // PrimaryExpr ::= BOOLEAN_LIT  |  STRING_LIT  |  INT_LIT  |  FLOAT_LIT  |  IDENT  |  '(' Expr ')'
    //                 | ColorConst | '<<' Expr ',' Expr ',' Expr '>>' | 'console'
    private ASTNode primaryexpr() throws PLCException
    {
        IToken firsttoken = peek();

        // Token MUST be one of these terminals
        if (peekMatches(Kind.BOOLEAN_LIT)) return new BooleanLitExpr(advance());
        if (peekMatches(Kind.STRING_LIT)) return new StringLitExpr(advance());
        if (peekMatches(Kind.INT_LIT)) return new IntLitExpr(advance());
        if (peekMatches(Kind.FLOAT_LIT)) return new FloatLitExpr(advance());
        if (peekMatches(Kind.IDENT)) return new IdentExpr(advance());
        if (peekMatches(Kind.COLOR_CONST)) return new ColorConstExpr(advance());
        if (peekMatches(Kind.KW_CONSOLE)) return new ConsoleExpr(advance());
        if(peekMatches(Kind.LPAREN))
        {
            //checking: '(' Expr ')'
        	advanceIfMatches(Kind.LPAREN, "Expected '(' or primary expression.");
            ASTNode expr = expression();
            advanceIfMatches(Kind.RPAREN, "Expected ')' after expression.");
            return expr;
        }
        if(peekMatches(Kind.LANGLE))
        {
            //checking: '<<' Expr ',' Expr ',' Expr '>>'
            advanceIfMatches(Kind.LANGLE, "Expected '<<'");
            ASTNode red = expression();
            advanceIfMatches(Kind.COMMA, "Expected ','");
            ASTNode green = expression();
            advanceIfMatches(Kind.COMMA, "Expected ','");
            ASTNode blue = expression();
            advanceIfMatches(Kind.RANGLE, "Expected '>>'");
            return new ColorExpr(firsttoken, (Expr) red, (Expr) green, (Expr) blue);
        }
        throw new SyntaxException("Expected primary expression", firsttoken.getSourceLocation());
    }
    
    //Statement::=
	//IDENT PixelSelector? '=' Expr |               //yields AssignmentStatement
	//IDENT PixelSelector? ‘<-’ Expr |              //yields  ReadStatement
    //'write' Expr '->' Expr |                      //yields WriteStatement
	//'^' Expr                                      //yields ReturnStatement
    private ASTNode statement() throws PLCException
    {
        IToken firsttoken = peek();

    	String name;
    	PixelSelector selector = null;

    	//Checks: IDENT PixelSelector? ('=' | '<-') Expr
    	if (peekMatches(Kind.IDENT)) 
        {
    		name = advance().getText();
    		
    		//Checks: PixelSelector?
    		if(peekMatches(Kind.LSQUARE))
        	{
    			selector = (PixelSelector)pixelselector();
        	}
    		//checks if '=' 
    		if(peekMatches(Kind.ASSIGN))
    		{
    			IToken operator = advance(); //stores '='
    			ASTNode expr =  expression();
    			return new AssignmentStatement(firsttoken, name, selector, (Expr)expr);
    		}
    		
    		//checks if ‘<-’
    		if(peekMatches(Kind.LARROW))
    		{
    			IToken operator = advance(); //stores ' <- '
    			ASTNode expr =  expression();
    			return new ReadStatement(firsttoken, name, selector,(Expr)expr);
    		}
    		
        }
    	//Checks:  'write' Expr '->' Expr
    	if(peekMatches(Kind.KW_WRITE))
    	{
    		advance();
    		ASTNode source =  expression();
    		advanceIfMatches(Kind.RARROW, "Expected '->'");
            ASTNode dest =  expression();
           return new WriteStatement(firsttoken, (Expr)source,  (Expr)dest);
    	}
        //Checks: '^' Expr
        if(peekMatches(Kind.RETURN))
        {
            advanceIfMatches(Kind.RETURN, "Expected '^'");
            ASTNode expr = expression();
            return new ReturnStatement(firsttoken, (Expr) expr);
        }
        throw new SyntaxException("Expected statement", firsttoken.getSourceLocation());
    }
    
    
    //Dimension ::= '[' Expr ',' Expr ']'
    private ASTNode dimension() throws PLCException
    {
        IToken firsttoken = peek();

        advanceIfMatches(Kind.LSQUARE, "Expected '[' before expression.");
        ASTNode expr =  expression();
        advanceIfMatches(Kind.COMMA, "Expected ',' between expressions.");
        ASTNode expr2 = expression();
        advanceIfMatches(Kind.RSQUARE, "Expected ']' after expression.");
        return new Dimension(firsttoken, (Expr)expr, (Expr)expr2);
    }
    
    
    // PixelSelector ::= '[' Expr ',' Expr ']'
    private ASTNode pixelselector() throws PLCException
    {
        IToken firsttoken = peek();

        advanceIfMatches(Kind.LSQUARE, "Expected '[' before expression.");
        ASTNode expr =  expression();
        advanceIfMatches(Kind.COMMA, "Expected ',' between expressions.");
        ASTNode expr2 = expression();
        advanceIfMatches(Kind.RSQUARE, "Expected ']' after expression.");
        return new PixelSelector(firsttoken, (Expr)expr, (Expr)expr2);
    }

    // UnaryExprPostfix ::= PrimaryExpr PixelSelector?
    private ASTNode unaryexprpostfix() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = primaryexpr();

    	if(peekMatches(Kind.LSQUARE))
    	{
    		ASTNode expr2 = pixelselector();
    		expr = new UnaryExprPostfix(firsttoken, (Expr)expr, (PixelSelector)expr2);
    	}

    	return expr;
    }

    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    private ASTNode unaryexpr() throws PLCException
    {
        IToken firsttoken = peek();

        if (peekMatches(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP))
        {
            IToken operator = advance(); // Advance and store ! - COLOR_OP or IMAGE_OP
            ASTNode right = unaryexpr();
            return new UnaryExpr(firsttoken, operator, (Expr)right);
        }
       
        return unaryexprpostfix();
    }

    // MultiplicativeExpr ::= UnaryExpr (('*'|'/' | '%') UnaryExpr)*
    private ASTNode multiplicativeexpr() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = unaryexpr();

        while (peekMatches(Kind.TIMES, Kind.DIV, Kind.MOD))
        {
            IToken operator = advance(); // Advance and store the * / or %
            ASTNode right = unaryexpr();
            expr = new BinaryExpr(firsttoken, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // AdditiveExpr ::= MultiplicativeExpr ( ('+'|'-') MultiplicativeExpr )*
    private ASTNode additiveexpr() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = multiplicativeexpr();

        while (peekMatches(Kind.PLUS, Kind.MINUS))
        {
            IToken operator = advance(); // Advance and store the + or -
            ASTNode right = multiplicativeexpr();
            expr = new BinaryExpr(firsttoken, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ComparisonExpr ::= AdditiveExpr ( ('<' | '>' | '==' | '!=' | '<=' | '>=') AdditiveExpr)*
    private ASTNode comparisonexpr() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = additiveexpr();

        while (peekMatches(Kind.GT, Kind.LT, Kind.EQUALS, Kind.NOT_EQUALS, Kind.LE, Kind.GE))
        {
            IToken operator = advance();
            ASTNode right = additiveexpr();
            expr = new BinaryExpr(firsttoken, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    //LogicalAndExpr ::= ComparisonExpr ( '&'  ComparisonExpr)*
    private ASTNode logicalandexpr() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = comparisonexpr();

        while (peekMatches(Kind.AND))
        {
            IToken operator = advance();
            ASTNode right = comparisonexpr();
            expr = new BinaryExpr(firsttoken, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    private ASTNode logicalorexpr() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = logicalandexpr();

        while (peekMatches(Kind.OR))
        {
            IToken operator = advance();
            ASTNode right = logicalandexpr();
            expr = new BinaryExpr(firsttoken, (Expr)expr, operator, (Expr)right);
        }

        return expr;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    private ASTNode conditionalexpr() throws PLCException
    {
        IToken firsttoken = peek();

        advanceIfMatches(Kind.KW_IF, "Expected 'if'");
        advanceIfMatches(Kind.LPAREN, "Expected '(' after 'if'");
        ASTNode expr = expression();
        advanceIfMatches(Kind.RPAREN, "Expected ')' after condition.");
        ASTNode expr2 = expression();
        advanceIfMatches(Kind.KW_ELSE, "Expected 'else' after true case.");
        ASTNode expr3 = expression();
        advanceIfMatches(Kind.KW_FI, "Expected 'fi' after false case.");
        return new ConditionalExpr(firsttoken, (Expr)expr, (Expr)expr2, (Expr)expr3);
    }

    // Expr::= ConditionalExpr | LogicalOrExpr
    private ASTNode expression() throws PLCException
    {
        IToken firsttoken = peek();
        if(peekMatches(Kind.KW_IF)) return conditionalexpr();
        else if(peekMatches(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP, Kind.BOOLEAN_LIT, Kind.STRING_LIT, Kind.INT_LIT, Kind.FLOAT_LIT, Kind.IDENT, Kind.LPAREN, Kind.KW_CONSOLE, Kind.COLOR_CONST, Kind.LANGLE)) return logicalorexpr();
        else throw new SyntaxException("Expected expression", firsttoken.getSourceLocation());
    }

	//NameDef ::= Type IDENT |                             //yields NameDef
	//Type Dimension IDENT                                //yields  NameDefWithDimension
    private ASTNode namedef() throws PLCException
    {
            IToken firsttoken = peek();

    		//check for Type 
    		String type = advanceIfMatches(Kind.TYPE, "Expected type").getText();

        	//check for IDENT and return 
            if (peekMatches(Kind.IDENT)) 
            {
            	String name = advance().getText();
            	return new NameDef(firsttoken, type, name);
            }
            
            //Check for Dimension
        	ASTNode expr = dimension();
            String name = advanceIfMatches(Kind.IDENT, "Expected identifier").getText();
            return new NameDefWithDim(firsttoken, type, name, (Dimension)expr);

    }
    

	//(Var)Declaration ::= NameDef (('=' | '<-') Expr)?
    private ASTNode vardeclaration() throws PLCException
    {
        IToken firsttoken = peek();

    	ASTNode expr = namedef();

        if (peekMatches(Kind.ASSIGN, Kind.LARROW))
        {
            IToken operator = advance();
            ASTNode right = expression();
            return new VarDeclaration(firsttoken, (NameDef)expr, operator, (Expr)right);
        }

        return new VarDeclaration(firsttoken, (NameDef)expr, null, null);
    }

    //(Type  | 'void') IDENT '(' (NameDef ( ',' NameDef)* )? ')' (  Declaration ';' | Statement ';'  )*    
    private ASTNode program() throws PLCException
    {
        IToken firsttoken = peek(); //Update current first token

    	if (peekMatches(Kind.TYPE,Kind.KW_VOID))
    	{
            Type returnType = Type.toType(advance().getText());
        	String name = advanceIfMatches(Kind.IDENT, "Expected 'IDENT' after expression.").getText();

            // '(' (NameDef ( ',' NameDef)* )? ')'
            advanceIfMatches(Kind.LPAREN, "Expected '(' after expression.");
            ArrayList<NameDef> params = new ArrayList<>();
            if(peekMatches(Kind.TYPE))
            {
                params.add((NameDef)namedef());
                while (peekMatches(Kind.COMMA)) {
                    advance();
                    //recheck for NameDef and assign expr to appropriate expression
                    params.add((NameDef)namedef());
                }
            }
            advanceIfMatches(Kind.RPAREN, "Expected ')' after expression.");

            // (  Declaration ';' | Statement ';'  )*
            ArrayList<ASTNode> decsAndStatements = new ArrayList<>();
            while(peekMatches(Kind.TYPE, Kind.KW_WRITE, Kind.IDENT, Kind.RETURN))
            {
                //first(declaration) = type
                if(peekMatches(Kind.TYPE)) decsAndStatements.add(vardeclaration());
                //first(statement) = write, IDENT, ^
                else decsAndStatements.add(statement());

            	advanceIfMatches(Kind.SEMI, "Expected ';' after expression.");
            }
            
            return new Program(firsttoken, returnType, name, params, decsAndStatements);
    	}
        else throw new SyntaxException("Expected return type.", firsttoken.getSourceLocation());
    }
}
