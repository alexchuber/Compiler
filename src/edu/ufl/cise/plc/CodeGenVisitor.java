package edu.ufl.cise.plc;

import java.util.List;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
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

	//generate Java code  (not sure if we need this, from power point)
	private String genCode(ASTNode ast, String packageName, String className) throws Exception {
		CodeGenVisitor v = (CodeGenVisitor)
		CompilerComponentFactory.getCodeGenerator(packageName);
		String[] names = {packageName, className};
		String code = (String) ast.visit(v, names);
		return code;
	}
	
	/*//Compile generated java code (not sure if we need this, from power point)
	private byte[] genBytecode(String input, String packageName, String className) throws Exception {
		String code = genJavaCode(input, packageName);
		String fullName = packageName != "" ? packageName + '.' + className : className;
		byte[] byteCode = DynamicCompiler.compile(fullName, code);
		return byteCode;
		}*/



	
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
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.append(nameDef.getType()).space();
		sb.append(nameDef.getName());
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//(only read initializers from console for assignment 5)
	// <nameDef> ;
	// Or if  this  has an assignment or read initializer
	// <nameDef> = <expr>
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		declaration.getNameDef().visit(this, sb); //get name def in stringbuilder
		IToken op = declaration.getOp();
		//If there's a RHS
		if(op != null) {
			sb.assign();
			//Check for cast
			Type coerceTo = declaration.getExpr().getCoerceTo();
			Type type = declaration.getExpr().getType();
			if (coerceTo != null && type != coerceTo) {
				sb.lparen().append(coerceTo).rparen();
			}

			if(op.getKind() == Kind.ASSIGN) {
				declaration.getExpr().visit(this, sb);
			}
			if(op.getKind() == Kind.LARROW) {
				if(declaration.getExpr().getType() != CONSOLE)
					throw new UnsupportedOperationException("Not yet implemented");
				declaration.getExpr().visit(this, sb);
			}
		}
		sb.semi();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	// ( <condition> ) ? <trueCase> : <falseCase>
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.lparen();
		sb.lparen();
		conditionalExpr.getCondition().visit(this, sb);
		sb.rparen();
		sb.question();
		conditionalExpr.getTrueCase().visit(this, sb);
		sb.colon();
		conditionalExpr.getFalseCase().visit(this, sb);
		sb.rparen();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//Not needed for assignment 5
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//( <left> <op> <right> )
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Type coerceTo = binaryExpr.getCoerceTo();
		Type type = binaryExpr.getType();
		Expr left = binaryExpr.getLeft();
		Expr right = binaryExpr.getRight();
		IToken op = binaryExpr.getOp();

		if (coerceTo != null && type != coerceTo) {
			sb.lparen().append(coerceTo).rparen();
		}

		//Very extremely specific case of comparing two strings
		if(left.getType() == STRING && right.getType() == STRING && (op.getKind() == Kind.NOT_EQUALS || op.getKind() == Kind.EQUALS))
		{
			sb.lparen();
			if(op.getKind() == Kind.NOT_EQUALS)
				sb.bang();
			binaryExpr.getLeft().visit(this, sb);
			sb.append(".equals").lparen();
			binaryExpr.getRight().visit(this, sb);
			sb.rparen().rparen();
			return ((CodeGenStringBuilder) arg).append(sb);
		}

		sb.lparen();
		left.visit(this, sb);
		sb.append(op.getText());
		right.visit(this, sb);
		sb.rparen();

		return ((CodeGenStringBuilder) arg).append(sb);

		}

	@Override
	//Java literal corresponding to value (i.e. true or false)
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.append(booleanLitExpr.getValue());
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	/*
	 * ( <boxed(coerceTo)> ConsoleIO.readValueFromConsole( “coerceType”, <prompt>)

	<prompt> is a string that requests the user to enter the desired type.

	<boxed(type)> means the object version of the indicated type: Integer, Boolean, Float, etc. 

	The first argument of readValueFromConsole is an all uppercase String literal with corresponding to the type. (i.e. one of “INT”, “STRING”, “BOOLEAN”,  “FLOAT”)

	For example, if the PLCLang source has

	j <- console;
	where j is int, then this would translate to

	j = (Integer) ConsoleIO.readValueFromConsole(“INT”, “Enter integer:”);

	Note that the “j = “ part would be generated by the parent AssignmentStatement.  See the provided ConsoleIO class.  
	 */
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Type coerceTo = consoleExpr.getCoerceTo(); //consoles will always have a coerceTo value
		sb.lparen();
		String boxedtype = switch(coerceTo) {
			case INT -> "Integer";
			case STRING -> "String";
			case BOOLEAN -> "Boolean";
			case FLOAT -> "Float";
			default -> throw new UnsupportedOperationException("Not yet (or supposed to be?) implemented");
		};
		sb.append(boxedtype);
		sb.rparen();
		sb.append("ConsoleIO.readValueFromConsole");
		sb.lparen();
		sb.dblquote();
		sb.append(coerceTo.name());
		sb.dblquote();
		sb.comma();
		sb.dblquote();
		sb.append("Enter ");
		sb.append(boxedtype);
		sb.colon();
		sb.dblquote();
		sb.rparen();
		return ((CodeGenStringBuilder) arg).append(sb);
	}

	@Override
	//Not needed for assignment 5
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	/*
	 * Java float literal corresponding to value.  

	If coerceTo != null and coerceTo != FLOAT, add cast to coerced type.

	Recall Java float literals must have f appended.  
	E.g.  12.3 in source is 12.3f in Java.  (12.3 in Java is a double–if you do this your program will probably run, 
	but fail test cases that check for equality)
	 */
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();

		Type coerceTo = floatLitExpr.getCoerceTo();
		if (coerceTo != null && coerceTo != FLOAT) {
			sb.lparen().append(coerceTo).rparen();
		}

		sb.append(floatLitExpr.getValue());
		sb.append("f");

		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	/*
	Java int literal corresponding to value

	If coerceTo != null and coerceTo != INT, add cast to coerced type.
	 */
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();

		Type coerceTo = intLitExpr.getCoerceTo();
		if (coerceTo != null && coerceTo != INT) {
			sb.lparen().append(coerceTo).rparen();
		}

		sb.append(intLitExpr.getValue());

		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	/*
	<identExpr.getText>

	If coerceTo != null and coerceTo != identExpr.type, add cast to coerced type.

	 */
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();

		Type coerceTo = identExpr.getCoerceTo();
		if (coerceTo != null && coerceTo != identExpr.getType()) {
			sb.lparen().append(coerceTo).rparen();
		}

		sb.append(identExpr.getText());

		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	// “””
	// <stringLitExpr.getValue>”””
	//(we will not handle escape sequences in String literals in this assignment)
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.dblquote(); sb.dblquote(); sb.dblquote();
		sb.newline();
		sb.append(stringLitExpr.getValue());
		sb.dblquote(); sb.dblquote(); sb.dblquote();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//( <op> <expr> ) 
	//(for assignment 5, only - and !)
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		IToken op = unaryExpr.getOp();

		sb.lparen();
		switch (op.getKind()) {
			case MINUS, BANG: sb.append(unaryExpr.getOp().getText()); break;
			default: throw new UnsupportedOperationException("Not yet implemented");
		}
		unaryExpr.getExpr().visit(this, sb);
		sb.rparen();

		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	// <name> = <consoleExpr> ;
	//(only read from console in assignment 5)
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Expr source = readStatement.getSource();

		sb.append(readStatement.getName());
		sb.assign();
		switch(source.getType()) {
			case CONSOLE: source.visit(this, sb); break;
			default: throw new UnsupportedOperationException("Not yet implemented");
		}
		sb.semi();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	// <name> = <expr> ;
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.append(assignmentStatement.getName());
		sb.assign();
		assignmentStatement.getExpr().visit(this, sb);
		sb.semi();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	/*
	 * ConsoleIO.console.println(<source>) ;

	println here is just the usual PrintStream method.   
	Usually this is used with the PrintStream instance System.out.  
	For this assignment, you should instead use the PrintStream object ConsoleIO.console.  
	This will typically be assigned to System.out, but may be changed for grading or other purposes.  

	 */
	// (only write to console in assignment 5)
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Expr dest = writeStatement.getDest();
		Expr source = writeStatement.getSource();
		switch(dest.getType()) {
			case CONSOLE:
				sb.append("ConsoleIO.console.println").lparen();
				source.visit(this, sb);
				sb.rparen().semi();
				break;
			default: throw new UnsupportedOperationException("Not yet implemented");
		}

		return ((CodeGenStringBuilder) arg).append(sb);
	}

	//from pwp
	@Override
	//return <expr> ;
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Expr expr = returnStatement.getExpr();
		sb.append("return ");
		expr.visit(this, sb);
		sb.semi().newline();
		return ((CodeGenStringBuilder) arg).append(sb);
	}

}
