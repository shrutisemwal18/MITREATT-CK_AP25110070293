import java.util.ArrayList;
import java.util.List;

/** Baseline 1: keyword / phrase matching. Longer matched phrases score higher. */
public class KeywordMatcher implements Retriever {
    private final List<Technique> techniques;
    private final List<List<String>> normKeywords = new ArrayList<>();

    public KeywordMatcher(List<Technique> techniques) {
        this.techniques = techniques;
        for (Technique t : techniques) {
            List<String> ks = new ArrayList<>();
            ks.add(Text.norm(t.name));
            for (String k : t.keywords) ks.add(Text.norm(k));
            normKeywords.add(ks);
        }
    }

    @Override public String name() { return "Keyword matching"; }

    @Override public List<Scored> rank(String passage) {
        String text = Text.norm(passage);
        List<Scored> out = new ArrayList<>();
        for (int i = 0; i < techniques.size(); i++) {
            double score = 0;
            List<String> hits = new ArrayList<>();
            for (String k : normKeywords.get(i)) {
                if (k.trim().isEmpty()) continue;
                if (text.contains(k)) { score += Text.wordCount(k); hits.add(k.trim()); }
            }
            if (score > 0) out.add(new Scored(techniques.get(i), score, String.join(", ", hits)));
        }
        out.sort((a, b) -> {
            int c = Double.compare(b.score, a.score);
            return c != 0 ? c : a.t.id.compareTo(b.t.id);
        });
        return out;
    }
}
