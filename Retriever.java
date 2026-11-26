import java.util.List;

/** A method that ranks ATT&CK techniques for a passage of text (best first, only score > 0). */
public interface Retriever {
    String name();
    List<Scored> rank(String passage);
}
