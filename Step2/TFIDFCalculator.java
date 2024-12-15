import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TFIDFCalculator {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String inputFile = "positional_index.txt";
        //  term        doc     positions
        Map<String, Map<String, List<Integer>>> positionalIndex = new HashMap<>();
        //  doc         term    tf-idf
        Map<String, Map<String, Double>> tfidfMatrix = new HashMap<>();
        
        // Read the positional index file
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 2) continue;
                
                String term = parts[0];
                String docDetails = parts[1];
                String[] entries = docDetails.split(";");
                for (String entry : entries) {
                    // Extract document ID and positions
                    Pattern pattern = Pattern.compile("(\\S+):\\s+(.+)");
                    Matcher matcher = pattern.matcher(entry.trim());

                    if (matcher.find()) {
                        String docID = matcher.group(1);
                        String[] positions = matcher.group(2).split(",\\s*");
                        List<Integer> positionList = new ArrayList<>();
                        for (String pos : positions) {
                            positionList.add(Integer.parseInt(pos));
                            }

                        positionalIndex
                            .computeIfAbsent(term, _ -> new HashMap<>())
                            .putIfAbsent(docID, positionList);
                        tfidfMatrix
                            .putIfAbsent(docID, new HashMap<>());
                    }
                }
            }
        }
        int totalDocuments = tfidfMatrix.keySet().size();

        Map<String, Term> terms = new HashMap<>();
        for (var entry : positionalIndex.entrySet()) {
            var docFreqMap = entry.getValue(); // doc, positions
            int docsContainingTerm = docFreqMap.size();
            double IDF = Math.log10((double) totalDocuments / docsContainingTerm); // calculating IDF

            var newTerm = new Term(entry.getKey(), IDF, entry.getValue());
            terms.putIfAbsent(newTerm.getTermName(), newTerm);
            tfidfMatrix = calcTermsTF_IDF(newTerm, tfidfMatrix);
        }

        int[] columnWidths = new int[totalDocuments + 1];
        Arrays.fill(columnWidths, 10);
        columnWidths[0] = 15;

        displayFrequences(terms.values(), tfidfMatrix.keySet(), columnWidths);
        displayIDF(terms.values());
        displayTF_IDF(terms.values(), tfidfMatrix.keySet(), columnWidths);

        var query = new PhraseQuery(positionalIndex, tfidfMatrix, terms);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter phrase query, or press CTRL + Z to exit.");

        System.out.print("Enter query: ");
        while (scanner.hasNextLine()) { // Reads until EOF
            String queryStr = scanner.nextLine();
            // var res =
            query.search(queryStr);
            // res.forEach(r -> System.out.println(r));
            
            System.out.println("----------------------------------------");
            System.out.print("Enter query: ");
        }
        scanner.close();
    }

    static void displayFrequences(Collection<Term> terms, Set<String> docsIDs, int[] columnWidths){
        String[] docsIDsSorted = docsIDs.toArray(String[]::new);
        Arrays.sort(docsIDsSorted);
        
        System.out.print(padString("Term", columnWidths[0]));
        for (int i=0; i < docsIDsSorted.length; i++){
            System.out.print(padString(docsIDsSorted[i], columnWidths[i+1]));
        }
        System.out.println();
        
        boolean flag = true;
        for (Term term : terms) {
            System.out.print(padString(term.getTermName(), columnWidths[0]));
            for (String docID : docsIDsSorted) {
                int i = 1;
                for (var entry : term.getDocFreq().entrySet()) {
                    if (docID.equals(entry.getKey())){
                        System.out.print(padString(entry.getValue().size() + "", columnWidths[i++]));
                        flag = false;
                        break;
                    }
                }
                if (flag)
                    System.out.print(padString("0", columnWidths[i++]));

                flag = true;
            }
            System.out.println();
        }
    }

    static void displayIDF(Collection<Term> terms){
        for (Term term : terms) {
            System.out.printf("%s\t%.4f\n", term.getTermName(), term.getIDF());
        }
    }

    static void displayTF_IDF(Collection<Term> terms, Set<String> docsIDs, int[] columnWidths){
        String[] docsIDsSorted = docsIDs.toArray(String[]::new);
        Arrays.sort(docsIDsSorted);

        System.out.print(padString("Term", columnWidths[0]));
        for (int i=0; i < docsIDsSorted.length; i++){
            System.out.print(padString(docsIDsSorted[i], columnWidths[i+1]));
        }
        System.out.println();
        
        boolean flag = true;
        for (Term term : terms) {
            System.out.print(padString(term.getTermName(), columnWidths[0]));
            for (String docID : docsIDsSorted) {
                int i = 1;
                for (var entry : term.getDocFreq().entrySet()) {
                    if (docID.equals(entry.getKey())){
                        double TF_DIF = tf_weight(entry.getValue().size()) * term.getIDF();
                        System.out.print(padString(String.format("%.4f", TF_DIF), columnWidths[i++]));
                        flag = false;
                        break;
                    }
                }
                if (flag)
                    System.out.print(padString("0", columnWidths[i++]));

                flag = true;
            }
            System.out.println();
        }
    }

    /** Method to calculate tf-idf weight for each term being sent */
    static Map<String, Map<String, Double>> calcTermsTF_IDF(Term term, Map<String, Map<String, Double>> appendMap){
        for (var docsContainingTerm : term.getDocFreq().entrySet()) {
            double TF_DIF = tf_weight(docsContainingTerm.getValue().size()) * term.getIDF();
            appendMap
                .computeIfAbsent(docsContainingTerm.getKey(), _ -> new HashMap<>())
                .putIfAbsent(term.getTermName(), TF_DIF);
        }
        
        return appendMap;
    }

    /** logarithmically weighted term frequency */
    public static double tf_weight(int tf_raw) {
        if (tf_raw < 1)
            return 0.0;
        return 1 + Math.log10(tf_raw);
    }

    /** Method to pad or truncate strings to match column width */
    private static String padString(String str, int width) {
        if (str.length() > width)
            return str.substring(0, width - 3) + "..."; // Truncate and add "..."

        return String.format("%-" + width + "s", str); // Left-align padding
    }
}