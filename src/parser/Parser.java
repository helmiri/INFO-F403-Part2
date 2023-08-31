package parser;


import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * LL(1) recursive descent parser
 */
public class Parser {
    private final LexicalAnalyzer scanner;
    private final StringBuilder derivation;
    private Symbol next;

    /**
     *
     * @param source File to be parsed
     * @throws Exception on read or parsing error
     */
    public Parser(String source) throws Exception {
        derivation = new StringBuilder();
        this.scanner = new LexicalAnalyzer(new InputStreamReader(new FileInputStream(source)));
        initScan(scanner);
        scanner.yyreset(new InputStreamReader(new FileInputStream(source)));
        this.next = scanner.nextToken();
    }

    /**
     * Initiates lexical scanning
     */
    private void initScan(LexicalAnalyzer scanner) throws Exception {
        while (!scanner.nextToken().getType().equals(LexicalUnit.EOS)) {
        }
    }

    /**
     * Fetch next token
     * @throws Exception on error reading file
     */
    private void updateNext() throws Exception {
        this.next = scanner.nextToken();
    }

    /**
     * Match current token with the expected one then consume it
     * @param token Expected token
     * @return Tree node
     * @throws Exception on error reading file
     * @throws ParserException when the current and expected tokens do not match
     */
    private ParseTree match(LexicalUnit token) throws Exception, ParserException {
        if (next.getType().equals(token)) {
            Symbol temp = new Symbol(next.getType(), next.getLine(), next.getColumn(), next.getValue());
            updateNext();
            return new ParseTree(temp);
        }
        throw new ParserException(next, token);
    }

    /**
     * Update derivation sequence
     * @param i Rule number
     */
    private void derive(int i) {
        derivation.append(i).append(" ");
    }

    /**
     * Initiate parsing
     * @return Parse Tree of the file if parsing was successful
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    public ParseTree parse() throws Exception, ParserException {
        // Program is the initial symbol of the grammar
        return program();
    }

    /**
     * Program rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree program() throws Exception, ParserException {
        derive(1);
        return new ParseTree("Program", Arrays.asList(
                match(LexicalUnit.BEGIN),
                match(LexicalUnit.PROGNAME),
                code(),
                match(LexicalUnit.END)));
    }

    /**
     * Code rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree code() throws Exception, ParserException {
        return switch (next.getType()) {
            case VARNAME, IF, WHILE, PRINT, READ -> {
                derive(2);
                yield new ParseTree("Code", Arrays.asList(instruction(), instNext()));
            }
            case END, ENDIF, ELSE -> {
                derive(3);
                yield new ParseTree("Code", List.of(epsilon()));
            }
            default -> throw new ParserException(next);
        };
    }

    /**
     * InstNext rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree instNext() throws Exception, ParserException {
        return switch (next.getType()) {
            case EOS -> {
                derive(5);
                yield new ParseTree("InstNext", List.of(epsilon()));
            }
            default -> {
                derive(4);
                yield new ParseTree("InstNext", Arrays.asList(match(LexicalUnit.COMMA), code()));
            }
        };
    }

    /**
     * Instruction rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree instruction() throws Exception, ParserException {
        return switch (next.getType()) {
            case VARNAME -> {
                derive(6);
                yield new ParseTree("Instruction", List.of(assign()));
            }
            case IF -> {
                derive(7);
                yield new ParseTree("Instruction", List.of(condition()));
            }
            case WHILE -> {
                derive(8);
                yield new ParseTree("Instruction", List.of(loop()));
            }
            case PRINT -> {
                derive(9);
                yield new ParseTree("Instruction", List.of(print()));
            }
            case READ -> {
                derive(10);
                yield new ParseTree("Instruction", List.of(read()));
            }
            default -> throw new ParserException(next);
        };
    }

    /**
     * Assign rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree assign() throws Exception, ParserException {
        derive(11);
        return new ParseTree("Assign", Arrays.asList(
                match(LexicalUnit.VARNAME),
                match(LexicalUnit.ASSIGN),
                cond()));
    }

    /**
     * Cond rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree cond() throws Exception, ParserException {
        derive(12);
        return new ParseTree("Cond", Arrays.asList(exprArith(), simpleCond()));
    }

    /**
     * SimpleCond rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree simpleCond() throws Exception, ParserException {
        return switch (next.getType()) {
            case GREATER -> {
                derive(13);
                yield new ParseTree("SimpleCond", Arrays.asList(match(LexicalUnit.GREATER), exprArith(), simpleCond()));
            }
            case SMALLER -> {
                derive(14);
                yield new ParseTree("SimpleCond", Arrays.asList(match(LexicalUnit.SMALLER), exprArith(), simpleCond()));
            }
            case EQUAL -> {
                derive(15);
                yield new ParseTree("SimpleCond", Arrays.asList(match(LexicalUnit.EQUAL), exprArith(), simpleCond()));
            }
            default -> {
                derive(16);
                yield new ParseTree("SimpleCond", Collections.singletonList(epsilon()));
            }
        };
    }

    /**
     * ExprArith rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree exprArith() throws Exception, ParserException {
        derive(17);
        return new ParseTree("ExprArith", Arrays.asList(product(), exprArith_bis()));
    }

    /**
     * ExprArith' rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree exprArith_bis() throws Exception, ParserException {
        return switch (next.getType()) {
            case PLUS -> {
                derive(18);
                yield new ParseTree("ExprArith'", Arrays.asList(match(LexicalUnit.PLUS), product(), exprArith_bis()));
            }
            case MINUS -> {
                derive(19);
                yield new ParseTree("ExprArith'", Arrays.asList(match(LexicalUnit.MINUS), product(), exprArith_bis()));
            }
            default -> {
                derive(20);
                yield new ParseTree("ExprArith'", Collections.singletonList(epsilon()));
            }
        };
    }

    /**
     * Product rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree product() throws Exception, ParserException {
        derive(21);
        return new ParseTree("Product", Arrays.asList(atom(), product_bis()));
    }

    /**
     * Product' rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree product_bis() throws Exception, ParserException {
        return switch (next.getType()) {
            case TIMES -> {
                derive(22);
                yield new ParseTree("Product'", Arrays.asList(match(LexicalUnit.TIMES), atom(), product_bis()));
            }
            case DIVIDE -> {
                derive(23);
                yield new ParseTree("Product'", Arrays.asList(match(LexicalUnit.DIVIDE), atom(), product_bis()));
            }
            default -> {
                derive(24);
                yield new ParseTree("Product'", Collections.singletonList(epsilon()));
            }
        };
    }

    /**
     * IF rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree condition() throws Exception, ParserException {
        derive(25);
        return new ParseTree("IF", Arrays.asList(
                match(LexicalUnit.IF),
                match(LexicalUnit.LPAREN),
                cond(),
                match(LexicalUnit.RPAREN),
                match(LexicalUnit.THEN),
                code(),
                ifseq()));
    }

    /**
     * IFseq rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree ifseq() throws Exception, ParserException {
        if (next.getType().equals(LexicalUnit.ENDIF)) {
            derive(26);
            return new ParseTree("IFseq", Collections.singletonList(match(LexicalUnit.ENDIF)));
        }
        derive(27);
        return new ParseTree("IFseq", Arrays.asList(
                match(LexicalUnit.ELSE),
                code(),
                match(LexicalUnit.ENDIF)));
    }

    /**
     * WHILE rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree loop() throws Exception, ParserException {
        derive(28);
        return new ParseTree("WHILE", Arrays.asList(
                match(LexicalUnit.WHILE),
                match(LexicalUnit.LPAREN),
                cond(),
                match(LexicalUnit.RPAREN),
                match(LexicalUnit.DO),
                code(),
                match(LexicalUnit.END)));
    }

    /**
     * PRINT rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree print() throws Exception, ParserException {
        derive(29);
        return new ParseTree("PRINT", Arrays.asList(
                match(LexicalUnit.PRINT),
                match(LexicalUnit.LPAREN),
                match(LexicalUnit.VARNAME),
                match(LexicalUnit.RPAREN)));
    }

    /**
     * READ rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree read() throws Exception, ParserException {
        derive(30);
        return new ParseTree("READ", Arrays.asList(
                match(LexicalUnit.READ),
                match(LexicalUnit.LPAREN),
                match(LexicalUnit.VARNAME),
                match(LexicalUnit.RPAREN)));
    }

    /**
     * Atom rule
     * @return Parse Tree node
     * @throws Exception On error reading file
     * @throws ParserException when a parsing error occurs
     */
    private ParseTree atom() throws Exception, ParserException {
        return switch (next.getType()) {
            case VARNAME -> {
                derive(31);
                yield new ParseTree("Atom", Collections.singletonList(match(LexicalUnit.VARNAME)));
            }
            case NUMBER -> {
                derive(32);
                yield new ParseTree("Atom", Collections.singletonList(match(LexicalUnit.NUMBER)));
            }
            case LPAREN -> {
                derive(33);
                yield new ParseTree("Atom", Arrays.asList(match(LexicalUnit.LPAREN), cond(), match(LexicalUnit.RPAREN)));
            }
            case MINUS -> {
                derive(34);
                yield new ParseTree("Atom", Arrays.asList(match(LexicalUnit.MINUS), match(LexicalUnit.NUMBER)));
            }
            default -> throw new ParserException(next);
        };
    }

    /**
     * Empty node
     * @return Parse Tree node containing epsilon
     */
    private ParseTree epsilon() {
        return new ParseTree(new Symbol(LexicalUnit.EPSILON));
    }

    /**
     *
     * @return The leftmost derivation
     */
    public StringBuilder getDerivation() {
        return derivation;
    }
}
