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

	/*
	 Zet de AST om naar een CSS-string. Tussen twee stylerules komt een lege regel
	 */
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

	/*
		Genereerd één stylerule: De selectors gescheiden door een comma, gevolgt door de declaraties tussen de accolades.
		Alleen declaraties worden geschreven, andere nodes hebben na de evaluatie geen betekenis meer voor de CSS-uitvoer.
	 */
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

	/*
		Genereert één declaratie in de vorm van "property": waarde
	 */
	private void generateDeclaration(Declaration declaration, StringBuilder css, int level) {
		indent(css, level)
				.append(declaration.property.name)
				.append(": ")
				.append(generateExpression(declaration.expression))
				.append(";\n");
	}

	/*
		Zet een literal om naar een CSS-notatie, inclusief eenheid (px of %).
		Geeft een lege string terug voor een expressie die geen literal is
	 */
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

	/*
	Voegt de inspringing voor het opgegeven niveau toe en geeft de builder terug.
	 */
	private StringBuilder indent(StringBuilder sb, int level) {
		for (int i = 0; i < level; i++) {
			sb.append(INDENT);
		}
		return sb;
	}
}