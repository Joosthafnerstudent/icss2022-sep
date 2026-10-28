package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
	}

    public AST getAST() {
        return ast;
    }


	// Helper functies

	private void closeNode(){
		ASTNode finished = currentContainer.pop();
		currentContainer.peek().addChild(finished);
	}

	private void addLeaf(ASTNode leaf){
		currentContainer.peek().addChild(leaf);
	}

	// Stylesheet

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx){
	currentContainer.push(ast.root);
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx){
		ast.setRoot((Stylesheet) currentContainer.pop());
	}

	// Stylerule & Selectors

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx){
		currentContainer.push(new Stylerule());
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx){
		closeNode();
	}

	@Override
	public void exitTagSelector(ICSSParser.TagSelectorContext ctx){
	 addLeaf(new TagSelector(ctx.getText()));
	}


	@Override
	public void exitIdSelector(ICSSParser.IdSelectorContext ctx){
		addLeaf(new IdSelector(ctx.getText()));
	}

	@Override
	public void exitClassSelector(ICSSParser.ClassSelectorContext ctx){
		addLeaf(new ClassSelector(ctx.getText()));
	}


	// Declarations
	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx){
		currentContainer.push(new Declaration());
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx){
		closeNode();
	}

	@Override
	public void exitPropertyName(ICSSParser.PropertyNameContext ctx){
		addLeaf(new PropertyName(ctx.getText()));
	}

	// variables
	@Override
	public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx){
	currentContainer.push(new VariableAssignment());
	}

	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx){
		closeNode();
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx){
		addLeaf(new VariableReference(ctx.getText()));
	}

	// if else
	@Override
	public void enterIfClause(ICSSParser.IfClauseContext ctx){
	 currentContainer.push(new IfClause());
	}

	@Override
	public void exitIfClause(ICSSParser.IfClauseContext ctx){
		closeNode();
	}

	@Override
	public void enterElseClause(ICSSParser.ElseClauseContext ctx){
		currentContainer.push(new ElseClause());
	}

	@Override
	public void exitElseClause(ICSSParser.ElseClauseContext ctx){
		closeNode();
	}

	// Operations

	@Override
	public void enterMultiplyOperation(ICSSParser.MultiplyOperationContext ctx){
		currentContainer.push(new MultiplyOperation());
	}

	@Override
	public void exitMultiplyOperation(ICSSParser.MultiplyOperationContext ctx){
		closeNode();
	}

	@Override
	public void enterAddOrSubtractOperation(ICSSParser.AddOrSubtractOperationContext ctx){
		if (ctx.PLUS() !=null){
			currentContainer.push(new AddOperation());
		} else {
			currentContainer.push(new SubtractOperation());
		}
	}

	@Override
	public void exitAddOrSubtractOperation(ICSSParser.AddOrSubtractOperationContext ctx){
		closeNode();
	}


	// Own Extenstion
	@Override
	public void enterNotOperation(ICSSParser.NotOperationContext ctx){
	currentContainer.push(new NotOperation());
	}

	@Override
	public void exitNotOperation(ICSSParser.NotOperationContext ctx){
		closeNode();
	}

	@Override
	public void enterComparisonOperation(ICSSParser.ComparisonOperationContext ctx){
		if (ctx.LESS_THAN() != null){
			currentContainer.push(new LessThanOperation());
		} else if (ctx.GREATER_THAN() != null){
			currentContainer.push(new GreaterThanOperation());
		} else if (ctx.LESS_OR_EQUAL() != null) {
			currentContainer.push(new LessOrEqualOperation());
		} else {
			currentContainer.push(new GreaterOrEqualOperation());
		}
	}

	@Override
	public void	exitComparisonOperation(ICSSParser.ComparisonOperationContext ctx){
		closeNode();
	}

	@Override
	public void enterEqualityOperation(ICSSParser.EqualityOperationContext ctx){
		if (ctx.EQUALS() != null){
			currentContainer.push(new EqualOperation());
		} else{
			currentContainer.push(new NotEqualOperation());
		}
	}

	@Override
	public void exitEqualityOperation(ICSSParser.EqualityOperationContext ctx){
		closeNode();
	}

	@Override
	public void enterAndOperation(ICSSParser.AndOperationContext ctx){
		currentContainer.push(new AddOperation());
	}

	@Override
	public void exitAndOperation(ICSSParser.AndOperationContext ctx){
		closeNode();
	}

	@Override
	public void enterOrOperation(ICSSParser.OrOperationContext ctx){
		currentContainer.push(new OrOperation());
	}

	@Override
	public void exitOrOperation(ICSSParser.OrOperationContext ctx){
		closeNode();
	}

	// literals

	@Override
	public void exitPixelLiteral(ICSSParser.PixelLiteralContext ctx){
		addLeaf(new PixelLiteral(ctx.getText()));
	}

	@Override
	public void exitPercentageLiteral(ICSSParser.PercentageLiteralContext ctx){
		addLeaf(new PercentageLiteral(ctx.getText()));
	}

	@Override
	public void exitScalarLiteral(ICSSParser.ScalarLiteralContext ctx){
		addLeaf(new ScalarLiteral(ctx.getText()));
	}

	@Override
	public void exitColorLiteral(ICSSParser.ColorLiteralContext ctx){
		addLeaf(new ColorLiteral(ctx.getText()));
	}

	@Override
	public void exitTrueLiteral(ICSSParser.TrueLiteralContext ctx) {
		addLeaf(new BoolLiteral(true));
	}
	@Override
	public void exitFalseLiteral(ICSSParser.FalseLiteralContext ctx) {
		addLeaf(new BoolLiteral(false));
	}

}