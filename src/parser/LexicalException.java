package parser;

/**
 * Exception thrown by the analyzer to provide information on the lexical error
 */
public class LexicalException extends Exception{
    private final String message;

    /**
     * Create the exception message
     * @param line Line at which the exeption occurred
     * @param column Column at which the exception occurred
     * @param token Token that triggered the exception
     */
    public LexicalException(int line, int column, String token) {
        String info = "";
        if (token.equals("%")) {
            info = "Long comment not closed?";
        } else if (token.matches("[a-zA-Z]\\w*[A-Z]*\\w*")) {
            info = "Variables must consist of lowercase and numeric characters only.";
        } else if (token.contains(".")) {
            info = "Decimal numbers are not supported.";
        } else if (token.matches("0\\w+")) {
            info = "Integer numbers cannot begin with 0.";
        }
        this.message = String.format("Invalid token at %d:%d: %s\n%s", line + 1, column + 1, token, info);
    }

    /**
     * Create the exception message (for PROGNAME to differentiate it from variable identifiers)
     * @param token Token that triggered the exception
     */
    public LexicalException(String token) {
        this.message = String.format("The program name must begin with an Uppercase letter: %s.", token);
    }
    @Override
    public String getMessage() {
        return message;
    }
}
