package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();

    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        openScope();
        ast.root.body = transformBody(ast.root.body);
        closeScope();
    }

    private void openScope() {
      variableValues.addFirst(new HashMap<>());
    }

    private void closeScope() {
        variableValues.removeFirst();
    }

    private void declareVariable(String name, Literal val){
        variableValues.getFirst().put(name, val);
    }
    private Literal lookupVariable(String name){
        for (int i = 0; i < variableValues.getSize() ; i++) {
            Literal value = variableValues.get(i).get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private ArrayList<ASTNode> transformBody(ArrayList<ASTNode> body) {
        ArrayList<ASTNode> result = new ArrayList<>();
        for (ASTNode node: body){
             if (node instanceof Stylerule){
                 transformStylerule((Stylerule) node);
                 result.add(node);
             } else if (node instanceof  VariableAssignment) {
                 VariableAssignment assignment = (VariableAssignment) node;
                 declareVariable(assignment.name.name, evaluate(assignment.expression));
             } else if (node instanceof Declaration) {
                 Declaration declaration = (Declaration) node;
                 declaration.expression = evaluate(declaration.expression);
                 result.add(declaration);
             } else if (node instanceof IfClause) {
                 result.addAll(transformIfClause((IfClause) node));
             } else {
                 result.add(node);
             }
        }
        return result;
    }


    private void transformStylerule(Stylerule stylerule){
        openScope();
        stylerule.body = transformBody(stylerule.body);
        closeScope();
    }

    private ArrayList<ASTNode> transformIfClause(IfClause ifClause) {
        BoolLiteral condition = (BoolLiteral) evaluate(ifClause.conditionalExpression);

        ArrayList<ASTNode> chosenBody = null;
        if (condition.value){
            chosenBody = ifClause.body;
        } else if (ifClause.elseClause != null)  {
            chosenBody = ifClause.elseClause.body;
        }

        if(chosenBody == null){
            return new ArrayList<>();
        }

        openScope();
        ArrayList<ASTNode> transformed = transformBody(chosenBody);
        closeScope();
        return transformed;
    }

    private Literal evaluate(Expression expression){
        if(expression instanceof Literal){
            return (Literal) expression;
        } else if (expression instanceof VariableReference) {
            return lookupVariable(((VariableReference) expression).name);
        } else if (expression instanceof Operation) {
            return evaluateOperation((Operation) expression);
        }
        return null;
    }

    private Literal evaluateOperation(Operation operation){
        Literal left = evaluate(operation.lhs);
        Literal right = evaluate(operation.rhs);

        if (operation instanceof MultiplyOperation){
            if (left instanceof ScalarLiteral){
                return createLiteral(right, valueOf(left) * valueOf(right));
            }
            return createLiteral(left, valueOf(left) * valueOf(right));
        }
        if(operation instanceof AddOperation){
            return createLiteral(left, valueOf(left) + valueOf(right));
        }

        if (operation instanceof SubtractOperation){
            return createLiteral(left, valueOf(left) - valueOf(right));
        }
        return null;
    }

    private int valueOf(Literal literal){
        if (literal instanceof PixelLiteral){
            return ((PixelLiteral) literal).value;
        } else if (literal instanceof PercentageLiteral) {
            return ((PercentageLiteral) literal).value;
        }
        return ((ScalarLiteral) literal).value;
    }

    private Literal createLiteral(Literal unitSource, int value){
        if (unitSource instanceof PixelLiteral){
            return new PixelLiteral(value);
        } else if (unitSource instanceof PercentageLiteral) {
            return new PercentageLiteral(value);
        }
        return new ScalarLiteral(value);
    }
}
