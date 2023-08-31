package parser;

/**
 * Exception thrown when a parsing error occurs
 */
public class ParserException extends Throwable {
    private final LexicalUnit token;
    private final Symbol symbol;

    /**
     *
     * @param symbol The symbol that caused the exception
     */
    public ParserException(Symbol symbol){
        this.symbol = symbol;
        this.token = null;
    }

    /**
     *
     * @param symbol The symbol that caused the exception
     * @param token The expected token
     */
    public ParserException(Symbol symbol, LexicalUnit token) {
        this.symbol = symbol;
        this.token = token;
    }

    public String getMessage(){
        if (token == null) {
            return String.format("Unexpected token at %d:%d --> %s", symbol.getLine(), symbol.getColumn(), symbol.getType(), symbol.getValue());
        }
        return String.format("Parsing Error at %d:%d --> expected %s but got %s", symbol.getLine(), symbol.getColumn(), token, symbol.getType());}

}
