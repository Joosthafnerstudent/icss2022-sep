package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;


/*
Deze code evalueert de ast na de transfromatie. De ast bevat geen variabelen meer maar alleen nog de stylerules
 */
public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();

    }

    /*
    Voert de evaluatie uit op de gehele AST. De globale scope blijft open tot de hele stylesheet is verwerkt
     */
    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        openScope();
        ast.root.body = transformBody(ast.root.body);
        closeScope();
    }


    // Scope-beheer

    /*
    Opent een nieuwe  scope bij bijvoorbeeld een stylerule
     */
    private void openScope() {
        variableValues.addFirst(new HashMap<>());
    }

    /*
    Sluit de binnenste scope, bijvoorbeeld bij een stylerule
     */
    private void closeScope() {
        variableValues.removeFirst();
    }

    private void declareVariable(String name, Literal val) {
        variableValues.getFirst().put(name, val);
    }

    /*
      Zoekt een variable, zodat een lokale variabele een globale met dezelfde naam overschaduwt

      @return de waarde van de variabele, of null als deze in geen enkele scope bestaad
     */
    private Literal lookupVariable(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            Literal value = variableValues.get(i).get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }


    /*
    Transformeerd de body van een stylesheet en geeft de nieuwe body terug:
     - Stylerule: wordt recursief getransformeerd en behouden
     - VariableAssignment: Wordt geevalueerd en opgeslagen in de scope, verdwijnt uit de body
     */
    private ArrayList<ASTNode> transformBody(ArrayList<ASTNode> body) {
        ArrayList<ASTNode> result = new ArrayList<>();
        for (ASTNode node : body) {
            if (node instanceof Stylerule) {
                transformStylerule((Stylerule) node);
                result.add(node);
            } else if (node instanceof VariableAssignment) {
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

    /*
     Een stylerule krijgt een eigen scope waardoor de variabele binnen lokaal blijven
     */
    private void transformStylerule(Stylerule stylerule) {
        openScope();
        stylerule.body = transformBody(stylerule.body);
        closeScope();
    }

    /*
       Evalueerd de voorwaarde van een if-clause, die de getransformeerde body terug geeft van de gekozen tak.
       Is de voorwaarde niet waar en er is geen else dan wordt het resultaat een lege lijst.
     */
    private ArrayList<ASTNode> transformIfClause(IfClause ifClause) {
        Literal result = evaluate(ifClause.conditionalExpression);

        if (!(result instanceof BoolLiteral)) {
            throw new IllegalArgumentException(
                    "De if-voorwaarde moet een geldige boolean opleveren."
            );
        }

        BoolLiteral condition = (BoolLiteral) result;

        ArrayList<ASTNode> chosenBody = null;

        if (condition.value) {
            chosenBody = ifClause.body;
        } else if (ifClause.elseClause != null) {
            chosenBody = ifClause.elseClause.body;
        }

        if (chosenBody == null) {
            return new ArrayList<>();
        }

        openScope();
        ArrayList<ASTNode> transformed = transformBody(chosenBody);
        closeScope();

        return transformed;
    }

     /*
        Rekent een expressie uit tot een literal
     */
    private Literal evaluate(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        } else if (expression instanceof VariableReference) {
            return lookupVariable(((VariableReference) expression).name);
        } else if (expression instanceof NotOperation) {
            NotOperation notOperation = (NotOperation) expression;
            Literal operand = evaluate(notOperation.operand);

            if (!(operand instanceof BoolLiteral)) {
                throw new IllegalArgumentException(
                        "Operator ! vereist een boolean operand."
                );
            }

            return new BoolLiteral(!((BoolLiteral) operand).value);
        } else if (expression instanceof Operation) {
            return evaluateOperation((Operation) expression);
        }

        return null;
    }

    /*
        Rekent een binare operatie uit. Eerst worden beide operanden gevalueerd zodat geneste operaties en variabelen zijn opgelost
        en daarna wordt er op basis van het type operatie het resultaat bepaalt.
     */
    private Literal evaluateOperation(Operation operation) {

        Literal left = evaluate(operation.lhs);
        Literal right = evaluate(operation.rhs);

        if (left == null  || right == null){
            throw new IllegalArgumentException("Operator " + operation.getNodeLabel().toLowerCase()  + " heeft een operand zonder waarde");
        }

        if (operation instanceof AndOperation) {
            return new BoolLiteral(
                    ((BoolLiteral) left).value &&
                            ((BoolLiteral) right).value
            );
        }

        if (operation instanceof OrOperation) {
            return new BoolLiteral(
                    ((BoolLiteral) left).value ||
                            ((BoolLiteral) right).value
            );
        }

        if (operation instanceof EqualOperation) {
            return new BoolLiteral(left.equals(right));
        }

        if (operation instanceof NotEqualOperation) {
            return new BoolLiteral(!left.equals(right));
        }

        if (operation instanceof LessThanOperation) {
            return new BoolLiteral(valueOf(left) < valueOf(right));
        }

        if (operation instanceof GreaterThanOperation) {
            return new BoolLiteral(valueOf(left) > valueOf(right));
        }

        if (operation instanceof LessOrEqualOperation) {
            return new BoolLiteral(valueOf(left) <= valueOf(right));
        }

        if (operation instanceof GreaterOrEqualOperation) {
            return new BoolLiteral(valueOf(left) >= valueOf(right));
        }

        if (operation instanceof MultiplyOperation) {
            if (left instanceof ScalarLiteral) {
                return createLiteral(right, valueOf(left) * valueOf(right));
            }
            return createLiteral(left, valueOf(left) * valueOf(right));
        }

        if (operation instanceof AddOperation) {
            return createLiteral(left, valueOf(left) + valueOf(right));
        }

        if (operation instanceof SubtractOperation) {
            return createLiteral(left, valueOf(left) - valueOf(right));
        }

        return null;
    }

    // Helper functies voor literals

    /*
    Haalt de numerike waarde uit een pixel- , percentage- of scalar-literal
     */
    private int valueOf(Literal literal) {
        if (literal instanceof PixelLiteral) {
            return ((PixelLiteral) literal).value;
        } else if (literal instanceof PercentageLiteral) {
            return ((PercentageLiteral) literal).value;
        }
        return ((ScalarLiteral) literal).value;
    }

    /*
    Maakt een nieuwe literal met dezelfde eenheid als unitSource en de gegeven waarde.
     */
    private Literal createLiteral(Literal unitSource, int value) {
        if (unitSource instanceof PixelLiteral) {
            return new PixelLiteral(value);
        } else if (unitSource instanceof PercentageLiteral) {
            return new PercentageLiteral(value);
        }
        return new ScalarLiteral(value);
    }
}
