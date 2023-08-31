package parser;

%%// Scanner options

%class LexicalAnalyzer
%unicode
%line
%column
%function nextToken
%type Symbol

%{ // For convenience
    private Symbol toSymbol(LexicalUnit unit){
       return new Symbol(unit, yyline, yycolumn, yytext());
    }

    // Stack structure used to keep track of nested WHILE and IF statements for END/ENDIF identification
    // true when BEGIN or WHILE is encountered, false when IF is encountered
    private java.util.Stack<Boolean> tracker = new java.util.Stack<>();
%}
// Return value of the program
%eofval{
	return toSymbol(LexicalUnit.EOS);
%eofval}

%yylexthrow Exception

// Extended Regular Expressions

Lowercase        = [a-z]
Uppercase        = [A-Z]
Letter           = {Lowercase}|{Uppercase}
Numeric          = [0-9]
Integer          = -?([1-9][0-9]*)|0
Decimal          = \.[0-9]*
Real             = {Numeric}*{Decimal}?
Identifier       = {Lowercase}({Lowercase}|{Numeric})*
ProgName         = {Uppercase}+({Numeric}|{Letter})*
LineTerminator   = \r|\n|\r\n
Blank            = {LineTerminator}|\s
InvalidTokens    = {Real}\w* | {Letter}\w*{Uppercase}*\w*
Any              = .

SingleComment    = ::.*
MultiLineComment = %%~%%
Comment          = {SingleComment}|{MultiLineComment}

%state PROGBODY
%%// Identification of tokens
<YYINITIAL> {
    {Comment}	  {/* Ignored */}

    // PROGNAME can only be matched at the start. Once matched, switch to PROGBODY state to easily detect invalid variable names
    "BEGIN"/.*		  {tracker.push(true);
                        return toSymbol(LexicalUnit.BEGIN);}
    {ProgName}    {yybegin(PROGBODY);
                        return toSymbol(LexicalUnit.PROGNAME);}
    {Blank}		    {}
    {InvalidTokens} {throw new LexicalException(yytext());}
    {Any}           {throw new LexicalException(yyline, yycolumn, yytext());}
}

<PROGBODY> {
    {Comment}	  {/* Ignored */}
    {Blank}		    {}
    // Reserved keywords
    "WHILE"/.*		  {tracker.push(true);
                        return toSymbol(LexicalUnit.WHILE);}
    "IF"/.*          {tracker.push(false);
                        return toSymbol(LexicalUnit.IF);}
    "END"/.*		  {if (tracker.pop()) { return toSymbol(LexicalUnit.END);}
                        else {return toSymbol(LexicalUnit.ENDIF );}}
    "READ"/.*		  {return toSymbol(LexicalUnit.READ);}
    "ELSE"/.*        {return toSymbol(LexicalUnit.ELSE);}
    "PRINT"/.*		  {return toSymbol(LexicalUnit.PRINT);}
    "THEN"/.*        {return toSymbol(LexicalUnit.THEN);}
    "DO"/.*          {return toSymbol(LexicalUnit.DO);}

    // Operations
    "(" 	  	  {return toSymbol(LexicalUnit.LPAREN);}
    ")" 		  {return toSymbol(LexicalUnit.RPAREN);}
    "+"           {return toSymbol(LexicalUnit.PLUS);}
    "-"           {return toSymbol(LexicalUnit.MINUS);}
    "/"           {return toSymbol(LexicalUnit.DIVIDE);}
    "*"           {return toSymbol(LexicalUnit.TIMES);}
    ":="          {return toSymbol(LexicalUnit.ASSIGN);}
    ">"           {return toSymbol(LexicalUnit.GREATER);}
    "<"           {return toSymbol(LexicalUnit.SMALLER);}
    "="           {return toSymbol(LexicalUnit.EQUAL);}
    ","           {return toSymbol(LexicalUnit.COMMA);}

    {Integer}     {return toSymbol(LexicalUnit.NUMBER);}
    {Identifier}  {return toSymbol(LexicalUnit.VARNAME);}

    // halt when an unexpected token is matched
    {InvalidTokens} {throw new LexicalException(yyline, yycolumn, yytext());}
    {Any}           {throw new LexicalException(yyline, yycolumn, yytext());}
}