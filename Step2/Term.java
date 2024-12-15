import java.util.List;
import java.util.Map;

public class Term {
    private String termName;
    private double IDF;
    
    private Map<String, List<Integer>> docFreq;
    
    public Term(String termName, double idf, Map<String, List<Integer>> docFreq) {
        this.termName = termName;
        IDF = idf;
        this.docFreq = docFreq;
    }
    
    public void setDocFreq(Map<String, List<Integer>> docFreq) {
        this.docFreq = docFreq;
    }

    public Map<String, List<Integer>> getDocFreq() {
        return docFreq;
    }

    public String getTermName() {
        return termName;
    }

    public double getIDF() {
        return IDF;
    }

    @Override
    public boolean equals(Object obj) {
        var term = (Term) obj;
        return term.getTermName().equals(termName);
    }

    @Override
    public String toString() {
        return termName + "\t" + IDF;
    }

}
