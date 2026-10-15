package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;


public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        openScope();
        checkBody(ast.root.getChildren());
        closeScope();
    }

    private void openScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    private void closeScope() {
        variableTypes.removeFirst();
    }

    private void declareVariable(String name, ExpressionType type) {
        variableTypes.getFirst().put(name, type);
    }

    private ExpressionType lookupVariable(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            ExpressionType type = variableTypes.get(i).get(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

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


    private void checkStylerule(Stylerule stylerule) {
        openScope();
        checkBody(stylerule.body);
        closeScope();
    }


    private void checkVariableAssignmenment(VariableAssignment assignment) {
        ExpressionType type = checkExpression(assignment.expression);
        declareVariable(assignment.name.name, type);
    }

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
        } else if (expression instanceof Operation) {
            return checkOperation((Operation) expression);
        }
        return ExpressionType.UNDEFINED;
    }


    private ExpressionType checkVariableReference(VariableReference reference) {
        ExpressionType type = lookupVariable(reference.name);
        if (type == null) {
            reference.setError("Variable '" + reference.name + "'is not defined in this scope");
            return ExpressionType.UNDEFINED;
        }
        return type;
    }

    private ExpressionType checkOperation(Operation operation) {
        ExpressionType left = checkExpression(operation.lhs);
        ExpressionType right = checkExpression(operation.rhs);

        if (left == ExpressionType.UNDEFINED || right == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
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

        if (operation instanceof AddOperation || operation instanceof SubtractOperation){
            if(left != right){
                operation.setError("Cannot " + operation.getNodeLabel().toLowerCase() + " " + describe(left) + " And " + describe(right));
                return ExpressionType.UNDEFINED;
            }
            return left;
        }
        return ExpressionType.UNDEFINED;
    }

    private boolean isNumeric(ExpressionType type){
        return type == ExpressionType.PIXEL
                || type == ExpressionType.PERCENTAGE
                || type == ExpressionType.SCALAR;
    }


    private String describe(ExpressionType type) {
        return type.name().toLowerCase();
    }

}
