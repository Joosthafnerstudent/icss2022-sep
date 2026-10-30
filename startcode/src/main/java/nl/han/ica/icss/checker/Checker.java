package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;


/*
Controleert de AST op symantische fouten. Fouten worden niet gegooit maar als foutmelding op de betreffende AST node gezet(setError),
 zodat alle fouten in één keer getoond kunnen worden
 */
public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    /*
    Controleert de gehele ASt de scope blijft open totdat alle nodes cocontroleerd zijn
     */
    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        openScope();
        checkBody(ast.root.getChildren());
        closeScope();
    }

    // Scope beheer

    /*
    Opent een nieuwe  scope bij bijvoorbeeld een stylerule
     */
    private void openScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    /*
    Sluit de binnenste scope, bijvoorbeeld bij een stylerule
     */
    private void closeScope() {
        variableTypes.removeFirst();
    }

    /*
    Slaat een variabele op in de scope
     */
    private void declareVariable(String name, ExpressionType type) {
        variableTypes.getFirst().put(name, type);
    }

    /*
        Zoekt het type van de variabele
     */

    private ExpressionType lookupVariable(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            ExpressionType type = variableTypes.get(i).get(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /*
        Controleert alle nodes die in de body staan en roept per node de bijbehorende controle aan
     */
    private void checkBody(Iterable<ASTNode> body) {
        for (ASTNode node : body) {
            if (node instanceof Stylerule) {
                checkStylerule((Stylerule) node);
            } else if (node instanceof VariableAssignment) {
                checkVariableAssignmenment((VariableAssignment) node);
            } else if (node instanceof Declaration) {
                checkDeclaration((Declaration) node);
            } else if (node instanceof IfClause) {
                checkIfClause((IfClause) node);
            }
        }
    }

    /*
      Een stylerule krijgt een eigen scope , zodat variabelen lokaal blijven
     */
    private void checkStylerule(Stylerule stylerule) {
        openScope();
        checkBody(stylerule.body);
        closeScope();
    }

    /*
        Bepaalt het type van de expressie en legt dit vast als type van de variabele
     */
    private void checkVariableAssignmenment(VariableAssignment assignment) {
        ExpressionType type = checkExpression(assignment.expression);
        declareVariable(assignment.name.name, type);
    }

    /*
    Controleert of de waarde van een declaratie past bij de property:
        with en height: pixel of percentage
        color en background color: kleur
        elke ander property is onbekend dus fout
     */
    private void checkDeclaration(Declaration declaration) {
        ExpressionType type = checkExpression(declaration.expression);
        if (type == ExpressionType.UNDEFINED) {
            return;
        }
        String property = declaration.property.name;
        switch (property) {
            case "width":
            case "height":
                if (type != ExpressionType.PIXEL && type != ExpressionType.PERCENTAGE) {
                    declaration.setError("Property '" + property + "' needs a pixel or percantage value but got " + describe(type));
                }
                break;
            case "color":
            case "background-color":
                if (type != ExpressionType.COLOR) {
                    declaration.setError("Property '" + property + "' needs a color value but got " + describe(type));
                }
                break;
            default:
                declaration.setError("Unknown property '" + property + "'");
        }
    }

    /*
    Controleert dat de voorwaarde van een if-clause een boolean is daarna de body van de if/else elke tak krijgt een eigen scope
     */
    private void checkIfClause(IfClause ifClause) {
        ExpressionType conditionType = checkExpression(ifClause.conditionalExpression);
        if (conditionType != ExpressionType.BOOL && conditionType != ExpressionType.UNDEFINED) {
            ifClause.setError("The condition of the if-statement must be a boolean but got " + describe(conditionType));
        }
        openScope();
        checkBody(ifClause.body);
        closeScope();

        if (ifClause.elseClause != null) {
            openScope();
            checkBody(ifClause.elseClause.body);
            closeScope();
        }
    }

    //Controle van expressie

    /*
    Bepaalt het type van een expressie. Literals hebben direct een vast type, bij variabelen en operaties wordt het type bepaald door de bijbehorende controle methode
    Elke fout geeft UNDEFINED
     */
    private ExpressionType checkExpression(Expression expression) {
        if (expression instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        } else if (expression instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        } else if (expression instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        } else if (expression instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        } else if (expression instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        } else if (expression instanceof VariableReference) {
            return checkVariableReference((VariableReference) expression);
        } else if (expression instanceof NotOperation) {
            return checkNotOperation((NotOperation) expression);
        } else if (expression instanceof Operation) {
            return checkOperation((Operation) expression);
        }
        return ExpressionType.UNDEFINED;
    }

   /*
        Een variabele moet in de huidige of een omliggende scope gedefineerd zijn
    */
    private ExpressionType checkVariableReference(VariableReference reference) {
        ExpressionType type = lookupVariable(reference.name);
        if (type == null) {
            reference.setError("Variable '" + reference.name + "'is not defined in this scope");
            return ExpressionType.UNDEFINED;
        }
        return type;
    }

    /*
        Controleert een binaire operatie en geeft het type van het resultaat terug

     */
    private ExpressionType checkOperation(Operation operation) {
        ExpressionType left = checkExpression(operation.lhs);
        ExpressionType right = checkExpression(operation.rhs);

        if (left == ExpressionType.UNDEFINED || right == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (operation instanceof AndOperation || operation instanceof OrOperation) {
            return checkLogicalOperation(operation, left, right);
        }

        if (operation instanceof EqualOperation || operation instanceof NotEqualOperation) {
            return checkEqualityOperation(operation, left, right);
        }

        if (isComperrison(operation)) {
            return checkComperisonOperation(operation, left, right);
        }

        if (!isNumeric(left) || !isNumeric(right)) {
            operation.setError("Cannot use " + describe(isNumeric(left) ? right : left)
                    + " in the operation " + operation.getNodeLabel().toLowerCase());
            return ExpressionType.UNDEFINED;
        }

        if (operation instanceof MultiplyOperation) {
            if (left == ExpressionType.SCALAR) {
                return right;
            }
            if (right == ExpressionType.SCALAR) {
                return left;
            }
            operation.setError("Mulitpication needs at least one scaler operand, but got " + describe(left) + " * " + describe(right));
            return ExpressionType.UNDEFINED;
        }

        if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
            if (left != right) {
                operation.setError("Cannot " + operation.getNodeLabel().toLowerCase() + " " + describe(left) + " And " + describe(right));
                return ExpressionType.UNDEFINED;
            }
            return left;
        }
        return ExpressionType.UNDEFINED;
    }


    // && en || : Beide operanden moeten een boolean zijn
    private ExpressionType checkLogicalOperation(Operation operation, ExpressionType left, ExpressionType right) {
        if (left != ExpressionType.BOOL || right != ExpressionType.BOOL) {
            operation.setError("Operator " + operation.getNodeLabel().toLowerCase() + " needs an boolean but got " + describe(left) + " and " + describe(right));
            return ExpressionType.UNDEFINED;
        }
        return ExpressionType.BOOL;
    }

    private ExpressionType checkEqualityOperation(Operation operation, ExpressionType left, ExpressionType right) {
        if (left != right) {
            operation.setError("Kan niet vergelijken " + describe(left) + "met " + describe(right) + "met gebruik van " + operation.getNodeLabel().toLowerCase());
            return ExpressionType.UNDEFINED;
        }
        return ExpressionType.BOOL;
    }


    private ExpressionType checkComperisonOperation(Operation operation, ExpressionType left, ExpressionType right) {
        if (!isNumeric(left) || !isNumeric(right) || left != right) {
            operation.setError("Kan niet vergelijken " + describe(left) + " met " + describe(right) + "gebruik " + operation.getNodeLabel().toLowerCase());
            return ExpressionType.UNDEFINED;
        }
        return ExpressionType.BOOL;
    }

    private ExpressionType checkNotOperation(NotOperation operation) {
        ExpressionType operand = checkExpression(operation.operand);
        if (operand == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }
        if (operand != ExpressionType.BOOL) {
            operation.setError("Operand moet van het type boolean zijn maar kreeg " + describe(operand));
        }

        return ExpressionType.BOOL;
    }

    // Helper functies

    private boolean isComperrison(Operation operation) {
        return operation instanceof LessThanOperation
                || operation instanceof GreaterThanOperation
                || operation instanceof LessOrEqualOperation
                || operation instanceof GreaterOrEqualOperation;
    }

    private boolean isNumeric(ExpressionType type) {
        return type == ExpressionType.PIXEL
                || type == ExpressionType.PERCENTAGE
                || type == ExpressionType.SCALAR;
    }


    private String describe(ExpressionType type) {
        return type.name().toLowerCase();
    }

}
