package nl.han.ica.icss.ast.operations;

import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.Operation;

import java.util.ArrayList;
import java.util.Objects;

public class NotOperation extends Expression {

    public Expression operand;

    @Override
    public String getNodeLabel(){
        return "Not";
    }

    @Override
    public ArrayList<ASTNode> getChildren(){
        ArrayList<ASTNode> children = new ArrayList<>();

        if (operand != null){
            children.add(operand);
        }
        return children;
    }

    @Override
    public ASTNode addChild(ASTNode child){
        if (operand == null){
            operand = (Expression) child;
        }
        return this;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(operand, ((NotOperation) o).operand);
    }

    @Override
    public int hashCode(){
        return Objects.hash(operand);
    }




}
