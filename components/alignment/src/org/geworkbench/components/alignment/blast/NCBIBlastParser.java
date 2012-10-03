package org.geworkbench.components.alignment.blast;

/**
 *
 * @version $Id$
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.bison.datastructure.bioobjects.sequence.BlastObj;

/**
 * BlastParser is a class that reads in from a text file generated by
 * RemoteBlast of Blast results. It parses out the Blast hits into a Vector of
 * BlastObj objects.
 */
public class NCBIBlastParser {
	static Log log = LogFactory.getLog(NCBIBlastParser.class);

	private int totalSequenceNum = 0;
	private String filename;
	final private ArrayList<Vector<BlastObj>> blastResultSet;

	/**
	 * Creates a new BlastParser with querySeq and filename set to specified
	 * ProtSeq and String value. Also creates a new hits Vector.
	 * @param blastResultSet 
	 * 
	 * @param the
	 *            ProtSeq to set querySeq to.
	 * @param the
	 *            String to set filename to.
	 */
	public NCBIBlastParser(final int totalSequenceNum, final String filename, ArrayList<Vector<BlastObj>> blastResultSet) {
		this.totalSequenceNum = totalSequenceNum;
		this.filename = filename;
		this.blastResultSet = blastResultSet; // this already contains the hits; file is used to get the 'detail' part 
	}

	final private static String NEWLINESIGN = "<BR>";
	final private static int HIT_NUMBER_LIMIT = 250;

	private int totalHitCount = 0;
	private boolean hitOverLimit = false;

	/**
	 * Reads in Blast results from file and parses the additional detail into BlastObj objects.
	 */
	public void parseResults() {

		totalHitCount = 0;

		File file = new File(filename);
		// server failure
		if (file.length() < 600) {
			log.warn("No hit found. try again.");
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			int count = 0;
			do { // loop through each sequence (they are all saved in one file
				boolean hitsFound = false;
				// loop to proceed to beginning of hit list from Blast output
				// file
				while (line != null) {

					if (line
							.startsWith("<caption>Sequences producing significant alignments:</caption>")) {
						hitsFound = true;
						break;
					}
					if (line.contains("No significant similarity found.")) { // TODO this case is not tested
						hitsFound = false;
						break;
					}
					line = br.readLine();
				}
				if (!hitsFound) {
					count++;
					line = br.readLine();
					continue;
				}

				/* skip the summary part */
				line = br.readLine();
				while(line!=null && !line.equals("</thead>")) { // skip table header
					line = br.readLine();
				}
				while (line != null && !line.trim().startsWith("</tbody>")) {
					line = br.readLine();
				}

				Vector<BlastObj> hits = blastResultSet.get(count);
				totalHitCount += hits.size();
				int index = 0;

				boolean endofResult = false;
				while (line != null) {
					line = line.trim();
					if (line.startsWith("Database") || line.startsWith(">")) {
						break;
					}
					line = br.readLine();
				}

				/* parsing detailed alignments Each has <PRE></PRE> */
				while (line != null && (line.trim().startsWith(">")
						|| line.trim().startsWith("Database")) ) {

					if (line.trim().startsWith("Database")) {
						endofResult = true;
						break;
					}

					// get BlastObj hit for which alignment this is for
					BlastObj each = hits.get(index);
					Pattern urlPattern = Pattern.compile(".+?href=\"(http:.+?)\".+"); // first URL
					Matcher match = urlPattern.matcher(line);
					String firstUrl = null;
					if (match.matches()) {
						firstUrl = match.group(1);
					}
					if(firstUrl!=null) {
						String s = firstUrl.replaceAll("GenPept", "fasta");
						s = s.replaceAll("GenBank", "fasta");
						each.setSeqURL(new URL(s));
					}

					StringBuffer detaillines = new StringBuffer("<PRE>").append(line);
					line = br.readLine();

					if(line!=null) line = line.trim();
					
					while (line!=null && !(line.trim().startsWith(">"))) {

						if (line.startsWith("</form>")) {
							// end of the useful information for one blast.
							endofResult = true;
							break;
						}

						line = br.readLine();
						if (line != null && !line.trim().startsWith(">")) {
							detaillines.append(line.trim()).append(NEWLINESIGN);
						}
					}

					detaillines.append("</PRE>");

					each.setDetailedAlignment(detaillines.toString());

					index++;
					if (endofResult || index>=hits.size()) {
						endofResult = false;
						break;
					}

				}
				line = br.readLine();

				count++;
			} while (count < totalSequenceNum);
			br.close();
		} catch (FileNotFoundException e) {
			log.error("file "+filename+"not found.");
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

	}

	/**
	 * getSummary
	 * 
	 * @return String
	 */
	public String getSummary() {
		if (hitOverLimit) {
			return "Some sequences have more than 250 hits, only the first "
					+ HIT_NUMBER_LIMIT + " hits are displayed. Total hits: "
					+ totalHitCount + ".";
		}
		return "Total hits for all sequences: " + totalHitCount + ".";
	}
	
	// TODO CSAlignmentResultSet constructor should be simplified so the unnecessary method could be eliminated
	public int getHitCount(){
		return totalHitCount;
	}

}
