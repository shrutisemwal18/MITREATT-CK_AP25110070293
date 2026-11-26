import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Desktop (Swing) application: maps sentences of a threat report to MITRE ATT&CK techniques. */
public class Main extends JFrame {
    private static final String SAMPLE =
        "The attackers sent spearphishing emails with a malicious attachment, a macro enabled Word document. "
      + "The macro launched PowerShell with an encoded command that downloaded a second stage using certutil. "
      + "The implant created a scheduled task for persistence and later dumped credentials from LSASS memory. "
      + "Operators then moved laterally over Remote Desktop and compressed documents into a password protected RAR archive. "
      + "The group abused a legitimate remote support tool already trusted in the environment to keep access.";

    private final Path datasetDir;
    private final List<Retriever> retrievers = new ArrayList<>();
    private final JComboBox<String> methodBox = new JComboBox<>();
    private final JTextArea input = new JTextArea(SAMPLE);
    private final JSpinner topN = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    private final JSpinner minScore = new JSpinner(new SpinnerNumberModel(0.15, 0.0, 1.0, 0.05));
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"#", "Evidence passage", "ATT&CK ID", "Technique", "Tactic", "Score", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextArea detail = new JTextArea(7, 60);
    private final JTextArea evalOut = new JTextArea();
    private final JLabel statusBar = new JLabel(" ");
    private final List<Pipeline.Finding> findings = new ArrayList<>();
    private final List<Technique> techniques;

    public Main(Path datasetDir, List<Technique> techniques) {
        super("ATT&CK Technique Extractor - CTI Threat Report Mapper");
        this.datasetDir = datasetDir;
        this.techniques = techniques;
        Retriever kw = new KeywordMatcher(techniques);
        Retriever tf = new TfidfRetriever(techniques);
        retrievers.add(kw);
        retrievers.add(tf);
        retrievers.add(new HybridRetriever(kw, tf));
        for (Retriever r : retrievers) methodBox.addItem(r.name());
        methodBox.setSelectedIndex(2);
        buildUi();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1250, 760);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JButton load = new JButton("Load report...");
        JButton analyze = new JButton("Analyze");
        JButton export = new JButton("Export CSV...");
        load.addActionListener(e -> loadReport());
        analyze.addActionListener(e -> analyze());
        export.addActionListener(e -> export());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Method:")); top.add(methodBox);
        top.add(new JLabel("Top-N per sentence:")); top.add(topN);
        top.add(new JLabel("Min score:")); top.add(minScore);
        top.add(load); top.add(analyze); top.add(export);

        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setMargin(new Insets(6, 6, 6, 6));
        table.setRowHeight(24);
        int[] widths = {30, 420, 80, 190, 120, 60, 170};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int r = table.getSelectedRow();
            if (r < 0 || r >= findings.size()) return;
            Pipeline.Finding f = findings.get(r);
            Technique t = f.scored.t;
            detail.setText("Evidence passage:\n" + f.sentence + "\n\n" + t.id + " - " + t.name + "  (" + t.tactic + ")\n"
                + t.description + "\n\nKeyword evidence: "
                + (f.scored.evidence.isEmpty() ? "(none - semantic match only, verify manually)" : f.scored.evidence)
                + "\nStatus: " + f.status);
            detail.setCaretPosition(0);
        });
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setMargin(new Insets(6, 6, 6, 6));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(input), new JScrollPane(table));
        split.setResizeWeight(0.3);
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder("Selected mapping (analyst verification)"));
        detailPanel.add(new JScrollPane(detail));

        JPanel extractor = new JPanel(new BorderLayout());
        extractor.add(top, BorderLayout.NORTH);
        extractor.add(split, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(detailPanel, BorderLayout.CENTER);
        south.add(statusBar, BorderLayout.SOUTH);
        extractor.add(south, BorderLayout.SOUTH);

        JButton run = new JButton("Run evaluation on Dataset/annotations.csv");
        run.addActionListener(e -> runEvaluation());
        evalOut.setEditable(false);
        evalOut.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        evalOut.setMargin(new Insets(8, 8, 8, 8));
        evalOut.setText("Click the button to compare keyword matching, TF-IDF and the hybrid method.");
        JPanel eval = new JPanel(new BorderLayout());
        JPanel evalTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        evalTop.add(run);
        eval.add(evalTop, BorderLayout.NORTH);
        eval.add(new JScrollPane(evalOut), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Extractor", extractor);
        tabs.addTab("Evaluation", eval);
        add(tabs);
        statusBar.setText("  " + techniques.size() + " techniques loaded from " + datasetDir.toAbsolutePath().normalize());
    }

    private void analyze() {
        String text = input.getText().trim();
        if (text.isEmpty()) { JOptionPane.showMessageDialog(this, "Paste or load a report first."); return; }
        Retriever r = retrievers.get(methodBox.getSelectedIndex());
        findings.clear();
        model.setRowCount(0);
        findings.addAll(Pipeline.analyze(text, r, (Integer) topN.getValue(), ((Number) minScore.getValue()).doubleValue()));
        int i = 1;
        for (Pipeline.Finding f : findings)
            model.addRow(new Object[]{i++, f.sentence, f.scored.t.id, f.scored.t.name, f.scored.t.tactic,
                String.format("%.3f", f.scored.score), f.status});
        statusBar.setText("  " + findings.size() + " mapping(s) from " + Pipeline.sentences(text).size()
            + " sentence(s) using " + r.name());
        detail.setText(findings.isEmpty() ? "No technique above the minimum score. Try lowering 'Min score'." : "Select a row to see its evidence.");
    }

    private void loadReport() {
        JFileChooser fc = new JFileChooser(datasetDir.resolve("sample_reports").toFile());
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            input.setText(Files.readString(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8));
            input.setCaretPosition(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage());
        }
    }

    private void export() {
        if (findings.isEmpty()) { JOptionPane.showMessageDialog(this, "Nothing to export. Run Analyze first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("attack_mappings.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            w.println("passage,technique_id,technique,tactic,score,status,keyword_evidence");
            for (Pipeline.Finding f : findings)
                w.println(q(f.sentence) + "," + f.scored.t.id + "," + q(f.scored.t.name) + "," + q(f.scored.t.tactic) + ","
                    + String.format("%.3f", f.scored.score) + "," + q(f.status) + "," + q(f.scored.evidence));
            statusBar.setText("  Exported to " + fc.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private static String q(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private void runEvaluation() {
        try {
            List<Evaluator.Example> data = Evaluator.load(datasetDir.resolve("annotations.csv"));
            evalOut.setText(Evaluator.run(retrievers, data));
            evalOut.setCaretPosition(0);
        } catch (Exception ex) {
            evalOut.setText("Evaluation failed: " + ex.getMessage());
        }
    }

    /** Finds the Dataset folder whether VS Code was opened on Implementation/ or on the project root. */
    static Path findDataset() {
        String prop = System.getProperty("dataset.dir");
        List<String> candidates = new ArrayList<>();
        if (prop != null) candidates.add(prop);
        candidates.add("../Dataset"); candidates.add("Dataset"); candidates.add("../../Dataset");
        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.exists(p.resolve("techniques.csv"))) return p;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Path ds = findDataset();
        if (ds == null) {
            String msg = "Could not find the Dataset folder (techniques.csv).\nOpen the project so that Dataset/ is next to Implementation/.";
            if (args.length > 0) System.err.println(msg);
            else JOptionPane.showMessageDialog(null, msg, "Dataset not found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Technique> ts = Technique.load(ds.resolve("techniques.csv"));
        if (args.length > 0 && args[0].equals("--eval")) {      // console mode, handy for the report
            Retriever kw = new KeywordMatcher(ts), tf = new TfidfRetriever(ts);
            List<Retriever> rs = List.of(kw, tf, new HybridRetriever(kw, tf));
            System.out.println(Evaluator.run(rs, Evaluator.load(ds.resolve("annotations.csv"))));
            return;
        }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new Main(ds, ts).setVisible(true));
    }
}
