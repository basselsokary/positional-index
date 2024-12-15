import java.util.*;
import java.util.stream.Collectors;

public class PhraseQuery {
    final static String[] operators = { "and not", "but not", "or not", "and", "or" };

    private Map<String, Map<String, List<Integer>>> positionalIndex;
    private Map<String, Map<String, Double>> tfidfMatrix;
    private Map<String, Term> termsMap;

    public PhraseQuery(Map<String, Map<String, List<Integer>>> positionalIndex,
            Map<String, Map<String, Double>> tfidfMatrix, Map<String, Term> terms) {
        this.positionalIndex = positionalIndex;
        this.tfidfMatrix = tfidfMatrix;
        this.termsMap = terms;
    }

    /** Checks if the words in a phrase in order */
    public int isPhraseInOrder(String doc, String term1, String term2, int start_index) {
        // Get positions of term1 and term2 in the specified document
        List<Integer> positionsTerm1, positionsTerm2;
        try {
            positionsTerm1 = positionalIndex.get(term1).getOrDefault(doc, Collections.emptyList());
            positionsTerm2 = positionalIndex.get(term2).getOrDefault(doc, Collections.emptyList());
        } catch (NullPointerException e) {
            return -1;
        }

        // If either term is missing in the document, return false
        if (positionsTerm1.isEmpty() || positionsTerm2.isEmpty())
            return -1;

        // Check if a position of term2 comes exactly after term1
        for (var i : positionsTerm1) {
            for (var j : positionsTerm2) {
                if (j > i + 1)
                    break; // We don't need to go any further

                if (start_index == 0 && j == i + 1)
                    return j;
                else if (j == i + 1 && start_index == i)
                    return j;
            }
        }

        return -1;
    }

    /** Get documents matching a phrase */
    private Set<String> getDocumentsForTerm(String phrase) {
        Set<String> matchedDocs = new HashSet<>();
        if (phrase.contains(" ")) {
            String[] terms = phrase.split(" ");
            boolean flag = false;
            var term = termsMap.get(terms[0]);
            if (term != null) {
                for (var doc : term.getDocFreq().keySet()) { // Only gets the documents that terms[0] in it
                    int end_index = 0;
                    for (int i = 0; i < terms.length - 1; i++) {
                        if ((end_index = isPhraseInOrder(doc, terms[i], terms[i + 1], end_index)) > 0)
                            flag = true;
                        else {
                            flag = false;
                            break;
                        }
                    }
                    if (flag)
                        matchedDocs.add(doc);
                    flag = false;
                }
            }
        } else
            matchedDocs.addAll(positionalIndex.getOrDefault(phrase, Collections.emptyMap()).keySet());
        return matchedDocs;
    }

    /** Handle OR in query, combine all the documents that in docs1 and docs2 */
    private Set<String> handleOr(Set<String> docs1, Set<String> docs2) {
        docs1.addAll(docs2);
        return docs1;
    }

    /**
     * Handle Or NOT in query, remove all the documents in docs1 that are not
     * contained in docs2
     */
    private Set<String> handleOrNot(Set<String> docs1, Set<String> docs2) {
        // Set<String> temp = Set.copyOf(tfidfMatrix.keySet());
        // Set<String> temp = tfidfMatrix.keySet();
        Set<String> temp = new HashSet<>(tfidfMatrix.keySet());
        temp.removeAll(docs2);
        docs1.addAll(temp);
        return docs1;
    }

    /**
     * Handle AND in query, remove all the documents in docs1 that are not contained
     * in docs2
     */
    private Set<String> handleAnd(Set<String> docs1, Set<String> docs2) {
        docs1.retainAll(docs2);
        return docs1;
    }

    /**
     * Handle AND NOT in query, remove all the documents in docs1 that are contained
     * in docs2
     */
    private Set<String> handleAndNot(Set<String> docs1, Set<String> docs2) {
        docs1.removeAll(docs2);
        return docs1;
    }

    /** Compute query vector length */
    public double getQueryVectorLength(List<String> queryTerms) {
        double length = 0.0;
        Term exTerm;
        for (String term : queryTerms) {
            double idf = 0.0;
            if ((exTerm = termsMap.get(term)) == null)
                continue;

            idf = exTerm.getIDF();
            double tfidf = TFIDFCalculator.tf_weight(getTermFrequencyInQuery(term, queryTerms)) * idf;
            length += Math.pow(tfidf, 2);
        }
        return Math.sqrt(length);
    }

    /** Compute document vector length */
    public double getDocumentVectorLength(String docId) {
        double length = 0.0;
        var doc = tfidfMatrix.get(docId);
        if (doc != null) {
            for (var entry : doc.values()) {
                length += Math.pow(entry, 2);
            }
        }

        return Math.sqrt(length);
    }

    /** Compute term frequency in query */
    public int getTermFrequencyInQuery(String term, List<String> queryTerms) {
        int frequency = 0;
        for (String queryTerm : queryTerms) {
            if (queryTerm.equals(term)) {
                frequency++;
            }
        }
        return frequency;
    }

    /** Compute query vector */
    private Map<String, Double> computeQueryVector(List<String> queryTerms) {
        Map<String, Double> queryVector = new HashMap<>();
        Term exTerm;
        for (String term : queryTerms) {
            double idf = 0.0;
            if ((exTerm = termsMap.get(term)) != null)
                idf = exTerm.getIDF();
            double tfidf = TFIDFCalculator.tf_weight(getTermFrequencyInQuery(term, queryTerms)) * idf;
            queryVector.put(term, tfidf);
        }

        return queryVector;
    }

    /** Compute cosine similarity */
    private double computeCosineSimilarity(Map<String, Double> queryVector, Map<String, Double> docVector,
            double docVectorLength, double queryVectorLength) {
        if (queryVectorLength == 0 || docVectorLength == 0)
            return 0.0;
        double dotProduct = 0.0;

        for (String term : queryVector.keySet()) {
            double queryWeight = queryVector.getOrDefault(term, 0.0);
            double docWeight = docVector.getOrDefault(term, 0.0);

            dotProduct += queryWeight * docWeight;
        }

        return dotProduct / (queryVectorLength * docVectorLength);
    }

    /** Rank documents based on similarity */
    private List<Map.Entry<String, Double>> rankDocuments(Map<String, Double> similarityScores) {
        return similarityScores.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Descending order
                .collect(Collectors.toList());
    }

    /** Parse query with boolean operators */
    public Set<String> evaluateQuery(String query) {
        return processQuery(query);
    }

    /** Process query recursively */
    private Set<String> processQuery(String query) {
        // Base case: if the query is a single term, return its documents
        if (!containsOperator(query))
            return getDocumentsForTerm(query.trim());

        String operator = findOperator(query);
        String[] terms = splitQuery(query, operator);

        Set<String> term1Docs = processQuery(terms[0].trim());
        Set<String> term2Docs = processQuery(terms[1].trim());

        // Handle the specific operator
        return applyOperator(term1Docs, term2Docs, operator);
    }

    /** Check if query contains a boolean operator */
    private boolean containsOperator(String query) {
        for (String op : operators) {
            if (query.contains(" " + op + " ")) {
                return true;
            }
        }
        return false;
    }

    /** Find the boolean operator */
    private String findOperator(String query) {
        for (String op : operators) {
            if (query.contains(" " + op + " ")) {
                return op;
            }
        }

        return "";
    }

    /** Split query by operator */
    private String[] splitQuery(String query, String operator) {
        int index = query.indexOf(" " + operator + " ");
        var x = new String[] {
                query.substring(0, index),
                query.substring(index + operator.length() + 2)
        };

        return x;
    }

    /** Applying and handeling the operator */
    private Set<String> applyOperator(Set<String> term1Docs, Set<String> term2Docs, String operator) {
        switch (operator) {
            case "and not":
            case "but not":
                return handleAndNot(term1Docs, term2Docs);
            case "or not":
                return handleOrNot(term1Docs, term2Docs);
            case "or":
                return handleOr(term1Docs, term2Docs);
            case "and":
                return handleAnd(term1Docs, term2Docs);
            default:
                return new HashSet<>();
        }
    }

    /** Main function to handle phrase queries */
    public List<String> search(String query) {
        query = query.toLowerCase();
        Set<String> matchedDocuments = new HashSet<>();
        for (String op : operators) {
            if (query.trim().equals(op)) {
                System.out.println("There is no documents returned!");
                return new ArrayList<String>();
            }
        }

        matchedDocuments = evaluateQuery(query);
        // System.out.println(query);
        List<String> termsInQuery = Arrays.asList(query.split(" "));
        Map<String, Double> queryVector = computeQueryVector(termsInQuery);
        // queryVector.forEach((x, y) -> System.out.println(x + "\t" + y));
        double queryVectorLength = getQueryVectorLength(termsInQuery);
        // System.out.println("qu length " + queryVectorLength);
        Map<String, Double> similarityScores = new HashMap<>(); // document, similarity

        for (String docID : matchedDocuments) {
            Map<String, Double> docVector = tfidfMatrix.getOrDefault(docID, new HashMap<>());
            // docVector.forEach((x, y) -> System.out.println(x + "\t" + y));
            // System.out.println();
            double docVectorLength = getDocumentVectorLength(docID);
            double similarity = computeCosineSimilarity(queryVector, docVector, docVectorLength, queryVectorLength);
            similarityScores.put(docID, similarity);
        }

        // Rank documents by similarity
        List<Map.Entry<String, Double>> rankedDocs = rankDocuments(similarityScores);

        // Display results
        if (!rankedDocs.isEmpty()) {
            System.out.println("Ranked Documents:");
            for (Map.Entry<String, Double> entry : rankedDocs) {
                System.out.printf("Document: %s, Similarity: %.4f\n", entry.getKey(), entry.getValue());
            }
        } else
            System.out.println("There is no documents returned!");

        // Return ranked documents
        return rankedDocs.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
