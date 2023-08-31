package grammar;

/**
 * Represents a rule in the grammar
 */
public record Rule(String rule, int num) {
    /**
     * @param rule Rule in string representation
     * @param num  Order of the rule
     */
    public Rule {
    }

    @Override
    public String toString() {
        return rule;
    }
}
