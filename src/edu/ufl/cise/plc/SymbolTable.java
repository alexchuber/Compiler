package edu.ufl.cise.plc;

import java.util.HashMap;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.Declaration;

public class SymbolTable {

//TODO:  Implement a symbol table class that is appropriate for this language. (this should be good)
	HashMap<String,Declaration> entries = new HashMap<>();
	//returns true if name successfully inserted in symbol table, false if already present
	public boolean insert(String name, Declaration declaration) 
	{ 
		return (entries.putIfAbsent(name,declaration) == null);
	}
	//returns Declaration if present, or null if name not declared.
	public Declaration lookup(String name) 
	{
	     return entries.get(name);
	} 
	
	SymbolTable symbolTable = new SymbolTable();
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException 
	{
		if (! condition)  throw new TypeCheckException(message, node.getSourceLoc()); 
	}

}
