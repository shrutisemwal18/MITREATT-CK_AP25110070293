import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Baseline 2: TF-IDF vectors + cosine similarity between the passage and each technique document. */
public class TfidfRetriever implements Retriever {
    private final List<Technique> techniques;
    private final Map<String, Double> idf = new HashMap<>();
    private final List<Map<String, Double>> docVectors = new ArrayList<>();

    public TfidfRetriever(List<Technique> techniques) {
        this.techniques = techniques;
        List<List<String>> docs = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();
        for (Technique t : techniques) {
            List<String> toks = new ArrayList<>();
            toks.addAll(Text.tokens(t.name));
            toks.addAll(Text.tokens(t.name));                 // name counts double
            for (String k : t.keywords) toks.addAll(Text.tokens(k));
            toks.addAll(Text.tokens(t.description));
            docs.add(toks);
            for (String w : new HashSet<>(toks)) df.merge(w, 1, Integer::sum);
        }
        int n = techniques.size();
        for (Map.Entry<String, Integer> e : df.entrySet())
            idf.put(e.getKey(), Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0);
        for (List<String> d : docs) docVectors.add(vectorize(d));
    }

    private Map<String, Double> vectorize(List<String> toks) {
        Map<String, Integer> tf = new HashMap<>();
        for (String w : toks) tf.merge(w, 1, Integer::sum);
        Map<String, Double> v = new HashMap<>();
        double norm = 0;
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            Double w = idf.get(e.getKey());
            if (w == null) continue;                          // unseen word: ignore
            double x = (1 + Math.log(e.getValue())) * w;
            v.put(e.getKey(), x);
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) for (Map.Entry<String, Double> e : v.entrySet()) e.setValue(e.getValue() / norm);
        return v;
    }

    @Override public String name() { return "TF-IDF + cosine"; }

    @Override public List<Scored> rank(String passage) {
        Map<String, Double> q = vectorize(Text.tokens(passage));
        List<Scored> out = new ArrayList<>();
        for (int i = 0; i < techniques.size(); i++) {
            Map<String, Double> d = docVectors.get(i);
            double dot = 0;
            for (Map.Entry<String, Double> e : q.entrySet()) {
                Double x = d.get(e.getKey());
                if (x != null) dot += e.getValue() * x;
            }
            if (dot > 0) out.add(new Scored(techniques.get(i), dot, ""));
        }
        out.sort((a, b) -> {
            int c = Double.compare(b.score, a.score);
            return c != 0 ? c : a.t.id.compareTo(b.t.id);
        });
        return out;
    }
}
