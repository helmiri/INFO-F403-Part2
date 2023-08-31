package grammar;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Context free grammar
 */
public class CFG {
    // Representation of the grammar
    private HashMap<String, List<Rule>> grammar = new HashMap<>();
    // Array to preserve the order of the rules because HashMap doesn't and there is no guarantee that LinkedHashMap will after removing useless and unreachable symbols
    private final ArrayList<String> variableOrd = new ArrayList<>();
    private final Pattern variablePattern = Pattern.compile("<[^ ]+?>"); // Pattern used to search for variables in rules

    private final HashMap<String, Set<Terminal>> firstSets = new HashMap<>();
    private final HashMap<String, Set<Terminal>> followSets = new HashMap<>();

    /**
     * Parses a grammar file
     * @param grammarFile The path to the grammar file
     * @throws IOException on error reading the file
     */
    public CFG(String grammarFile) throws IOException {
        try(FileInputStream file = new FileInputStream(grammarFile)){
            Scanner reader = new Scanner(file);
            String key = null;
            int ruleNum = 0;
            while(reader.hasNextLine()){
                ruleNum += 1;
                String[] line = reader.nextLine().split("->");
                String nextKey = line[0].trim();
                if (nextKey.length() > 0){
                    key = nextKey;
                    variableOrd.add(key);
                }
                append(grammar, key, line[1].trim(), ruleNum);
            }
        } catch (IOException e) {
            // Close the inputStream and throw back the exception
            throw new IOException(e);
        }
    }

    /**
     * Removes unproductive symbols from the grammar.
     * The algorithm will iterate through every rule and :
     *  - In the first iteration, it will transfer the rules with terminals only to the new grammar and remove them from the old one
     *  - From the second iteration onwards, it will transfer the rules with terminals and/or terminal variables contained
     *    in the new grammar from iteration 1 and remove them from the old one.
     *  - The algorithm will proceed until no rules have been added to the new clean grammar.
     */
    public void removeUnproductiveSymbols(){
        HashMap<String, List<Rule>> newGrammar = new HashMap<>();
        ArrayList<Object[]> removedRules = new ArrayList<>();
        boolean change = true;
        while(change) {
            if (getProductiveRules(newGrammar, removedRules)) {
                change = false;
            } else{
                // Remove productive rules from the grammar to get ready for the next iteration
                for (Object[] keySet : removedRules) {
                    String key = (String) keySet[0];
                    grammar.get(key).remove((Rule)keySet[1]);
                    if (grammar.get(key).isEmpty()) {
                        grammar.remove(key);
                    }
                }
                removedRules.clear();
            }
        }
        grammar = newGrammar;
    }

    /**
     * Scans the grammar and transfers the productive rules to newGrammar
     * @param newGrammar The new clean grammar to replace the old dirty one
     * @param removedRules Array of rules to be removed from the old grammar
     * @return True if no productive rules have been found, false otherwise
     */
    private boolean getProductiveRules(HashMap<String, List<Rule>> newGrammar, ArrayList<Object[]> removedRules) {
        for (String key : grammar.keySet()) {
            for (Rule rule : grammar.get(key)) {
                // For each rule in the grammar, search for variables
                Matcher symbolMatcher = variablePattern.matcher(rule.toString());
                boolean isTerminal = true;
                int count = 0;
                while (symbolMatcher.find()) {
                    count += 1;
                    // If the new grammar does not contain the variable, then the rule is not terminal
                    if (!newGrammar.containsKey(symbolMatcher.group())) {
                        isTerminal = false;
                        break;
                    }
                }
                // Count will be 0 if the rule contains terminals only
                // isTerminal will be True if the rule contains terminals and/or terminal variables contained in newGrammar
                if (isTerminal || count == 0) {
                    // Update the new grammar and rules to be removed from the old one
                    append(newGrammar, key, rule.rule(), rule.num());
                    Object[] keySet = {key, rule};
                    removedRules.add(keySet);
                }
            }
        }
        return removedRules.isEmpty();
    }

    /**
     * Removes unreachable rules from the starting one
     * - Starting from the start state, scan its rules and add the variables to a queue if not contained in reachableSymbols
     *   to avoid endless loops in recursive grammars
     * - For every variable in the queue, scan its rules and do the same as in the previous steps
     * - The algorithm will stop when no variables are found in the queue
     * - Finally, get the intersection between variableOrd and reachable rules. What is left in variableOrd is the reachable rules
     *   while maintaining their order
     */
    public void removeUnreachable(){
        HashSet<String> reachableSymbols = new HashSet<>();
        Queue<String> foundSymbols = new LinkedList<>();
        foundSymbols.add(variableOrd.get(0));
        while(!foundSymbols.isEmpty()){
            String currentSymbol = foundSymbols.poll();
            reachableSymbols.add(currentSymbol);
            for (Rule rule : grammar.get(currentSymbol)){
                Matcher symbolMatcher = variablePattern.matcher(rule.toString());
                while(symbolMatcher.find()){
                    String symbol = symbolMatcher.group();
                    if(!reachableSymbols.contains(symbol)) {
                        foundSymbols.add(symbolMatcher.group());
                    }
                }
            }
        }
        variableOrd.retainAll(reachableSymbols);
    }

    /**
     * Add a rule to the grammar
     * @param grammar grammar to be updated
     * @param key Variable containing the rule
     * @param value The rule in string format
     * @param order Number of the rule
     */
    private void append(HashMap<String, List<Rule>> grammar, String key, String value, int order) {
        if (!grammar.containsKey(key)){
            grammar.put(key, new ArrayList<>());
        }
        grammar.get(key).add(new Rule(value, order));
    }

    /**
     * Computes the first set of the rule
     * @param rule Rule to be computed
     * @return The first set of the rule
     */
    private Set<Terminal> getFirst(Rule rule) {
        Set<Terminal> result = new HashSet<>();
        String[] tokenized = rule.rule().split(" ");
        for (String token : tokenized) {
            Matcher symbolMatcher = variablePattern.matcher(token);
            if (symbolMatcher.find()) {
                // Deep copy the set so any modifications to Terminal won't be reflected in the sets of other rules
                Set<Terminal> temp = firstSets.get(symbolMatcher.group()).stream().map(Terminal::new).collect(Collectors.toSet());
                // If rule contains epsilon remove it
                // If it doesn't, compute the union of the current first(rule) set and first(symbolMatcher.group()) and stop searching
                if (!temp.remove(new Terminal())) {
                    result.addAll(temp);
                    break;
                }
                // The first(symbolMatcher.group()) set contained epsilon, update current set and keep iterating
                result.addAll(temp);
            } else {
                // It's a terminal, add it to the set and stop
                result.add(new Terminal(token, rule));
                break;
            }
        }
        // Store the current rule in the terminals found for use in the action table
        for (Terminal t : result) {
            t.setRule(rule);
        }
        return result;
    }

    /**
     * Computes the first sets of the grammar
     */
    public void computeFirstSets() {
        // Initialize to empty
        for (String symbol : variableOrd) {
            firstSets.put(symbol, new HashSet<>());
        }

        boolean changed = true;
        while(changed) {
            changed = false;
            for (String symbol : variableOrd) {
                for (Rule rule : grammar.get(symbol)) {
                    Set<Terminal> previous = new HashSet<>(firstSets.get(symbol)); // Copy set for comparison
                    firstSets.get(symbol).addAll(getFirst(rule));
                    if (!previous.equals(firstSets.get(symbol))) {
                        // Keep iterating until no change detected
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * Computes the Follow1 sets for the grammar
     */
    public void computeFollowSets() {
        if (firstSets.isEmpty()) {
            computeFirstSets();
        }

        // Initialize each rule's follow set to empty
        for (String symbol : variableOrd) {
            followSets.put(symbol, new HashSet<>());
        }
        followSets.get(variableOrd.get(0)).add(new Terminal()); // Start rule has epsilon as follow1

        boolean changed = true;
        while(changed) {
            changed = false;

            // For each Variable V in the grammar
            for (int i = 0; i < variableOrd.size(); i++) {
                Set<Terminal> copy = new HashSet<>(followSets.get(variableOrd.get(i))); // Copy for comparison
                // Iterate through all the Variables Y in the grammar
                for (int j = 0; j < variableOrd.size(); j++) {
                    if (i == j) continue; // Y != V
                    // For each rule in with LHS = Y
                    for (Rule rule : grammar.get(variableOrd.get(j))){
                        // Compute Follow1(V)
                        followSets.get(variableOrd.get(i)).addAll(getFollow(variableOrd.get(i), variableOrd.get(j), rule.rule()));
                    }
                }
                if (!copy.equals(followSets.get(variableOrd.get(i)))) {
                    changed = true; // Follow1(V) was changed. Keep iterating.
                }
            }
        }
        followSets.get(variableOrd.get(0)).remove(new Terminal()); // No longer needed
    }

    /**
     * Compute Follow1(currentVar)
     * @param currentVar The variable for which to compute the Follow1 set
     * @param inVar The variable of the rule we're looking at
     * @param rule The rule to search currentVar occurrences in with LHS = inVar
     * @return The Follow1(currentVar) set
     */
    private Set<Terminal> getFollow(String currentVar, String inVar, String rule) {
        Set<Terminal> tempRes  = new HashSet<>();
        // Tokenize the rule string and iterate through it
        String[] tokenizedRule = rule.split(" ");
        for (int k = 0; k < tokenizedRule.length; k++) {
            // Search for currentVar occurrences
            if (tokenizedRule[k].equals(currentVar)) {
                // Get next token if not the end of the string
                if (k + 1 < tokenizedRule.length) {
                    Matcher symbolMatcher = variablePattern.matcher(tokenizedRule[k + 1]);
                    // If next token is a Variable
                    if (symbolMatcher.find()) {
                        // Add its first set to Follow1(currentVar)
                        tempRes.addAll(firstSets.get(symbolMatcher.group()));
                    } else {
                        // It's a terminal
                        tempRes.add(new Terminal(tokenizedRule[k + 1]));
                    }
                    // If it contains epsilon, remove it and look at the remaining tokens
                    if (tempRes.remove(new Terminal())) {
                       tempRes.addAll(getFirstsFollowing(inVar, rule, k));
                       return tempRes;
                    }
                } else {
                    tempRes.addAll(followSets.get(inVar));
                }
            }
        }
        return tempRes;
    }

    /**
     * Helper method to getFollow. Iterate through a rule string computing each token's First1 set until no epsilon is returned or end of string is reached
     * @param inVar Variable producing the rule to search in
     * @param rule The rule string
     * @param k Starting index to iterate though
     * @return The combined First1 sets (if epsilons are found) of the tokens in the rule starting from index k
     */
    private Set<Terminal> getFirstsFollowing(String inVar, String rule, int k) {
        Set<Terminal> result = new HashSet<>();
        String[] tokenizedRule = rule.split(" ");
        for (int i = k + 1; i < tokenizedRule.length; i++) {
            Matcher varMatcher = variablePattern.matcher(tokenizedRule[i]);
            if (varMatcher.find()) {
                result.addAll(firstSets.get(varMatcher.group()));
            } else {
                result.add(new Terminal(tokenizedRule[i]));
            }
            // If an epsilon is not found while computing the First1 set, computation complete
            if (!result.remove(new Terminal())) {
                return result;
            }
        }
        // We reached the end of the rule string. Add Follow1(inVar) where inVar is the LHS variable of the rule we looked in
        result.addAll(followSets.get(inVar));
        return result;
    }

    /**
     * Get the follow sets
     * @return The follow sets
     */
    public HashMap<String, Set<Terminal>> getFollowSets() {
        return followSets;
    }

    /**
     * Get the first sets
     * @return The first sets
     */
    public HashMap<String, Set<Terminal>> getFirstSets() {
        return firstSets;
    }

    /**
     * Get the map iteration order
     * @return Array of hashmap key in the correct iteration order
     */
    public List<String> getOrder() {
        return variableOrd;
    }

    /**
     * Removes useless and unreachable symbols
     */
    public void cleanup() {
        removeUnproductiveSymbols(); removeUnreachable();
    }

    /**
     * Converts grammar to Tex format
     * @return LaTex representation
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (String symbol : variableOrd) {
            boolean added = false;
            List<Rule> rules = grammar.get(symbol);
            rules.sort(Comparator.comparingInt(Rule::num));
            for (Rule rule : rules) {
                if (!added){
                    output.append(String.format("(%s) %s -> %s\\\\", rule.num(), symbol, rule));
                    added = true;
                } else {
                    output.append(String.format("(%s) \\verb+%s+ -> %s\\\\", rule.num(), String.join("",Collections.nCopies(symbol.length(), " ")), rule));
                }
            }
        }
        return output.toString();
    }

    /**
     * Converts a given set to string representation
     * @param set first or follow set
     * @return Formatted string
     */
    private String setToString(HashMap<String, Set<Terminal>> set) {
        StringBuilder str = new StringBuilder();

        for (String key: variableOrd) {
            str.append(String.format("%s | %s\n", key, set.get(key).stream().toList()));
        }
        return str.toString();
    }

    /**
     * Get string representation of the first set
     * @return set in string format
     */
    public String firstToString() {
        return setToString(firstSets);
    }

    /**
     * Get string representation of the follow set
     * @return set in string format
     */
    public String followToString() {
        return setToString(followSets);
    }
}

