package org.geworkbench.components.medusa.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.components.medusa.MedusaUtil;
import org.ginkgo.labs.gui.SwingUtil;
import org.ginkgo.labs.psam.PsamUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.larvalabs.chart.PSAMPlot;

import edu.columbia.ccls.medusa.io.SerializedRule;

/**
 * 
 * @author keshav
 * @version $Id: DiscreteHitOrMissHeatMapPanel.java,v 1.1 2007/05/23 17:31:22
 *          keshav Exp $
 */
public class DiscreteHitOrMissHeatMapPanel extends JPanel implements
		MouseListener, MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Log log = LogFactory.getLog(this.getClass());

	private String sequencePath = null;

	private String rulePath = null;

	private List<String> ruleFiles = null;

	private List<String> targetNames = null;

	private ArrayList<SerializedRule> srules = null;

	private boolean[][] hitOrMissMatrix = null;

	private JTabbedPane parentPanel = null;

	public ArrayList rectRule = new ArrayList();
	private static final int COLUMN_WIDTH = 80;
	JPanel pssmPanel;

	/**
	 * 
	 * @param rulePath
	 * @param ruleFiles
	 * @param targetNames
	 */
	public DiscreteHitOrMissHeatMapPanel(String rulePath,
			List<String> ruleFiles, List<String> targetNames,
			String sequencePath) {

		log.debug("Constructing " + this.getClass().getSimpleName());

		this.rulePath = rulePath;

		this.ruleFiles = ruleFiles;

		this.sequencePath = sequencePath;

		this.targetNames = targetNames;

		srules = MedusaUtil.getSerializedRules(ruleFiles, rulePath);

		hitOrMissMatrix = MedusaUtil.generateHitOrMissMatrix(targetNames,
				srules, sequencePath);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@SuppressWarnings("unchecked")
	public void paintComponent(Graphics g) {

		clear(g);
		rectRule.clear();

		Graphics2D g2d = (Graphics2D) g;

		int row = 120;
		int lcol = 25;
		for (SerializedRule srule : srules) {

			/*
			 * String sequence = MedusaUtil.generateConsensusSequence(srule
			 * .getPssm());
			 */
			drawColumnNames(srule, g2d, row, lcol);
			lcol = lcol + 15;
		}

		for (int i = 0; i < targetNames.size(); i++) {
			int col = 15;
			for (int j = 0; j < ruleFiles.size(); j++) {
				boolean isHit = hitOrMissMatrix[i][j];

				Rectangle2D.Double rect = new Rectangle2D.Double(col, row, 15,
						15);
				if (isHit)
					g2d.setColor(Color.blue);
				else
					g2d.setColor(Color.black);

				g2d.fill(rect);
				col = col + 15;
			}
			row = row + 15;
		}
		int x = 15, y = 120;
		for (SerializedRule srule : srules) {
			Rectangle2D.Double rect = new Rectangle2D.Double(x, y, 15,
					15 * targetNames.size());
			x += 15;
			rectRule.add(new RectangleRule(rect, srule));
		}
	}

	/**
	 * @param label
	 *            The String to draw.
	 * @param g2d
	 * @param row
	 *            The row from where to draw the text.
	 * @param lcol
	 *            The (incremental) column where the text should be drawn.
	 */
	@SuppressWarnings("unchecked")
	private void drawColumnNames(SerializedRule srule, Graphics2D g2d, int row,
			int lcol) {

		String sequence = MedusaUtil.generateConsensusSequence(srule.getPssm());

		// TODO move method into a "gui util" class
		AffineTransform fontAT = new AffineTransform();

		/* slant text backwards */
		// fontAT.shear(0.2, 0.0);
		/* counter-clockwise 90 degrees */
		fontAT.setToRotation(Math.PI * 3.0f / 2.0f);
		Font font = new Font("Helvetica", Font.ITALIC, 12);
		Font theDerivedFont = font.deriveFont(fontAT);

		FontRenderContext frc = g2d.getFontRenderContext();
		TextLayout tstring = new TextLayout(sequence, theDerivedFont, frc);

		tstring.draw(g2d, lcol, row);
		Rectangle2D stringBound = tstring.getOutline(fontAT).getBounds2D();

		Rectangle2D.Double rect = new Rectangle2D.Double(lcol
				- stringBound.getHeight(), row - stringBound.getWidth(),
				stringBound.getHeight(), stringBound.getWidth());
		rectRule.add(new RectangleRule(rect, srule));
	}

	/**
	 * 
	 * @param g
	 */
	protected void clear(Graphics g) {
		super.paintComponent(g);
	}

	public void setParentPanel(JTabbedPane parentPanel) {
		this.parentPanel = parentPanel;
	}

	public void mouseDragged(MouseEvent mouseEvent) {
		// Do Nothing
	}

	public void mouseMoved(MouseEvent e) {
		// Graphics g = getGraphics();
		// ToDO: implement this.. highlist the rect area where the mouse is
		boolean check = false;
		for (Iterator itr = rectRule.iterator(); itr.hasNext();) {
			// getPreferredSize();
			RectangleRule rectangleRule = (RectangleRule) itr.next();
			if (rectangleRule.rect.contains(e.getX(), e.getY())) {
				check = true;
				break;
				// uncomment following, if border of the rectangle needs to be
				// shown on mouse-hover
				/*
				 * this.repaint(); //g.setXORMode(Color.black);
				 * g.setColor(Color.YELLOW);
				 * g.drawRect((int)rectangleRule.rect.getX(),
				 * (int)rectangleRule.rect.getY(),
				 * (int)rectangleRule.rect.getWidth(),
				 * (int)rectangleRule.rect.getHeight());
				 */
			}
		}
		if (check) {
			if (this.getCursor().getType() != Cursor.HAND_CURSOR)
				this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			if (this.getCursor().getType() != Cursor.DEFAULT_CURSOR)
				this.setCursor(Cursor
						.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mouseClicked(MouseEvent e) {
		System.out
				.println("In HitOrMissHeatPanel: MOUSE CLICKED. MOUSE CLICKED. RUN RUN !!");
		// TODO: implement this... open new PSSM Tab
		for (Iterator itr = rectRule.iterator(); itr.hasNext();) {
			// getPreferredSize();
			RectangleRule rectangleRule = (RectangleRule) itr.next();
			if (rectangleRule.rect.contains(e.getX(), e.getY())) {
				parentPanel.remove(pssmPanel);
				System.out.println("Inside : "
						+ MedusaUtil
								.generateConsensusSequence(rectangleRule.rule
										.getPssm()));
				addTab(rectangleRule.rule);
				break;
			}
		}
	}

	private void addTab(SerializedRule rule) {
		pssmPanel = new JPanel();
		JScrollPane mainScrollPane = new JScrollPane();

		/*
		 * mainScrollPane
		 * .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		 * mainScrollPane
		 * .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		 */

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"center:default", // columns
				""));// rows added dynamically

		PSAMPlot psamPlot = new PSAMPlot(PsamUtil.convertScoresToWeights(rule
				.getPssm(), true));
		psamPlot.setMaintainProportions(false);
		psamPlot.setAxisDensityScale(4);
		psamPlot.setAxisLabelScale(3);
		BufferedImage image = new BufferedImage(
				MedusaVisualComponent.IMAGE_WIDTH,
				MedusaVisualComponent.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		psamPlot.layoutChart(MedusaVisualComponent.IMAGE_WIDTH,
				MedusaVisualComponent.IMAGE_HEIGHT, graphics
						.getFontRenderContext());
		psamPlot.paint(graphics);
		ImageIcon psamImage = new ImageIcon(image);

		// add the image as a label
		builder.append(new JLabel(psamImage));

		// add the table
		JTable pssmTable = PsamUtil.createPssmTable(rule.getPssm(),
				"Nucleotides");

		TableColumn column = null;
		for (int k = 0; k < 5; k++) {
			column = pssmTable.getColumnModel().getColumn(k);
			if (k > 0) {
				column.setPreferredWidth(COLUMN_WIDTH);
			}
		}
		pssmTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scrollPane = new JScrollPane(pssmTable);
		scrollPane
				.setPreferredSize(new Dimension(psamImage.getIconWidth(), 190));

		builder.append(scrollPane);

		JPanel transFacButtonPanel = new JPanel();
		List<JRadioButton> buttons = SwingUtil.createRadioButtonGroup("JASPAR",
				"Custom");
		for (JRadioButton b : buttons) {
			transFacButtonPanel.add(b);
		}

		JButton loadTransFacButton = SwingUtil
				.createButton("Load TF",
						"Load file containing new transcription factors to add to the TF listing.");
		transFacButtonPanel.add(loadTransFacButton);
		builder.append(transFacButtonPanel);

		// add search results table

		JPanel pssmButtonPanel = new JPanel();
		JButton exportButton = SwingUtil.createButton("Export",
				"Export search results to file in PSSM file format.");
		pssmButtonPanel.add(exportButton);

		JButton searchButton = SwingUtil.createButton("Search",
				"Executes a database search.");
		pssmButtonPanel.add(searchButton);

		builder.append(pssmButtonPanel);
		mainScrollPane.setPreferredSize(new Dimension((int) psamImage
				.getIconWidth() + 10, (int) parentPanel.getPreferredSize()
				.getHeight() - 30));
		builder.getPanel().setPreferredSize(
				new Dimension((int) mainScrollPane.getPreferredSize()
						.getWidth() + 10, (int) mainScrollPane
						.getPreferredSize().getHeight() + 10));

		mainScrollPane.setViewportView(builder.getPanel());
		JPanel mainPSSMPanel = new JPanel();
		mainPSSMPanel.add(mainScrollPane);

		pssmPanel.add(mainPSSMPanel);

		// end the main logic

		parentPanel.add("PSSM", pssmPanel);
		parentPanel.setSelectedComponent(pssmPanel);
	}

	public void mousePressed(MouseEvent mouseEvent) {
		// Do Nothing
	}

	public void mouseReleased(MouseEvent mouseEvent) {
		// Do Nothing
	}

	public void mouseEntered(MouseEvent mouseEvent) {
		// Do Nothing
	}

	public void mouseExited(MouseEvent mouseEvent) {
		// Do Nothing
	}

	public ArrayList getRectRule() {
		return rectRule;
	}

	public class RectangleRule {
		Rectangle2D rect;
		SerializedRule rule;

		public RectangleRule(Rectangle2D rect, SerializedRule rule) {
			this.rect = rect;
			this.rule = rule;
		}
	}
}
