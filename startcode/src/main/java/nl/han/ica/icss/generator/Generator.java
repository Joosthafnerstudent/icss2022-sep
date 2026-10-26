package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Generates CSS2-compliant code from a fully transformed (evaluated) ICSS AST.
 * Indentation is two spaces per scope level.
 */
public class Generator {

	private static final String INDENT = "  ";

	public String generate(AST ast) {
		StringBuilder css = new StringBuilder();
		ArrayList<ASTNode> body = ast.root.body;

		boolean first = true;
		for (ASTNode node : body) {
			if (node instanceof Stylerule) {
				if (!first) {
					css.append("\n");
				}
				generateStylerule((Stylerule) node, css, 0);
				first = false;
			}
		}
		return css.toString();
	}

	private void generateStylerule(Stylerule rule, StringBuilder css, int level) {
		String selectors = rule.selectors.stream()
				.map(Selector::toString)
				.collect(Collectors.joining(", "));

		indent(css, level).append(selectors).append(" {\n");
		for (ASTNode node : rule.body) {
			if (node instanceof Declaration) {
				generateDeclaration((Declaration) node, css, level + 1);
			}
		}
		indent(css, level).append("}\n");
	}

	private void generateDeclaration(Declaration declaration, StringBuilder css, int level) {
		indent(css, level)
				.append(declaration.property.name)
				.append(": ")
				.append(generateExpression(declaration.expression))
				.append(";\n");
	}

	private String generateExpression(Expression expression) {
		if (expression instanceof PixelLiteral) {
			return ((PixelLiteral) expression).value + "px";
		} else if (expression instanceof PercentageLiteral) {
			return ((PercentageLiteral) expression).value + "%";
		} else if (expression instanceof ScalarLiteral) {
			return String.valueOf(((ScalarLiteral) expression).value);
		} else if (expression instanceof ColorLiteral) {
			return ((ColorLiteral) expression).value;
		} else if (expression instanceof BoolLiteral) {
			return ((BoolLiteral) expression).value ? "true" : "false";
		}
		return "";
	}

	private StringBuilder indent(StringBuilder sb, int level) {
		for (int i = 0; i < level; i++) {
			sb.append(INDENT);
		}
		return sb;
	}
}