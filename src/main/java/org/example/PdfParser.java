package org.example;

import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.*;
import com.itextpdf.kernel.pdf.canvas.parser.data.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class PdfParser {


    private static final double LINE_TOLERANCE = 5.0;


    public void parsePdf(String pathFile) throws Exception {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(pathFile));
        int slashIndex = pathFile.lastIndexOf('/') + 1;
        int dotIndex = pathFile.lastIndexOf('.');

        String result = pathFile.substring(slashIndex, dotIndex);
        PdfJson pdfJson = new PdfJson();

        try (BufferedWriter txtWriter = new BufferedWriter(new FileWriter(result + ".txt"))) {

            for (int pageNum = 1; pageNum <= pdfDoc.getNumberOfPages(); pageNum++) {
                txtWriter.write("=== Page " + pageNum + " ===\n");
                PdfJson.Page jsonPage = new PdfJson.Page(pageNum);

                Map<Double, List<TextChunk>> lineMap = new TreeMap<>(Collections.reverseOrder());

                IEventListener listener = new IEventListener() {
                    @Override
                    public void eventOccurred(IEventData data, EventType type) {
                        if (type == EventType.RENDER_TEXT) {
                            TextRenderInfo renderInfo = (TextRenderInfo) data;
                            String text = renderInfo.getText();
                            if (text == null || text.trim().isEmpty()) return;

                            Vector start = renderInfo.getBaseline().getStartPoint();
                            if (start == null) return;

                            double x = start.get(Vector.I1);
                            double y = Math.round(start.get(Vector.I2));

                            lineMap.computeIfAbsent(y, k -> new ArrayList<>())
                                    .add(new TextChunk(x, y, text));
                        }
                    }

                    @Override
                    public Set<EventType> getSupportedEvents() {
                        return Collections.singleton(EventType.RENDER_TEXT);
                    }
                };

                PdfCanvasProcessor processor = new PdfCanvasProcessor(listener);

                try {
                    processor.processPageContent(pdfDoc.getPage(pageNum));
                } catch (ClassCastException | NullPointerException e) {
                    System.err.println("Warning: PDF parsing issue: " + e.getMessage());
                }

                List<Line> finalLines = mergeLines(lineMap);

                for (Line ln : finalLines) {
                    String formattedLine = formatTwoColumnLine(ln);
                    txtWriter.write(formattedLine + "\n");
                    jsonPage.lines.add(formattedLine);
                }

                pdfJson.pages.add(jsonPage);
            }
        }

        pdfDoc.close();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter jsonWriter = new FileWriter(result + ".json")) {
            gson.toJson(pdfJson, jsonWriter);
        }

        System.out.println("Parsing complete! Saved to .txt and .json.");
    }

    private List<Line> mergeLines(Map<Double, List<TextChunk>> lineMap) {
        List<Line> finalLines = new ArrayList<>();
        List<Double> yValues = new ArrayList<>(lineMap.keySet());
        yValues.sort(Collections.reverseOrder());

        for (double y : yValues) {
            List<TextChunk> chunks = lineMap.get(y);
            boolean merged = false;
            for (Line lineObj : finalLines) {
                if (Math.abs(lineObj.y - y) <= LINE_TOLERANCE) {
                    lineObj.chunks.addAll(chunks);
                    lineObj.y = (lineObj.y + y) / 2.0;
                    merged = true;
                    break;
                }
            }
            if (!merged) finalLines.add(new Line(y, new ArrayList<>(chunks)));
        }

        finalLines.sort((a, b) -> Double.compare(b.y, a.y));
        finalLines.forEach(l -> l.chunks.sort(Comparator.comparingDouble(c -> c.x)));
        return finalLines;
    }

    private List<TextChunk> mergeChunksHorizontally(List<TextChunk> chunks) {
        if (chunks.isEmpty()) return chunks;
        List<TextChunk> merged = new ArrayList<>();
        TextChunk current = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            TextChunk next = chunks.get(i);
            if (next.x - (current.x + approxWidth(current.text)) < 2.0) {
                current = new TextChunk(current.x, current.y, current.text + next.text);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private double approxWidth(String s) {
        return s.length() * 5.0;
    }

    private String formatTwoColumnLine(Line line) {
        line.chunks.sort(Comparator.comparingDouble(c -> c.x));
        List<TextChunk> mergedChunks = mergeChunksHorizontally(line.chunks);

        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();

        double splitX = 200;

        for (TextChunk c : mergedChunks) {
            if (c.x < splitX) {
                if (left.length() > 0) left.append(" ");
                left.append(c.text);
            } else {
                if (right.length() > 0) right.append(" ");
                right.append(c.text);
            }
        }

        return String.format("%-30s %s", left.toString(), right.toString());
    }

}
