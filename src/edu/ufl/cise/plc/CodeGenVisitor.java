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
	private String genCode(ASTNode ast, String packageName, String className) throws 
	Exception {
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
	//visitX(CodeGenVisitor v, StringBuilder sb) 

	
	//from pwp
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
	  //TODO: Is this the correct object type for sb? 
	  DynamicClassLoader sb = (DynamicClassLoader) arg;
	  Expr expr = returnStatement.getExpr();
	  sb.append("return ");
	  expr.visit(this, sb);
	  sb.semi().newline();
	  return sb;
	}
	
	public Object program(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	
	public Object params(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object imports(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object decsAndStatements(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object nameDef(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object varDeclaration(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object conditionalExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object binaryExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object booleanLitExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object consoleExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object floatLitExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object intLitExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object identExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object stringLitExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object unaryExpr(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	public Object readStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	
	public Object assignmentStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		  return null;
		}
	
	//return statement is above 

}

