package edu.ufl.cise.plc;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.TypeCheckVisitor.Pair;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.*; //just added 
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.*; //may need to remove 
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
	
	
	public CodeGenVisitor(String packageName) {
		// TODO check if the object is correct 
		ASTVisitor v = CompilerComponentFactory.getCodeGenerator(packageName);
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
	

	
	//from pwp
	@Override
	//return <expr> ;
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
	  //TODO: Is this the correct object type for sb? alex: i updated it with the new class that handles making strings
		// Get the entire code's CGSB (in arg) (the sb is bad naming because its not the same as the other sb's)
		CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
		Expr expr = returnStatement.getExpr();
		// Append that right onto the entire code's CGSB (we don't really need to make a whole other CGSB to visit only one expr)
		sb.append("return ");
	  	expr.visit(this, sb);
	  	sb.semi().newline();
	  	return sb;
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
		CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

		program.getClass().getPackage();
		// get	<imports>
		Type type = program.getReturnType();
		sb.append("public class ");
		sb.append(program.getName());
		sb.clparen();
		sb.append("public static ");
		sb.append(type.toString());
		sb.append(" apply");
		sb.lparen();
		program.getParams();
		sb.rparen();
		sb.clparen();
		program.getDecsAndStatements();
		sb.crparen();
		sb.crparen();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//<type> <name>
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
		String type = nameDef.getType().name();
		String name = nameDef.getName();
		sb.append(type);
		sb.append(name);
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		return null;
	}

	@Override
	//(only read initializers from console for assignment 5)
	// <nameDef> ;
	// Or if  this  has an assignment or read initializer
	// <nameDef> = <expr>
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		declaration.getNameDef();
		declaration.getExpr();
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		return null;
	}

	@Override
	// ( <condition> ) ? <trueCase> : <falseCase>
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
		sb.lparen();
		sb.append(conditionalExpr.getCondition().getText());
		sb.rparen();
		sb.question();
		sb.append(conditionalExpr.getTrueCase().getText());
		sb.colon();
		sb.append(conditionalExpr.getFalseCase().getText());
		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	//Not needed for assignment 5
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		return null;
	}

	@Override
	//Not needed for assignment 5
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		return null;
	}

	@Override
	//( <left> <op> <right> )
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Type type = binaryExpr.getType();
		Expr leftExpr = binaryExpr.getLeft();
		Expr rightExpr = binaryExpr.getRight();
		Type leftType = leftExpr.getCoerceTo() != null ? leftExpr.getCoerceTo() : leftExpr.getType();
		Type rightType = rightExpr.getCoerceTo() != null ? rightExpr.getCoerceTo() : rightExpr.getType();
		Kind op = binaryExpr.getOp().getKind();

		//now build the binary expr's string
		sb.lparen();
		binaryExpr.getLeft().visit(this, sb);
		sb.append(binaryExpr.getOp().getText());
		binaryExpr.getRight().visit(this, sb);
		sb.rparen();

		//idk what this is but im leaving it here for now
		if (binaryExpr.getCoerceTo() != type) {
			genTypeConversion(type, binaryExpr.getCoerceTo(), sb);
		}

		// append the binary expr's CodeGenStringBuilder (sb) to the entire code's CodeGenStringBuilder (arg)
		return ((CodeGenStringBuilder) arg).append(sb);

		}

	@Override
	//Java literal corresponding to value (i.e. true or false)
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		booleanLitExpr.equals(arg);
		return null;
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
		  return null;
		}

	@Override
	//Not needed for assignment 5
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		return null;
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
		  return null;
		}

	@Override
	//Not needed for assignment 5
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		return null;
	}

	@Override
	/*
	Java int literal corresponding to value

	If coerceTo != null and coerceTo != INT, add cast to coerced type.
	 */
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	/*
	<identExpr.getText>

	If coerceTo != null and coerceTo != identExpr.type, add cast to coerced type.

	 */
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	// “””
	// <stringLitExpr.getValue>”””
	//(we will not handle escape sequences in String literals in this assignment)
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		
		return null;
		}

	@Override
	//( <op> <expr> ) 
	//(for assignment 5, only - and !)
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		Kind op = unaryExpr.getOp().getKind();
		Expr expr = unaryExpr.getExpr();
		
		sb.lparen();
		sb.append(unaryExpr.getOp().getText());
		unaryExpr.getExpr().visit(this, sb);;
		sb.rparen();

		return ((CodeGenStringBuilder) arg).append(sb);
		}

	@Override
	// <name> = <consoleExpr> ;
	//(only read from console in assignment 5)
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		
		sb.append(readStatement.getName());
		//get expr from console 
		sb.semi();
		return null;
		}

	@Override
	// <name> = <expr> ;
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		CodeGenStringBuilder sb = new CodeGenStringBuilder();
		sb.append(assignmentStatement.getName());
		sb.equal();
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
		return null;
	}

	//return statement is above 

}
