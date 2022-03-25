package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws PLCException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Expr left = binaryExpr.getLeft();
		Expr right = binaryExpr.getRight();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		
		switch(op) {//AND, OR, PLUS, MINUS, TIMES, DIV, MOD, EQUALS, NOT_EQUALS, LT, LE, GT,GE} 
		
		case AND,OR -> {
			if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) resultType = Type.BOOLEAN;
			else check(false, binaryExpr, "incompatible types for operator");

		}
		case EQUALS,NOT_EQUALS -> {
				if (leftType == rightType) resultType = Type.BOOLEAN;
				else check(false, binaryExpr, "incompatible types for operator");

		}
		case PLUS,MINUS  -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) { resultType = Type.FLOAT; left.setCoerceTo(Type.FLOAT); }
				else if (leftType == Type.FLOAT && rightType == Type.INT) { resultType = Type.FLOAT; right.setCoerceTo(Type.FLOAT); }
				else if (leftType == Type.COLOR && rightType == Type.COLOR) resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT) resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) { resultType = Type.COLORFLOAT; right.setCoerceTo(Type.COLORFLOAT); }
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) { resultType = Type.COLORFLOAT; left.setCoerceTo(Type.COLORFLOAT); }
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
				else check(false, binaryExpr, "incompatible types for operator");


		}
		case TIMES,DIV,MOD -> {
				//from plus,minus
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) { resultType = Type.FLOAT; left.setCoerceTo(Type.FLOAT); }
				else if (leftType == Type.FLOAT && rightType == Type.INT) { resultType = Type.FLOAT; right.setCoerceTo(Type.FLOAT); }
				else if (leftType == Type.COLOR && rightType == Type.COLOR) resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT) resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) { resultType = Type.COLORFLOAT; right.setCoerceTo(Type.COLORFLOAT); }
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) { resultType = Type.COLORFLOAT; left.setCoerceTo(Type.COLORFLOAT); }
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
				//unique to this case
				else if (leftType == Type.IMAGE && rightType == Type.INT) resultType = Type.IMAGE;
				else if (leftType == Type.IMAGE && rightType == Type.FLOAT) resultType = Type.IMAGE;
				else if (leftType == Type.INT && rightType == Type.COLOR) { resultType = Type.COLOR; left.setCoerceTo(Type.COLOR); }
				else if (leftType == Type.COLOR && rightType == Type.INT) { resultType = Type.COLOR; right.setCoerceTo(Type.COLOR); }
				else if (leftType == Type.FLOAT && rightType == Type.COLOR) { resultType = Type.COLORFLOAT; left.setCoerceTo(Type.COLORFLOAT); right.setCoerceTo(Type.COLORFLOAT); }
				else if (leftType == Type.COLOR && rightType == Type.FLOAT) { resultType = Type.COLORFLOAT; left.setCoerceTo(Type.COLORFLOAT); right.setCoerceTo(Type.COLORFLOAT); }
				else check(false, binaryExpr, "incompatible types for operator");
		}
		case LT, LE, GT, GE -> {
				if (leftType == Type.INT && rightType == Type.INT) resultType = Type.BOOLEAN;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT) resultType = Type.BOOLEAN;
				else if (leftType == Type.INT && rightType == Type.FLOAT) { resultType = Type.BOOLEAN; left.setCoerceTo(Type.FLOAT); }
				else if (leftType == Type.FLOAT && rightType == Type.INT) { resultType = Type.BOOLEAN; right.setCoerceTo(Type.FLOAT); }
				else check(false, binaryExpr, "incompatible types for operator");
		}
		default -> {
				throw new Exception("compiler error");
		}
		
		} 
		binaryExpr.setType(resultType); 
		return resultType;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();
		Declaration dec = symbolTable.lookup(name); // Lookup name in symbol table, it must be declared.
		check(dec != null, identExpr, "undefined identifier " + name); 
		check(dec.isInitialized(), identExpr, "using uninitialized variable"); 
		identExpr.setDec(dec); //save declaration--will be useful later.
		Type type = dec.getType(); 
		identExpr.setType(type); 
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type condition = (Type) conditionalExpr.getCondition().visit(this, arg);
		Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);
		//Type of condition must be BOOLEAN
		check(condition == BOOLEAN, conditionalExpr, "condition should be of type Boolean");
		//Type of trueCase must be the same as the type of falseCase
		check(trueCase == falseCase, conditionalExpr, "type of trueCase must be the same as the type of falseCase");
		conditionalExpr.setType(trueCase);
		//Type is the type of trueCase
		return trueCase;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		check(dimension.getWidth().getType() == INT, dimension, "dimension expression must be int");
		check(dimension.getHeight().getType() == INT, dimension, "dimension expression must be int");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		//Get target type by looking up lhs var name in symbol table.  Save type of target variable and its Declaration. 
		String name = assignmentStatement.getName();
		Declaration targetdec = symbolTable.lookup(name); // Get target type by looking up lhs var name in symbol table
		check(targetdec != null, assignmentStatement, "undeclared target");
		//Type pixelSelector = (Type)assignmentStatement.getSelector().visit(this, arg);
		assignmentStatement.setTargetDec(targetdec); // Save type of target variable and its Declaration.
		targetdec.setInitialized(true); //Target variable is marked as initialized.

		Type targetType = targetdec.getType();

		//CASE:  target type is not IMAGE (There is no PixelSelector on left side.)
		if(targetType != IMAGE)
		{
			Type exprType = (Type) assignmentStatement.getExpr().visit(this, arg);

			//There is no PixelSelector on left side.
			check(assignmentStatement.getSelector() == null, assignmentStatement, "illegal pixelselector");

			//Expression must be assignment compatible with target.
				if(targetType != exprType)
				{
					//The following pairs are assignment compatible.
					// Map = <TargetType, ExprType>, CoerceToType
					Map<Pair<Type,Type>, Type> compatible = Map.of(
							new Pair<Type,Type>(INT,FLOAT), INT,
							new Pair<Type,Type>(FLOAT,INT), FLOAT,
							new Pair<Type,Type>(INT,COLOR), INT,
							new Pair<Type,Type>(COLOR,INT), COLOR
							);
					Type resultType = compatible.get(new Pair<Type,Type>(targetType,exprType));
					check(resultType != null, assignmentStatement, "incompatible types for assignmentStatement");
					//The expression is coerced to match the target variable type.
					assignmentStatement.getExpr().setCoerceTo(resultType);
				}
		}
		//CASE:  target type is an IMAGE without a PixelSelector
		else if(targetType == IMAGE && assignmentStatement.getSelector() == null)
		{
			Type exprType = (Type) assignmentStatement.getExpr().visit(this, arg);

			//Expression must be assignment compatible with target
			//If both the expression and target are IMAGE, they are assignment compatible
			if(targetType != exprType)
			{
				//The following pairs are assignment compatible.  If indicated, the variable should be coerced to the indicated type.
				// Map = <TargetType, ExprType>, CoerceToType
				Map<Pair<Type,Type>, Type> compatible = Map.of(
						new Pair<Type,Type>(IMAGE,INT), COLOR,
						new Pair<Type,Type>(IMAGE,FLOAT), COLORFLOAT,
						new Pair<Type,Type>(IMAGE,COLOR), COLOR,
						new Pair<Type,Type>(IMAGE,COLORFLOAT), COLORFLOAT
				);
				Type resultType = compatible.get(new Pair<Type,Type>(targetType,exprType));
				check(resultType != null, assignmentStatement, "incompatible types for assignmentStatement");
				//The expression is coerced to match the target variable type.
				assignmentStatement.getExpr().setCoerceTo(resultType);
			}
		}
		//CASE:  target type is an IMAGE with a PixelSelector
		else //if(targetType == IMAGE && assignmentStatement.getSelector() != null)
		{
			//Recall from scope rule:  expressions appearing in PixelSelector that appear on the left side of an assignment statement are local variables defined in the assignment statement.

			Expr x = assignmentStatement.getSelector().getX();
			Expr y = assignmentStatement.getSelector().getY();
			// The names cannot be previously declared as global variable.
			check(symbolTable.lookup(x.getText()) == null && symbolTable.lookup(y.getText()) == null, assignmentStatement, "illegal global variables in pixelselector ");
			//These variables must be an IdentExpr.
			check(x.getClass() == IdentExpr.class && y.getClass() == IdentExpr.class, assignmentStatement, "illegal expression in pixelselector");
			//Implicitly declare to type INT
			x.setType(Type.INT);
			y.setType(Type.INT);

			// Temporarily insert x and y into symbol table
			NameDef xtemp = new NameDef(x.getFirstToken(), "int", x.getText());
			xtemp.setInitialized(true);
			NameDef ytemp = new NameDef(y.getFirstToken(), "int", y.getText());
			ytemp.setInitialized(true);
			symbolTable.insert(x.getText(), xtemp);
			symbolTable.insert(y.getText(), ytemp);

			//Then process rhs..
			Type exprType = (Type) assignmentStatement.getExpr().visit(this, arg);

			//Type of right hand side must be COLOR, COLORFLOAT, FLOAT, or INT, and is coerced to COLOR.  
			// Map = <TargetType, ExprType>, CoerceToType
			Map<Type, Type> compatible = Map.of(
					COLOR, COLOR,
					COLORFLOAT, COLOR,
					FLOAT, COLOR,
					INT, COLOR
			);
			Type resultType = compatible.get(exprType);
			check(resultType != null, assignmentStatement, "incompatible types for assignmentStatement");
			//The expression is coerced
			assignmentStatement.getExpr().setCoerceTo(resultType);

			//Then remove from symbol table
			symbolTable.remove(x.getText());
			symbolTable.remove(y.getText());
		}

		return null;
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement, "illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		//Get target type by looking up lhs var name in symbol table.
		String name = readStatement.getName();
		Declaration targetdec = symbolTable.lookup(name); // Lookup name in symbol table, it must be declared.
		readStatement.setTargetDec(targetdec);
		
		//A read statement cannot have a PixelSelector
		check(readStatement.getSelector() == null , readStatement, "read statement cannot have a PixelSelector");

		//The right hand side type must be CONSOLE or STRING
		Type sourceType = (Type) readStatement.getSource().visit(this, arg);
		check(sourceType == CONSOLE || sourceType == STRING, readStatement, "right hand side type must be CONSOLE or STRING");

		//Mark target variable as initialized.
		targetdec.setInitialized(true);
		return null;

	}

	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		//  implement this method (should be good)
		Type targetType = (Type) declaration.getNameDef().visit(this, arg);

		// if declaration has a rhs
		if (declaration.getExpr() != null) {
			declaration.getNameDef().setInitialized(true);
			declaration.setInitialized(true);
			// ensure rhs is initialized
			declaration.getExpr().visit(this, arg);
		}

		// If type of variable is Image
		if(targetType == IMAGE)
		{
			//it must either have an initializer expression of type IMAGE
			if(declaration.getExpr() != null) {
				Type exprType = (Type) declaration.getExpr().visit(this, arg);
				check(exprType == IMAGE, declaration, "need image initializer expression");
			}
			//or a Dimension
			else {
				Dimension dim = declaration.getDim();
				check(dim != null, declaration, "need image dimension");
				//For Dimensions, both expressions must have type INT
				Type x = (Type) dim.getWidth().visit(this,arg);
				Type y = (Type) dim.getHeight().visit(this,arg);
				check(x == INT && y == INT, declaration, "need integer types for dimension arguments");
			}
		}

		//If VarDeclaration has an assignment initializer, the right hand side type must be assignment compatible as defined above for Assignment Statements.
		if(declaration.getOp() != null && declaration.getOp().getKind() == Kind.ASSIGN)
		{
			Type exprType = (Type) declaration.getExpr().visit(this,arg);

			//CASE:  target type is not IMAGE (There is no PixelSelector on left side.)
			if(targetType != IMAGE)
			{
				//Expression must be assignment compatible with target.
				if(targetType != exprType)
				{
					//The following pairs are assignment compatible.
					// Map = <TargetType, ExprType>, CoerceToType
					Map<Pair<Type,Type>, Type> compatible = Map.of(
							new Pair<Type,Type>(INT,FLOAT), INT,
							new Pair<Type,Type>(FLOAT,INT), FLOAT,
							new Pair<Type,Type>(INT,COLOR), INT,
							new Pair<Type,Type>(COLOR,INT), COLOR
					);
					Type resultType = compatible.get(new Pair<Type,Type>(targetType,exprType));
					check(resultType != null, declaration, "incompatible types for assignmentStatement");
					//The expression is coerced to match the target variable type.
					declaration.getExpr().setCoerceTo(resultType);
					//return the type for convenience in this visitor.
					//return resultType;
				}
				//If the expression type and target variable type are the same, they are assignment compatible.
				else
				{
					//return exprType;
				}
			}

			//CASE:  target type is an IMAGE without a PixelSelector
			else if(targetType == IMAGE && declaration.getDim() == null)
			{
				//Expression must be assignment compatible with target
				//If both the expression and target are IMAGE, they are assignment compatible
				if(targetType != exprType)
				{
					//The following pairs are assignment compatible.  If indicated, the variable should be coerced to the indicated type.
					// Map = <TargetType, ExprType>, CoerceToType
					Map<Pair<Type,Type>, Type> compatible = Map.of(
							new Pair<Type,Type>(IMAGE,INT), COLOR,
							new Pair<Type,Type>(IMAGE,FLOAT), COLORFLOAT,
							new Pair<Type,Type>(IMAGE,COLOR), COLOR,
							new Pair<Type,Type>(IMAGE,COLORFLOAT), COLORFLOAT
					);
					Type resultType = compatible.get(new Pair<Type,Type>(targetType,exprType));
					check(resultType != null, declaration, "incompatible types for assignmentStatement");
					//The expression is coerced to match the target variable type.
					declaration.getExpr().setCoerceTo(resultType);
					//return the type for convenience in this visitor.
					//return resultType;
				}
				else
				{
					//return exprType;
				}
			}
			//CASE:  target type is an IMAGE with a PixelSelector
			else //if(targetType == IMAGE && targetdec.getDim() != null)
			{
				//Recall from scope rule:  expressions appearing in PixelSelector that appear on the left side of an assignment statement are local variables defined in the assignment statement.

				Expr x = declaration.getDim().getWidth();
				Expr y = declaration.getDim().getHeight();
				// The names cannot be previously declared as global variable.
				check(symbolTable.lookup(x.getText()) == null && symbolTable.lookup(y.getText()) == null, declaration, "illegal global variables in pixelselector");
				//These variables are implicitly declared to have type INT, and must be an IdentExpr.
				check(x.getClass() == IdentExpr.class && y.getClass() == IdentExpr.class, declaration, "illegal expression in pixelselector");

				//Type of right hand side must be COLOR, COLORFLOAT, FLOAT, or INT, and is coerced to COLOR.
				// Map = <TargetType, ExprType>, CoerceToType
				Map<Type, Type> compatible = Map.of(
						COLOR, COLOR,
						COLORFLOAT, COLOR,
						FLOAT, COLOR,
						INT, COLOR
				);
				Type resultType = compatible.get(exprType);
				check(resultType != null, declaration, "incompatible types for assignmentStatement");
				//The expression is coerced
				declaration.getExpr().setCoerceTo(resultType);
				//return the type for convenience in this visitor.
				//return resultType;
			}
		}

		//If VarDeclaration has a read initializer, the right hand side type must be assignment compatible as defined above for Read Statements.
		else if (declaration.getOp() != null && declaration.getOp().getKind() == Kind.LARROW)
		{
			Type exprType = (Type) declaration.getExpr().visit(this, arg);
			check(exprType == CONSOLE || exprType == STRING, declaration, "right hand side type must be CONSOLE or STRING");
		}

		return null;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		//Save root of AST so return type can be accessed in return statements
		root = program;

		//Handle parameters as NameDef (not NameDefWithDim).  Mark name as initialized.
		List<NameDef> params = program.getParams();
		for (NameDef node : params) {
			node.visit(this, arg);
			node.setInitialized(true);
		}

		//Visit nodes in decsAndStatements.
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program.getReturnType();
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		//insert name in symbol table
		String name = nameDef.getName();
		boolean inserted = symbolTable.insert(name,nameDef);
		check(inserted, nameDef, "name "+name+" already in use");
		return nameDef.getType();
	
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		//insert name in symbol table
		String name = nameDefWithDim.getName();
		boolean inserted = symbolTable.insert(name,nameDefWithDim);
		check(inserted, nameDefWithDim, "name already in use");
		return nameDefWithDim.getType();
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
