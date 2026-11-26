/** A candidate technique with its score and the keyword evidence (if any) that supports it. */
public class Scored {
    public final Technique t;
    public final double score;
    public final String evidence;   // matched keywords, empty if the match is purely semantic

    public Scored(Technique t, double score, String evidence) {
        this.t = t;
        this.score = score;
        this.evidence = evidence;
    }
}
