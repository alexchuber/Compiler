package edu.ufl.cise.plc;

import java.awt.image.BufferedImage;
import java.util.List;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.*;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.PLCLangExec;
import edu.ufl.cise.plc.ast.Types.Type;

import static edu.ufl.cise.plc.ast.Types.Type.*;

/*
TODO:
Implement a CodeGenVisitor to generate code for the parts of the language given in the table.  
The visitProgram method should return a String that contains the generated Java source code. 
The CodeGenVisitor constructor should accept a String with the package name as a parameter.
Add a method  with this signature and appropriate body to your CompilerComponentFactory
 */

public class CodeGenVisitor implements ASTVisitor {

	String pkgdec;

	public CodeGenVisitor(String pkgdec) {
		this.pkgdec = pkgdec;
	}

	@Override
	/* <package declaration>
	<imports>
	public class <name> {
	   public static <returnType> apply( <params> ){
	        <decsAndStatements>
	   }
	} */
	public Object visitProgram(Program program, Object arg) throws Exception {
		arg = new CodeGenStringBuilder();
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		((CodeGenStringBuilder) arg).append("package ").append(pkgdec).semi().newline();
		//TODO may need to fix imports
		((CodeGenStringBuilder) arg).append("import ").append("edu.ufl.cise.plc.runtime.ConsoleIO").semi();
		((CodeGenStringBuilder) arg).append("public class ").append(program.getName()).clparen().newline();
		((CodeGenStringBuilder) arg).append("public static ").append(program.getReturnType()).append(" apply").lparen();
		// <params>
		List<NameDef> params = program.getParams();
		for(int i = 0; i < params.size(); i++)
		{
			params.get(i).visit(this, arg);
			if(i < params.size()-1)
				((CodeGenStringBuilder) arg).comma();
		}
		((CodeGenStringBuilder) arg).rparen().clparen().newline();

		// <decsAndStatements>
		List<ASTNode> decsandstatements = program.getDecsAndStatements();
		for(int i = 0; i < decsandstatements.size(); i++) {
			decsandstatements.get(i).visit(this, arg);
			((CodeGenStringBuilder) arg).newline();
		}

		((CodeGenStringBuilder) arg).crparen().newline().crparen();
		return ((CodeGenStringBuilder) arg).getStringBuilder().toString();
		}

	@Override
	//<type> <name>
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append(nameDef.getType())
				.space()
				.append(nameDef.getName());
		return arg;
		}

	@Override
	//Not needed for assignment 5
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	// <nameDef> ;
	// Or if  this  has an assignment or read initializer
	// <nameDef> = <expr>
	//(only read initializers from console for assignment 5)
	
	//TODO: 
	//If the PLCLang type is image, implement with a java.awt.image.BufferedImage.
	// implement the table from the google doc 
	// If the PLCLang type is color, implement with edu.ufl.cise.plc.runtime.ColorTuple.

	/*
	 * @Override
	//BufferedImage <name> = new BufferedImage(<visitDim>, BufferedImage.TYPE_INT_RGB)
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append("BufferedImage ")
				.append(nameDefWithDim.getName())
				.assign()
				.append("new BufferedImage")
				.lparen();

		nameDefWithDim.getDim().visit(this, arg);

		((CodeGenStringBuilder)arg)
				.comma()
				.append("BufferedImage.TYPE_INT_RGB")
				.rparen();

		return arg;
	}
	 */
	
	
	
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		NameDef namedef = declaration.getNameDef();
		Expr expr = declaration.getExpr();
		IToken op = declaration.getOp();

		namedef.visit(this, arg);

		//If there's a RHS
		if(expr != null) 
		{
			((CodeGenStringBuilder)arg).assign();

			//Check for cast
			Type coerceTo = expr.getCoerceTo();
			Type type = expr.getType();
			if (coerceTo != null && type != coerceTo) {
				((CodeGenStringBuilder)arg)
						.lparen()
						.append(coerceTo)
						.rparen();
			}
			

			//Check read or assign... actually this needs refactoring but i dont have the will
			if(op.getKind() == Kind.ASSIGN) {
				expr.visit(this, arg);
			}
			if(op.getKind() == Kind.LARROW) {
				if(expr.getType() != CONSOLE)
				{
					//If the PLCLang type is image
					if(namedef.getType() == IMAGE) // implement with a java.awt.image.BufferedImage.
					{
						//boolean VERBOSE;
						//PLCLangExec("java.awt.image.BufferedImage", VERBOSE);
						
						if(namedef.isInitialized()) //to check if it has initalizer 
						{
							if(namedef.getDim() != null)//has dimension ->//visit the source to get the relevant dimensions 
							{
								int height = namedef.getDim().getHeight();
								int width = namedef.getDim().getWidth();
								String url = expr.getText(); 
								
								//Read image using readImage(String,int,int) method in FileURLIO
								 BufferedImage b = FileURLIO.readImage(url,width,height);
							     FileURLIO.closeFiles();
							     return b; //return? 
							}
							else //does not have dimension 
							{
								//Read image using readImage(String) method in FileURLIO
								String url = expr.getText(); 
								BufferedImage b = FileURLIO.readImage(url);
							    FileURLIO.closeFiles();
							    return b; //?
								
							}
						}
						
					}
					else if(namedef.visit(this, arg).equals(COLOR)) //edu.ufl.cise.plc.runtime.ColorTuple.
					{
						//same code
					}
				}
				
				//add else statement? 
				expr.visit(this, arg); 
			}
		}
		else
		{
			if(namedef.getDim() != null)//has dimension ->//visit the source to get the relevant dimensions 
				{
					//Instantiate a new BufferedImage of the given dimension and type BufferedImage.TYPE_INT_RGB
					namedef.visit(this, arg);
					
			
				}
				else //does not have dimension 
				{
					throw new UnsupportedOperationException("This case should have been marked as error during type checking");

				}
				
		
		}

		((CodeGenStringBuilder)arg).semi();

		return arg;
		}

	@Override
	//TODO: The expression will be something like a[e0,e1] where a is an image and [e0,e1] is represented by a PixelSelector.  
	// Invoke the BufferedImage getRGB method with the expressions in the PixelSelector as parameters and unpack the returned int to create a ColorTuple
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	// ( <condition> ) ? <trueCase> : <falseCase>
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Expr condition = conditionalExpr.getCondition();
		Expr truecase = conditionalExpr.getTrueCase();
		Expr falsecase = conditionalExpr.getFalseCase();

		((CodeGenStringBuilder)arg)
				.lparen()
				.lparen();
		condition.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.rparen()
				.question();
		truecase.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.colon();
		falsecase.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.rparen();
		return arg;
		}

	@Override
	//TODO: Usually, the width and height are used as parameters, for example to the BufferedImage constructor.  
	// It is convenient for the visit method to generate code to evaluate width expression, comma, code to evaluate height expression.
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//TODO: Handled differently depending on whether they are on the left or right side of expression.   
	// If on the left side, they are the index variables of a nested for loop.  
	// If they are on the right side, they will be used as parameters and generate code to evaluate X, comma, code to evaluate Y.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		// If on the left side, they are the index variables of a nested for loop.
		
		//passing pixelselector a bool along with sb to determine if it's lhs or rhs
		//Otherwise you can just choose one or the other and do the other code in whatever calling pixelselector
		//But that's a lot of copy pasting code so I wouldn't recommend it
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		
	
		//if LHS
		for(int i = 0; i < pixelSelector.getX().; i++)
		{
			
		}
		// If they are on the right side, they will be used as parameters and generate code to evaluate X, comma, code to evaluate Y.
		
	  
	}

	@Override
	//( <left> <op> <right> )
	
	/*
	TODO: 
	BinaryExpressions on color objects are performed componentwise.  There are routines in edu.ufl.cise.plc.runtime.ImageOps that might be useful. 

	BinaryExpressions on image objects are performed pixelwise.  There are routines in edu.ufl.cise.plc.runtime.ImageOps that might be useful. 

	If a binary operation has one image and a color,  apply the operation between pixel and color to all operations in the image.  If a binary operation involves an image an int, create a color with all color components equal to the int value and then apply pixelwise to the image. In other words for image im0 and int k =,   im0 * k = im0 * <<k,k,k>>.  (There is a ColorTuple constructor for this case)
	 */
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Type coerceTo = binaryExpr.getCoerceTo();
		Type type = binaryExpr.getType();
		Expr left = binaryExpr.getLeft();
		Expr right = binaryExpr.getRight();
		IToken op = binaryExpr.getOp();

		//checking if we have to type cast 
		if (coerceTo != null && type != coerceTo) {
			((CodeGenStringBuilder)arg)
					.lparen()
					.append(coerceTo)
					.rparen();
		}

		//Very extremely specific case of comparing two strings
		if(left.getType() == STRING && right.getType() == STRING && (op.getKind() == Kind.NOT_EQUALS || op.getKind() == Kind.EQUALS))
		{
			((CodeGenStringBuilder)arg)
					.lparen();

			if(op.getKind() == Kind.NOT_EQUALS)
				((CodeGenStringBuilder)arg)
						.bang();

			left.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(".equals")
					.lparen();
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.rparen()
					.rparen();
			return arg;
		}

		((CodeGenStringBuilder)arg)
				.lparen();
		left.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.append(op.getText());
		right.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.rparen();

		return arg;

		}

	@Override
	//Java literal corresponding to value (i.e. true or false)
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append(booleanLitExpr.getValue());
		return arg;
	}

	@Override
	// ( <boxed(coerceTo)> ConsoleIO.readValueFromConsole(“coerceType”, <prompt>) )
	// TODO : Color types are read from console similarly to the other types–the user inputs three values for the three color components.
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		Type coerceTo = consoleExpr.getCoerceTo(); //consoles will always have a coerceTo value
		String boxedtype = switch(coerceTo) {
			case INT -> "Integer";
			case STRING -> "String";
			case BOOLEAN -> "Boolean";
			case FLOAT -> "Float";
			case COLOR -> "Color";
			default -> throw new UnsupportedOperationException("INVALID INPUT");
		};

		((CodeGenStringBuilder)arg)
				.lparen()
				.append(boxedtype)
				.rparen()
				.append("ConsoleIO.readValueFromConsole")
				.lparen()
				.dblquote()
				.append(coerceTo.name())
				.dblquote()
				.comma()
				.dblquote()
				.append("Enter ")
				.append(boxedtype)
				.colon()
				.dblquote()
				.rparen();
		return arg;
	}

	@Override
	//TODO: Generate code to evaluate each color component expression and create a ColorTuple object.
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//Java float literal corresponding to value, with appended f
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		Type coerceTo = floatLitExpr.getCoerceTo();

		//Cast if needed
		if (coerceTo != null && coerceTo != FLOAT) {
			((CodeGenStringBuilder)arg)
					.lparen()
					.append(coerceTo)
					.rparen();
		}

		((CodeGenStringBuilder)arg)
				.append(floatLitExpr.getValue())
				.append("f");

		return arg;
		}

	@Override
	//TODO: Interpret the color constants as predefined instances of the java.awt.Color class.  
	//Use getRGB routine to get a packed pixel, unpack it, and create a ColorTuple object. 
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		ColorTuple obj; 
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//Java int literal corresponding to value
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		Type coerceTo = intLitExpr.getCoerceTo();

		//Cast if needed
		if (coerceTo != null && coerceTo != INT) {
			((CodeGenStringBuilder)arg)
					.lparen()
					.append(coerceTo)
					.rparen();
		}

		((CodeGenStringBuilder)arg)
				.append(intLitExpr.getValue());

		return arg;
		}

	@Override
	// <identExpr.getText>
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		Type coerceTo = identExpr.getCoerceTo();

		//Cast if needed
		if (coerceTo != null && coerceTo != identExpr.getType()) {
			((CodeGenStringBuilder)arg)
					.lparen()
					.append(coerceTo)
					.rparen();
		}

		((CodeGenStringBuilder)arg)
				.append(identExpr.getText());

		return arg;
		}

	@Override
	// “””
	// <stringLitExpr.getValue>”””
	// (we will not handle escape sequences in String literals in this assignment)
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.dblquote()
				.dblquote()
				.dblquote()
				.newline()
				.append(stringLitExpr.getValue())
				.dblquote()
				.dblquote()
				.dblquote();
		return arg;
		}

	@Override
	// ( <op> <expr> )
	// (for assignment 5, only - and !)
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		Expr expr = unaryExpr.getExpr();
		IToken op = unaryExpr.getOp();
		
		if(op.getKind() != Kind.MINUS && op.getKind() != Kind.BANG) //might need to move this logic for next assignment
			throw new UnsupportedOperationException("Not yet implemented");

		//TODO: NEW // interpreted as packed pixel
		if(op.getKind() == Kind.COLOR_OP && (expr.getType() == Type.INT || expr.getType() == Type.COLOR || expr.getType() == Type.IMAGE))
		{
			// interpreted as packed pixel
			if(expr.getType() == Type.IMAGE) //if expr is type Imagee 
			{
				//use extractRed, etc.  routines in ImageOps
				//extractBlue(expr)
				//extractRed(expr)
				//extractGreen(expr)
			}
			else // if expr is type int or Color 
			{
				//Use “routine” in ColorTuple class to get color value. 
				 return new ColorTuple(expr);
				// return color component
			}
		}
		
		
		((CodeGenStringBuilder)arg)
				.lparen()
				.append(op.getText()); //<op> 
		expr.visit(this, arg);
		((CodeGenStringBuilder)arg) //<expr>
				.rparen();

		return arg;
		}

	@Override
	// <name> = <consoleExpr> ;
	//(only read from console in assignment 5)

	//TODO : Use routines in FileURLIO to handle reading from a file or URL.  
	// If the target type is image, then the value read will be a string interpreted as url of filename.
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		Expr source = readStatement.getSource();
		
		if(source.getType() != CONSOLE) //might need to move this logic for next assignment
		{
			// Use routines in FileURLIO to handle reading from a file or URL.  
			// If the target type is image
			if(source.getType() == IMAGE)
			{
				// then the value read will be a string interpreted as url of filename. 
				return new readImage(source.getText());
			}
		}

		
		
		((CodeGenStringBuilder)arg)
				.append(readStatement.getName())
				.assign();
		source.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.semi();
		return arg;
		}

	@Override
	// <name> = <expr> ;
	
	// TODO: 
	//If <name>.type is image and <expr>.type is image, there are two cases, depending on whether the <name> was declared with a Dimension or not.  

	//If declared with a Dimension, the image <name> always keeps the declared size.  The assignment is implemented by evaluating the right hand size and calling ImageOps.resize.

	//If not declared with a size, the image <name>  takes the size of the right hand side image.  If <expr> is an identExpr, the rhs image is cloned using ImageOps.clone

	//If <expr>.coerceTo is color, the color is assigned to every pixel in the image.

	//If <expr>.coerceTo is int, the int is used as a single color component in a ColorTuple where all three color components have the value of the int.  (The value is truncated, so values outside of [0, 256) will be either white or black.)

	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		Expr expr = assignmentStatement.getExpr();
		
		if(expr.getType() == IMAGE)
		{
			// if <name> was declared with a Dimension
				//the image <name> always keeps the declared size.  The assignment is implemented by evaluating the right hand size and calling ImageOps.resize.
			
			//else 
				//the image <name>  takes the size of the right hand side image.  
			
			// If <expr> is an identExpr
			if(expr.getType() == IDENT) 
			{
				//the rhs image is cloned using ImageOps.clone
			}
			//If <expr>.coerceTo is color
			if (expr.getCoerceTo() == COLOR)
			{
				//the color is assigned to every pixel in the image.
			}
			//If <expr>.coerceTo is int
			if(expr.getCoerceTo() == INT)
			{
				//the int is used as a single color component in a ColorTuple where all three color components have the value of the int.  
				// (The value is truncated, so values outside of [0, 256) will be either white or black.)
			}
		}
		
		((CodeGenStringBuilder)arg)
				.append(assignmentStatement.getName())
				.assign();
		expr.visit(this, arg);
		((CodeGenStringBuilder)arg)
				.semi();
		return arg;
		}

	@Override
	// ConsoleIO.console.println(<source>) ;
	// (only write to console in assignment 5)
	
	//TODO: If type is image and target is console, use displayImageOnScreen method in ConsoleIO.
	// If target is a file, use writeImage in FileURLIO for image types and writeValue for other types.

	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Expr dest = writeStatement.getDest();
		Expr source = writeStatement.getSource();
		
		//If type is image 
		if(dest.getType() == IMAGE) 
		{
		
			// if target is console
				//displayImageOnScreen() from ConsoleIO
			// If target is a file
				// use writeImage() in FileURLIO for image types and writeValue for other types.
		}

		((CodeGenStringBuilder) arg)
				.append("ConsoleIO.console.println")
				.lparen();
		source.visit(this, arg);
		((CodeGenStringBuilder) arg)
				.rparen()
				.semi();

		return arg;
	}

	@Override
	//return <expr> ;
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Expr expr = returnStatement.getExpr();
		((CodeGenStringBuilder) arg)
				.append("return ");
		expr.visit(this, arg);
		((CodeGenStringBuilder) arg)
				.semi()
				.newline();
		return arg;
	}

}
