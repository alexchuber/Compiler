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
	
	/*
	 * From powerpoint: not sure if needed 
	 * 
	 * 
		class CodeGenStringBuilder {
         StringBuilder delegate;
         //methods reimplemented—just call the delegates method
            public CodeGenStringBuilder append(String s){
                delegate.append(st);
                return this;
            }
            etc.
            //new methods
            public CodeGenStringBuilder comma(){
                 delegate.append(“,”);
                 return this;
            }
            etc.
		}*/

	
	//from pwp
	@Override
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
	public Object visitProgram(Program program, Object arg) throws Exception {
		  return null;
		}

	//i dont think we need these-- i think theyre handled in visitProgram. dw about it for now
	public Object params(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	//i dont think we need these-- i think theyre handled in visitProgram. dw about it for now
	public Object imports(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	//i dont think we need these-- i think theyre handled in visitProgram. dw about it for now
	public Object decsAndStatements(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		return null;
	}

	//( <left> <op> <right> )
	@Override
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
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		  return null;
		}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		return null;
	}

	//return statement is above 

}

