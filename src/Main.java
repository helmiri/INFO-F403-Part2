import grammar.ActionTable;
import grammar.CFG;
import parser.ParseTree;
import parser.Parser;
import parser.ParserException;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EmptyStackException;

public class Main {

    public static void main(final String[] args) {
        String texFile = null, sourceFile;
        boolean printSets = false, printTable = false;

        if (args.length >= 2) {
            for (int i = 0; i < args.length - 1; i++) {
                switch (args[i]) {
                    case "-wt" -> {
                        texFile = args[i + 1];
                        i += 1;
                    }
                    case "-t" -> printTable = true;
                    case "-s" -> printSets = true;
                    default -> {
                        errorMSG();
                        return;
                    }
                }
            }
        } else if (args.length == 0) {
            errorMSG();
            return;
        }
        sourceFile = args[args.length - 1];
        if (sourceFile.equals(texFile)) {
            errorMSG();
            return;
        }
        startParser(texFile, sourceFile, printSets, printTable);
    }

    /**
     * Initiate parsing
     * @param texFile Parse Tree output
     * @param sourceFile File to be parsed
     * @param printSets Print first and follow sets?
     * @param printTable Print action table?
     */
    private static void startParser(String texFile, String sourceFile, boolean printSets, boolean printTable) {
        try {
            loadGrammar(printTable, printSets);
            writeToTex(texFile, initScan(sourceFile));
        } catch (FileNotFoundException e) {
            System.err.println("File not found : \"" + sourceFile + "\"");
        } catch (EmptyStackException e) {
            System.err.println("Trailing END with no matching BEGIN, WHILE or IF was detected.");
        } catch (Exception | ParserException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Writes the parse tree to Latex file
     * @param texFile Path to file
     * @throws IOException On error writing the file
     */
    private static void writeToTex(String texFile, String texTree) throws Exception {
        if (texFile != null) {
            try(FileWriter file = new FileWriter(texFile)) {
                file.write(texTree);
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * Initiates the file parsing
     *
     * @param reader InputStream reader
     * @return String representation of the successful leftmost derivation if found
     * @throws IOException On error reading or accessing the file
     */
    private static String initScan(String reader) throws Exception, ParserException {
        Parser parser = new Parser(reader);
        ParseTree tree = parser.parse();
        System.out.println(parser.getDerivation());
        return tree.toLaTeX();
    }

    /**
     * Loads grammar from file
     * @throws IOException on error reading the file
     */
    private static void loadGrammar(boolean printTable, boolean printSets) throws Exception {
        if (!printSets && !printTable) {
            return;
        }
        CFG grammar = new CFG("more/FORTRESS_grammar.txt");
        grammar.cleanup();
        grammar.computeFirstSets();
        grammar.computeFollowSets();
        ActionTable action = new ActionTable(grammar.getFirstSets(), grammar.getFollowSets());
        if (printTable) {
            System.out.println("****************** Action Table ******************");
            System.out.println(action);
        }
        if (printSets) {
            System.out.println("****************** First Sets ******************");
            System.out.println(grammar.firstToString());
            System.out.println("****************** Follow Sets ******************");
            System.out.println(grammar.followToString());
        }
    }

    /**
     * Error message displayed when the arguments are invalid
     */
    private static void errorMSG() {
        System.err.println("""
                                Usage: java -jar dist/part2.jar [OPTIONS] [SOURCE.FS]
                                \t- [OPTIONS] = -wt [FILE.TEX] : write parse tree to [FILE.TEX]
                                \t\t\t\t  -t  : print action table
                                \t\t\t\t  -s  : print first and follow sets""");
    }
}