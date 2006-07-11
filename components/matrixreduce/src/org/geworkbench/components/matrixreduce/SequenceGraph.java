package org.geworkbench.components.matrixreduce;

import org.geworkbench.bison.datastructure.complex.pattern.matrix.DSPositionSpecificAffintyMatrix;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * @author John Watkinson
 */
public class SequenceGraph extends JPanel {

    public static final Color COLOR_A = new Color(0x00CC00);
    public static final Color COLOR_C = new Color(0x0000CC);
    public static final Color COLOR_G = new Color(0xFFB300);
    public static final Color COLOR_T = new Color(0xCC0000);

    public static final char[] NUCLEOTIDES = {'A', 'C', 'G', 'T'};

    public static final Color[] NUCLEOTIDE_COLORS =
            {
                    COLOR_A,
                    COLOR_C,
                    COLOR_G,
                    COLOR_T,
            };

    private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final int MAX_FONT_SIZE = 8;
    private static final int HEIGHT = 39;
    private static final int ACTIVE_HEIGHT = 37;
    private static final int LABEL_WIDTH = 80;

    private int n;
    private int maxLength;
    private String label;
    private int[] sequence;
    private float[] posScores;
    private float[] negScores;
    private float bestPosScore = 0;
    private float bestNegScore = 0;
    private int componentWidth = 1;
    private int toolTipLength;
    private MatrixReduceViewer viewer;

    public SequenceGraph(String seq, String label, int maxLength, MatrixReduceViewer viewer) {
        super(true);
        this.label = label;
        this.viewer = viewer;
        // Convert sequence in to indices
        n = seq.length();
        sequence = new int[n];
        for (int i = 0; i < n; i++) {
            switch (seq.charAt(i)) {
                case 'A':
                    sequence[i] = 0;
                    break;
                case 'C':
                    sequence[i] = 1;
                    break;
                case 'G':
                    sequence[i] = 2;
                    break;
                default:
                    sequence[i] = 3;
            }
        }
        this.maxLength = maxLength;
        n = sequence.length;
        posScores = new float[n];
        negScores = new float[n];
    }

    public static Font getSizedFont(FontRenderContext context, Font template, double width, double maxHeight) {
        Rectangle2D bounds = template.getMaxCharBounds(context);
        double ratio = width / bounds.getWidth();
        float originalSize = template.getSize2D();
        float newSize = (float) (originalSize * ratio);
        // return template.deriveFont(Font.PLAIN, (int)newSize);
        Font result = template.deriveFont(template.getStyle(), newSize);
        if (result.getMaxCharBounds(context).getHeight() > maxHeight) {
            ratio = maxHeight / bounds.getHeight();
            newSize = (float) (originalSize * ratio);
            return template.deriveFont(template.getStyle(), newSize);
        } else {
            return result;
        }
    }

    public void clearScores(boolean forward) {
        if (forward) {
            posScores = new float[n];
        } else {
            negScores = new float[n];
        }
    }

    public void createScores(DSPositionSpecificAffintyMatrix psam, boolean forward) {
        // Reverse PSAM scores to run sequence in reverse
        double[][] scores = psam.getScores();
        int m = scores.length;
        toolTipLength = m;
        float[] results;
        if (forward) {
            results = posScores;
        } else {
            results = negScores;
            scores = new double[m][4];
            for (int i = 0; i < m; i++) {
                scores[i] = psam.getScores()[m - i - 1];
            }
        }
        float bestScore = 0;
        for (int i = 0; i < (n - m); i++) {
            double v = 1.0;
            for (int j = 0; j < m; j++) {
                v *= scores[j][sequence[i + j]];
            }
            if (forward) {
                results[i] = (float) v;
            } else {
                results[i + m] = (float) v;
            }
            if (results[i] > bestScore) {
                bestScore = results[i];
            }
        }
        if (forward) {
            bestPosScore = bestScore;
        } else {
            bestNegScore = bestScore;
        }
    }

    public float getBestPosScore() {
        return bestPosScore;
    }

    public float getBestNegScore() {
        return bestNegScore;
    }

    public Dimension getMinimumSize() {
        Dimension min = super.getMinimumSize();
        min.height = HEIGHT;
        return min;
    }

    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        pref.height = HEIGHT;
        return pref;
    }

    public Dimension getMaximumSize() {
        Dimension max = super.getMaximumSize();
        max.height = HEIGHT;
        return max;
    }

    public String getToolTipText(MouseEvent event) {
        float x = event.getX() - LABEL_WIDTH;
        float charWidth = ((float) componentWidth) / maxLength;
        int index = (int) (x / charWidth);
        if ((index >= 0) && (index < n)) {
            int k = Math.min(index + toolTipLength, n);
            char[] chars = new char[k - index];
            for (int i = index; i < k; i++) {
                chars[i - index] = NUCLEOTIDES[sequence[i]];
            }
            return "" + index + ": " + new String(chars);
        }
        return null;
    }

    public void paint(Graphics g1d) {
        Graphics2D g = (Graphics2D) g1d;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth() - LABEL_WIDTH - 4;
        componentWidth = width;
        float charWidth = ((float) width) / maxLength;
        FontRenderContext fontRenderContext = g.getFontRenderContext();
//        Font font = getSizedFont(fontRenderContext, DEFAULT_FONT, charWidth, MAX_FONT_SIZE);
//        font = DEFAULT_FONT.deriveFont(5f);
//        g.setFont(font);
//        float fontHeight = (float) font.getMaxCharBounds(fontRenderContext).getHeight() + 1;
        int centerLine = HEIGHT / 2;
        // todo - size font appropriately
        Font font = DEFAULT_FONT;
        LineMetrics line = font.getLineMetrics(label, fontRenderContext);
        float textWidth = (float) font.getStringBounds(label, fontRenderContext).getWidth();
        float halfHeight = -line.getStrikethroughOffset();
        float baseLine = halfHeight + centerLine;
        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString(label, LABEL_WIDTH - 4 - textWidth, baseLine);
        float position = LABEL_WIDTH;
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < n; i++) {
            g.setColor(NUCLEOTIDE_COLORS[sequence[i]]);
            g.draw(new Line2D.Float(position, centerLine, position + charWidth, centerLine));
            position += charWidth;
        }
        int graphHeight = (ACTIVE_HEIGHT - 5) / 2;
        // POSITIVE GRAPH
        {
            int startY = centerLine - 3;
            int endY = startY - graphHeight;
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), endY);
            g.setColor(Color.BLACK);
            if (viewer.isShowForward()) {
                float oldY = 0f;
                float oldX = 0f;
                float x = LABEL_WIDTH - charWidth / 2;
                for (int i = 0; i < n; i++) {
                    float y = startY + (endY - startY) * posScores[i];
                    oldX = x;
                    x += charWidth;
                    if (i > 0) {
                        g.draw(new Line2D.Float(oldX, oldY, x, y));
                    }
                    oldY = y;
                }
            }
        }
        // NEGATIVE GRAPH
        {
            int startY = centerLine + 3;
            int endY = startY + graphHeight;
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, endY, getWidth(), centerLine - 2 - graphHeight);
            g.setColor(Color.BLACK);
            if (viewer.isShowBackward()) {
                float oldY = 0f;
                float oldX = 0f;
                float x = LABEL_WIDTH - charWidth / 2;
                for (int i = 0; i < n; i++) {
                    float y = startY + (endY - startY) * negScores[i];
                    oldX = x;
                    x += charWidth;
                    if (i > 0) {
                        g.draw(new Line2D.Float(oldX, oldY, x, y));
                    }
                    oldY = y;
                }
            }
        }
//        g.setColor(Color.GRAY);
//        g.draw(new Line2D.Float(0, centerLine - halfHeight - 1, width, centerLine - halfHeight - 1));
//        g.draw(new Line2D.Float(0, centerLine + halfHeight + 1, width, centerLine + halfHeight + 1));
    }
}
