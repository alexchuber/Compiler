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

	@Override
	// <nameDef> ;
	// Or if  this  has an assignment or read initializer
	// <nameDef> = <expr>
	//(only read initializers from console for assignment 5)
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		NameDef namedef = declaration.getNameDef();
		Expr expr = declaration.getExpr();
		IToken op = declaration.getOp();

		namedef.visit(this, arg);

		//If there's a RHS
		if(expr != null) {
			((CodeGenStringBuilder)arg)
					.assign();

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
					throw new UnsupportedOperationException("Not yet implemented");
				expr.visit(this, arg);
			}
		}

		((CodeGenStringBuilder)arg)
				.semi();

		return arg;
	}

	@Override
	//ColorTuple.unpack(<unaryExpName>.getRGB(<visitPixelSelector>))
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append("ColorTuple.unpack")
				.lparen()
				.append(unaryExprPostfix.getExpr().getText())
				.dot()
				.append("getRGB")
				.lparen();

		unaryExprPostfix.getSelector().visit(this, arg);

		((CodeGenStringBuilder)arg)
				.rparen()
				.rparen();

		return arg;
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
	//<visitWidth>, <visitHeight>
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		dimension.getWidth().visit(this, arg);
		((CodeGenStringBuilder)arg)
				.comma();
		dimension.getHeight().visit(this, arg);

		return arg;
	}

	@Override
	//TODO
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	//( <left> <op> <right> )
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Type coerceTo = binaryExpr.getCoerceTo();
		Type type = binaryExpr.getType();
		Expr left = binaryExpr.getLeft();
		Expr right = binaryExpr.getRight();
		IToken op = binaryExpr.getOp();

		//if left and right are colors
		//(ImageOps.binaryTupleOp(ImageOps.OP.<opText>, <visit left>, <visit right>));
		if(left.getType() == COLOR && right.getType() == COLOR) {
			((CodeGenStringBuilder)arg)
					.append("(ImageOps.binaryTupleOp(ImageOps.OP.")
					.append(op.getText())
					.comma();
			left.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.comma();
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append("));");
		}
		//if left and right are images
		//(ImageOps.binaryImageImageOp(ImageOps.OP.<opText>, <left>, <right>));
		else if(left.getType() == COLOR && right.getType() == COLOR) {
			((CodeGenStringBuilder)arg)
					.append("(ImageOps.binaryImageImageOp(ImageOps.OP.")
					.append(op.getText())
					.comma();
			left.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.comma();
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append("));");
		}
		//if one is image, one is color
		/*(ImageOps.binaryImageScalarOp(ImageOps.OP.<opText>, <left>, <right>),
			ColorTuple.makePackedColor(ColorTuple.getRed(<right>),
			ColorTuple.getGreen(<right>),
			ColorTuple.getBlue(<right>))); */
		else if((left.getType() == COLOR && right.getType() == IMAGE)
				|| (left.getType() == IMAGE && right.getType() == COLOR)) {
			((CodeGenStringBuilder)arg)
					.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.")
					.append(op.getText())
					.comma();
			left.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.comma();
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append("),")
					.append("ColorTuple.makePackedColor(ColorTuple.getRed(");
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append("), ColorTuple.getGreen(");
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append("), ColorTuple.getBlue(");
			right.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(")));");
		}
		// for everything else (from assignment 5)
		else{
			if (coerceTo != null && type != coerceTo) {
				((CodeGenStringBuilder) arg)
						.lparen()
						.append(coerceTo)
						.rparen();
			}

			//Very extremely specific case of comparing two strings
			if (left.getType() == STRING && right.getType() == STRING && (op.getKind() == Kind.NOT_EQUALS || op.getKind() == Kind.EQUALS)) {
				((CodeGenStringBuilder) arg)
						.lparen();

				if (op.getKind() == Kind.NOT_EQUALS)
					((CodeGenStringBuilder) arg)
							.bang();

				left.visit(this, arg);
				((CodeGenStringBuilder) arg)
						.append(".equals")
						.lparen();
				right.visit(this, arg);
				((CodeGenStringBuilder) arg)
						.rparen()
						.rparen();
				return arg;
			}

			((CodeGenStringBuilder) arg)
					.lparen();
			left.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.append(op.getText());
			right.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.rparen();
		}

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
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		Type coerceTo = consoleExpr.getCoerceTo(); //consoles will always have a coerceTo value
		String boxedtype = switch(coerceTo) {
			case INT -> "Integer";
			case STRING -> "String";
			case BOOLEAN -> "Boolean";
			case FLOAT -> "Float";
			case COLOR -> "ColorTuple";
			default -> throw new UnsupportedOperationException("Not yet (or supposed to be?) implemented");
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
	//new ColorTuple(<visitRed>, <visitGreen>, <visitBlue>)
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append("new ColorTuple")
				.lparen();
		colorExpr.getRed().visit(this, arg);
		((CodeGenStringBuilder)arg)
				.comma();
		colorExpr.getGreen().visit(this, arg);
		((CodeGenStringBuilder)arg)
				.comma();
		colorExpr.getBlue().visit(this, arg);
		((CodeGenStringBuilder)arg)
				.rparen();

		return arg;
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
	//ColorTuple.unpack(Color.<ColorConstName>.getRGB())
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		((CodeGenStringBuilder)arg)
				.append("ColorTuple.unpack(")
				.append("Color.")
				.append(colorConstExpr.getText())
				.append(".getRGB())");

		return arg;
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
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		Expr expr = unaryExpr.getExpr();
		IToken op = unaryExpr.getOp();
		// if op is ! or -
		// ( <op> <expr> )
		if(op.getKind() == Kind.MINUS || op.getKind() == Kind.BANG) {
			((CodeGenStringBuilder)arg)
					.lparen()
					.append(op.getText());
			expr.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.rparen();
		}
		// if expr is int
		// ColorTuple.<opText>(<visitExpr>);
		else if(expr.getType() == INT || expr.getType() == COLOR) {
			((CodeGenStringBuilder)arg)
					.append("ColorTuple.")
					.append(op.getText())
					.lparen();
			expr.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(");");
		}
		// if expr is image
		// ImageOps.extract(switch on opText to output Blue for getBlue and so on)(<visitExpr>);
		else if(expr.getType() == IMAGE)
		{
			String color = switch(op.getText()) {
				case "getRed" -> "Red";
				case "getGreen" -> "Green";
				case "getBlue" -> "Blue";
				default -> throw new UnsupportedOperationException("Not implemented!");
			};

			((CodeGenStringBuilder)arg)
					.append("ImageOps.extract")
					.append(color)
					.lparen();
			expr.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(");");
		}
		else
			throw new UnsupportedOperationException("You didn't implement this!");

		return arg;
	}

	@Override
	// <name> = <consoleExpr> ;
	//(only read from console in assignment 5)
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		Declaration target = readStatement.getTargetDec();
		Expr source = readStatement.getSource();

		//if reading from console like before
		if(source.getType() == CONSOLE)
		{
			((CodeGenStringBuilder)arg)
					.append(readStatement.getName())
					.assign();
			source.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.semi();
		}
		//(if name.type is image and getdim != null)
		else if(target.getType() == IMAGE && target.getDim() != null)
		{
			target.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.assign()
					.append("FileURLIO.readImage(");
			source.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.comma();
			target.getDim().visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(");\nFileURLIO.closeFiles();");
		}
		//(if name.type is image and getdim==null)
		else if(target.getType() == IMAGE && target.getDim() == null)
		{
			target.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.assign()
					.append("FileURLIO.readImage(");
			source.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.append(");\nFileURLIO.closeFiles();");
		}
		//uhhh lets see
		else
			throw new UnsupportedOperationException("You didn't implement this yet!");

		return arg;
	}

	@Override
	//TODO may not be filled in-- i didnt check blue text
	// <name> = <expr> ;
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		Expr expr = assignmentStatement.getExpr();
		Declaration name = assignmentStatement.getTargetDec();

		//if the namedef for assignment statement is an image and has dimension and expr.coerceTo is color)
		if(name.getType() == IMAGE && name.getDim() != null && expr.getCoerceTo() == Type.COLOR) {
			Expr x = name.getDim().getWidth();
			Expr y = name.getDim().getHeight();

			//for(int <getX> = 0; <getX> < a.getWidth(); <getX>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(x.getText())
					.append(" = 0; ")
					.append(x.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getWidth(); ")
					.append(x.getText())
					.append("++)\n\t");
			//for(int <getY> = 0; <getY> < a.getHeight(); <getY>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(y.getText())
					.append(" = 0; ")
					.append(y.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getHeight(); ")
					.append(y.getText())
					.append("++)\n\t");
			//ImageOps.setColor(a, <getX>, <getY>, <visitColorExpr>);
			((CodeGenStringBuilder) arg)
					.append("ImageOps.setColor(")
					.append(assignmentStatement.getName())
					.comma()
					.append(x.getText())
					.comma()
					.append(y.getText())
					.comma();
			expr.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.append(");");
		}

		//if the namedef for assignment statement is an image and has dimension and expr.coerceTo is int)
		else if(name.getType() == IMAGE && name.getDim() != null && expr.getCoerceTo() == Type.INT) {
			Expr x = name.getDim().getWidth();
			Expr y = name.getDim().getHeight();

			//for(int <getX> = 0; <getX> < a.getWidth(); <getX>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(x.getText())
					.append(" = 0; ")
					.append(x.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getWidth(); ")
					.append(x.getText())
					.append("++)\n\t");
			//for(int <getY> = 0; <getY> < a.getHeight(); <getY>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(y.getText())
					.append(" = 0; ")
					.append(y.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getHeight(); ")
					.append(y.getText())
					.append("++)\n\t");
			//ImageOps.setColor(a, <getX>, <getY>, ColorTuple.unpack(ColorTuple.truncate(<visitExpr>)));
			((CodeGenStringBuilder) arg)
					.append("ImageOps.setColor(")
					.append(assignmentStatement.getName())
					.comma()
					.append(x.getText())
					.comma()
					.append(y.getText())
					.comma()
					.append("ColorTuple.unpack(ColorTuple.truncate(");
			expr.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.append(")));");
		}

		//if the namedef for assignment statement is an image with no dimension
		else if(name.getType() == IMAGE && name.getDim() == null) {
			Expr x = name.getDim().getWidth();
			Expr y = name.getDim().getHeight();

			//for(int <getX> = 0; <getX> < a.getWidth(); <getX>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(x.getText())
					.append(" = 0; ")
					.append(x.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getWidth(); ")
					.append(x.getText())
					.append("++)\n\t");
			//for(int <getY> = 0; <getY> < a.getHeight(); <getY>++)\n\t
			((CodeGenStringBuilder) arg)
					.append("for(int ")
					.append(y.getText())
					.append(" = 0; ")
					.append(y.getText())
					.append(" < ")
					.append(assignmentStatement.getName())
					.append(".getHeight(); ")
					.append(y.getText())
					.append("++)\n\t");
		}

		//for normal stuff
		else {
			((CodeGenStringBuilder) arg)
					.append(assignmentStatement.getName())
					.assign();
			expr.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.semi();
		}
		return arg;
	}

	@Override
	// ConsoleIO.console.println(<source>) ;
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Expr dest = writeStatement.getDest();
		Expr source = writeStatement.getSource();

		//if image to console
		//ConsoleIO.displayImageOnScreen(<sourceName>);
		if(source.getType() == IMAGE && dest.getType() == CONSOLE) {
			((CodeGenStringBuilder)arg)
					.append("ConsoleIO.displayImageOnScreen(")
					.append(source.getText())
					.append(");");
		}
		//if image to file
		//FileURLIO.writeImage(<sourceImage>, <destName>);
		else if(source.getType() == IMAGE && dest.getType() == STRING) {
			((CodeGenStringBuilder)arg)
					.append("FileURLIO.writeImage(")
					.append(source.getText())
					.comma()
					.append(dest.getText())
					.append(");");
		}
		//if anything else to file
		//FileURLIO.writeValue(<source>, <destName>);
		else if (dest.getType() == STRING) {
			((CodeGenStringBuilder)arg)
					.append("FileURLIO.writeValue(");
			source.visit(this, arg);
			((CodeGenStringBuilder)arg)
					.comma()
					.append(dest.getText())
					.append(");");
		}
		//else anything else to console
		else {
			((CodeGenStringBuilder) arg)
					.append("ConsoleIO.console.println")
					.lparen();
			source.visit(this, arg);
			((CodeGenStringBuilder) arg)
					.rparen()
					.semi();
		}

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
