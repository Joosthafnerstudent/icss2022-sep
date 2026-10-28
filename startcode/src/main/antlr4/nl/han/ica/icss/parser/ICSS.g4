grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//Line comments are skipped as well
COMMENT: '//' ~[\r\n]* -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';

// Boolean expression support (own extension):
PAREN_OPEN: '(';
PAREN_CLOSE: ')';
NOT: '!';
AND: '&&';
OR: '||';
EQUALS: '==';
NOT_EQUALS: '!=';
LESS_OR_EQUAL: '<=';
GREATER_OR_EQUAL: '>=';
LESS_THAN: '<';
GREATER_THAN: '>';




//--- PARSER: ---
stylesheet: (variableAssignment | stylerule)* EOF;

stylerule: selector OPEN_BRACE ruleBody CLOSE_BRACE;

selector: ID_IDENT      #idSelector
        | CLASS_IDENT   #classSelector
        | LOWER_IDENT   #tagSelector
        ;

// A rule body (also used by if/else): declarations, variable assignments and nested ifs
ruleBody: (declaration | variableAssignment | ifClause)*;

declaration: propertyName COLON expression SEMICOLON;
propertyName: LOWER_IDENT;

variableAssignment: variableReference ASSIGNMENT_OPERATOR expression SEMICOLON;
variableReference: CAPITAL_IDENT;

ifClause: IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE OPEN_BRACE ruleBody CLOSE_BRACE elseClause?;
elseClause: ELSE OPEN_BRACE ruleBody CLOSE_BRACE;

// Alternatives are listed from highest to lowest precedence; all binary operators are left associative:
// !  >  *  >  + -  >  < > <= >=  >  == !=  >  &&  >  ||
expression: NOT expression                                          #notOperation
          | expression MUL expression                               #multiplyOperation
          | expression (PLUS | MIN) expression                      #addOrSubtractOperation
          | expression (LESS_THAN | GREATER_THAN
                       | LESS_OR_EQUAL | GREATER_OR_EQUAL) expression #comparisonOperation
          | expression (EQUALS | NOT_EQUALS) expression             #equalityOperation
          | expression AND expression                               #andOperation
          | expression OR expression                                #orOperation
          | PAREN_OPEN expression PAREN_CLOSE                       #parenthesesExpression
          | literal                                                 #literalExpression
          | variableReference                                       #variableExpression
          ;

literal: COLOR       #colorLiteral
       | PIXELSIZE   #pixelLiteral
       | PERCENTAGE  #percentageLiteral
       | SCALAR      #scalarLiteral
       | TRUE        #trueLiteral
       | FALSE       #falseLiteral
       ;

