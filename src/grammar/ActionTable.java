package grammar;

import java.util.*;

/**
 * Class to compute the action table
 */
public class ActionTable {
    private final HashMap<String, Set<Terminal>> firstSets;
    private final HashMap<String, Set<Terminal>> followSets;
    private final HashMap<String, Rule> table;


    /**
     * Constructs an action table from the provided sets
     * @param firstSets The first sets of the grammar
     * @param followSets The follow sets of the grammar
     * @throws Exception If the grammar is not LL(1)
     */
    public ActionTable(HashMap<String, Set<Terminal>> firstSets, HashMap<String, Set<Terminal>> followSets) throws Exception {
        this.firstSets = new HashMap<>(firstSets);
        this.followSets = new HashMap<>(followSets);
        this.table = new HashMap<>();
        computeTable();
    }

    /**
     * Computes the action table of the provided grammar
     * @throws Exception If the language is not LL(1)
     */
    private void computeTable() throws Exception {
        for (String variable : firstSets.keySet()) {
            boolean containsEpsilon = false;
            Rule epsilonRule = null;
            for (Terminal terminal : firstSets.get(variable)) {
                if (terminal.getTerminal().equals("e")) {
                    containsEpsilon = true;
                    epsilonRule = terminal.getRule();
                } else {
                    updateTable(variable, terminal.getRule(), terminal);
                }
            }
            // If the firs set contains an epsilon, consider the follow set
            if (containsEpsilon) {
                for (Terminal terminal : followSets.get(variable)) {
                    updateTable(variable, epsilonRule, terminal);
                }
            }
        }
    }

    /**
     * Updates the action table with the provided parameters
     * @param variable variable being considered
     * @param rule Rule allowing transition from variable with look-ahead terminal
     * @param terminal Terminal being considered
     * @throws Exception When a conflict in the table is detected
     */
    private void updateTable(String variable, Rule rule, Terminal terminal) throws Exception {
        // The table already contains a rule at table[variable,terminal]
        if (table.containsKey(variable)) {
            throw new Exception(String.format("Conflict detected. Your grammar is not LL(1)\n%s -> %s\n%s -> %s",
                    variable, table.get(variable), variable, rule.toString()));
        }
        table.put(String.format("%s,%s", variable, terminal), rule);
    }

    @Override
    public String toString() {
        List<String> temp = new ArrayList<>();
        for (Map.Entry<String, Rule> entry: table.entrySet()) {
            temp.add(String.format("[ %s ] : %s", entry.getKey(), entry.getValue()));
        }
        Collections.sort(temp);
        return String.join("\n", temp);
    }
}
