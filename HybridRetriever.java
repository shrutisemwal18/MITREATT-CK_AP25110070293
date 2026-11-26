import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combined method: 0.5 * (keyword score / max keyword score) + 0.5 * TF-IDF cosine. */
public class HybridRetriever implements Retriever {
    private final Retriever keyword, tfidf;

    public HybridRetriever(Retriever keyword, Retriever tfidf) {
        this.keyword = keyword;
        this.tfidf = tfidf;
    }

    @Override public String name() { return "Hybrid (keyword + TF-IDF)"; }

    @Override public List<Scored> rank(String passage) {
        List<Scored> kw = keyword.rank(passage);
        List<Scored> tf = tfidf.rank(passage);
        double max = kw.isEmpty() ? 1.0 : kw.get(0).score;
        Map<String, Scored> merged = new LinkedHashMap<>();
        for (Scored s : kw) merged.put(s.t.id, new Scored(s.t, 0.5 * s.score / max, s.evidence));
        for (Scored s : tf) {
            Scored old = merged.get(s.t.id);
            if (old == null) merged.put(s.t.id, new Scored(s.t, 0.5 * s.score, ""));
            else merged.put(s.t.id, new Scored(s.t, old.score + 0.5 * s.score, old.evidence));
        }
        List<Scored> out = new ArrayList<>(merged.values());
        out.sort((a, b) -> {
            int c = Double.compare(b.score, a.score);
            return c != 0 ? c : a.t.id.compareTo(b.t.id);
        });
        return out;
    }
}
