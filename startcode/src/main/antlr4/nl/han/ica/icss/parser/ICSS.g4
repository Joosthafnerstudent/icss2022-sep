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

// Alternatives are listed from highest to lowest precedence; both operators are left associative
expression: expression MUL expression          #multiplyOperation
          | expression (PLUS | MIN) expression #addOrSubtractOperation
          | literal                            #literalExpression
          | variableReference                  #variableExpression
          ;

literal: COLOR       #colorLiteral
       | PIXELSIZE   #pixelLiteral
       | PERCENTAGE  #percentageLiteral
       | SCALAR      #scalarLiteral
       | TRUE        #trueLiteral
       | FALSE       #falseLiteral
       ;

