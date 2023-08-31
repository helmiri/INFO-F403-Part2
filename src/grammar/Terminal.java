package grammar;

import java.util.Objects;

/**
 * Class representing a terminal used to keep track of which rule results in which terminal in the first/follow tables
 */
public class Terminal {
    private final String terminal;
    private Rule rule;

    /**
     *
     * @param terminal terminal
     * @param rule rule that produced the terminal
     */
    public Terminal(String terminal, Rule rule) {
        this.terminal = terminal;
        this.rule = rule;
    }

    /**
     *
     * @param terminal the terminal
     */
    public Terminal(String terminal) {
        this.terminal = terminal;
        this.rule = null;
    }

    public Terminal() {
        this.terminal = "e";
        this.rule = null;
    }

    public Terminal(Terminal terminal) {
        this.terminal = terminal.getTerminal();
        this.rule = terminal.getRule();
    }

    public Rule getRule() {
        return rule;
    }

    public String getTerminal() {
        return terminal;
    }

    @Override
    public String toString() {
        return terminal;
    }

    public void setRule(Rule newRule) {
        rule = newRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal1 = (Terminal) o;
        return Objects.equals(terminal, terminal1.terminal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminal);
    }

}
