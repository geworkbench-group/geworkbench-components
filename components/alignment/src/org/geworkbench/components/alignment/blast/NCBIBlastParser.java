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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
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

	/**
	 * Creates a new BlastParser with querySeq and filename set to specified
	 * ProtSeq and String value. Also creates a new hits Vector.
	 * 
	 * @param the
	 *            ProtSeq to set querySeq to.
	 * @param the
	 *            String to set filename to.
	 */
	public NCBIBlastParser(final int totalSequenceNum, final String filename) {
		this.totalSequenceNum = totalSequenceNum;
		this.filename = filename;
	}

	final private static String NEWLINESIGN = "<BR>";
	final private static int HIT_NUMBER_LIMIT = 250;

	private int totalHitCount = 0;
	private boolean hitOverLimit = false;

	/**
	 * Reads in Blast results from file and parses data into BlastObj objects.
	 */
	public ArrayList<Vector<BlastObj>> parseResults() {
		/**
		 * The new BlastDataSet Array
		 */
		ArrayList<Vector<BlastObj>> blastDataset = new ArrayList<Vector<BlastObj>>(
				10);

		totalHitCount = 0;
		StringTokenizer st;
		BlastObj each;
		int index = 0;

		File file = new File(filename);
		// server failure
		if (file.length() < 600) {
			log.warn("No hit found. try again.");
			return null;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			int count = 0;
			do {
				// A Vector of Strings representing each Blast hit by Accession
				// number.
				Vector<BlastObj> hits = new Vector<BlastObj>();
				boolean hitsFound = false;
				// loop to proceed to beginning of hit list from Blast output
				// file
				while (line != null) {

					if (line
							.startsWith("<caption>Sequences producing significant alignments:</caption>")) {
						hitsFound = true;
						break;
					}
					if (line.contains("No significant similarity found.")) {
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

				/* parsing section of the blast Hit info text */
				line = br.readLine();
				while(!line.equals("</thead>")) { // skip table header
					line = br.readLine();
				}
				line = br.readLine();
				int hitCount = 0;
				while (line != null && !line.trim().startsWith("</tbody>")) {
					hitCount++;
					totalHitCount++;
					if (hitCount > HIT_NUMBER_LIMIT) {
						hitOverLimit = true;
						line = br.readLine();
						continue; // skip further parsing the summary section
					}
					
					Pattern p = Pattern.compile(".+?href=\"(http:.+?)\".+"); // first URL
					Matcher m = p.matcher(line);
					String firstUrl = null;
					if (m.matches()) {
						firstUrl = m.group(1);
					}

					String name = null;
					String description = null;
					String score = null;
					String evalue = null;

					line = readTR(br);
					String[] tagSeparated = getTD(line);
					final int NUMBER_FIELDS = 7; // the number of fields could be 8 if there is an optional last column, the 'Links' icons
					//String[] tagSeparated = line.split("\\<(/?[^\\>]+)\\>"); // separated by HTML tag
					if(tagSeparated.length>=NUMBER_FIELDS) { // for most databases
						name = tagSeparated[0];
						description = tagSeparated[1].trim();
						score = tagSeparated[3].trim(); // FIXME total core, or max score tagSeparated[2], where is this used? 
						evalue = tagSeparated[5].trim();
						String[] tokens=evalue.split("\\s");
						evalue=tokens[0];
					} else if(tagSeparated.length==3) { // for database alu (without HTML links)
						// FIXME this case is not fixed yet
						String[] fields = tagSeparated[0].split("\\|");
						//id = fields[0];
						int firstSpace = fields[2].indexOf(" ");
						name = fields[1]+":"+fields[2].substring(0, firstSpace);
						description = fields[2].trim().substring(firstSpace);
						score = tagSeparated[1].trim();
						evalue = tagSeparated[2].trim();
					} else if(tagSeparated.length==0) { // after reading all <tr>....</tr>
						continue;
					} else {
						log.error("unexcepted HTML tag count " + tagSeparated.length);
						line = br.readLine();
						continue;
					}

					each = new BlastObj(true, null, name, description, score,
							evalue); // create new BlastObj for hit

					try {
						each.setInfoURL(new URL(firstUrl)); // FIXME this field may not be used for anything
						String s = firstUrl.replaceAll("GenPept", "fasta");
						s = s.replaceAll("GenBank", "fasta");
						each.setSeqURL(new URL(s));
					} catch (MalformedURLException e) {
						// ignore if URL is valid, e.g. null or the reason
					}

					hits.add(each);

					//line = br.readLine();
				} // end of processing summary.

				index = 0;

				boolean endofResult = false;
				final String ALU_DETAIL_LEADING = "<pre><script src=\"blastResult.js\"></script>>";
				while (line != null) {
					line = line.trim();
					if (line.startsWith("Database") || line.startsWith(">")
							|| line.startsWith(ALU_DETAIL_LEADING)) {
						break;
					}
					line = br.readLine();
				}

				if (line!=null && line.startsWith(ALU_DETAIL_LEADING)) {
					line = line.substring(ALU_DETAIL_LEADING.length()-1);
				}

				/* parsing detailed alignments Each has <PRE></PRE> */
				while (line != null && (line.trim().startsWith(">")
						|| line.trim().startsWith("Database")) ) {

					if (line.trim().startsWith("Database")) {
						endofResult = true;
						break;
					}

					String dbId = getDbId(line);
					StringBuffer detaillines = new StringBuffer("<PRE>").append(line);
					line = br.readLine();

					if(line!=null) line = line.trim();
					
					boolean additionalDetail = false;
					if (line!=null && line.trim().startsWith("Score")) {
						index--;
						additionalDetail = true;

					}
					// get BlastObj hit for which alignment is for
					each = hits.get(index);
					each.setDatabaseID( dbId );
					// skip the beginning description
					boolean getStartPoint = true;
					StringBuffer subject = new StringBuffer();
					int endPoint = 0;
					while (line!=null && !(line.trim().startsWith(">"))) {

						if (line.startsWith("</form>")) {
							// end of the useful information for one blast.
							endofResult = true;
							break;
						}

						if (line.startsWith("Length=")) {
							String[] lengthVal = line.split("=");
							each.setLength(new Integer(lengthVal[1].trim())
									.intValue());
						}
						final Pattern p = Pattern
								.compile("Identities\\s*=\\s*\\d+/(\\d+)\\s*.\\((\\d+)%\\).+");
						Matcher m = p.matcher(line);
						if (m.matches()) {
							if (0 == each.getAlignmentLength()) {
								String alignmentLengthString = m.group(1);
								int alignmentLength = Integer
										.parseInt(alignmentLengthString);
								each.setAlignmentLength(alignmentLength);
							}
							if (0 == each.getPercentAligned()) {
								String percentString = m.group(2);
								each.setPercentAligned(new Integer(
										percentString).intValue());
							}

						}
						// get the start point, end point and length

						if (line.trim().startsWith("Sbjct")) {
							st = new StringTokenizer(line);
							st.nextToken();
							if (getStartPoint) {
								if(0==each.getStartPoint())
									each.setStartPoint(Integer.valueOf(
										st.nextToken()).intValue());
								getStartPoint = false;
							} else {
								st.nextToken();
							}
							// concat the aligned parts and get rid of "-"
							subject = subject.append(st.nextToken().replaceAll(
									"-", ""));

							endPoint = Integer.valueOf(st.nextToken())
									.intValue();
						}

						String s = br.readLine();
						if (s != null) {
							line = s.trim();
						}
						if (s != null && !line.startsWith(">")) {
							detaillines.append(s).append(NEWLINESIGN);
						}
					}
					each.setEndPoint(endPoint);
//					each.setAlignmentLength(Math.abs(each.getStartPoint()
//							- each.getEndPoint()) + 1);
					each.setSubject(subject.toString());

					detaillines.append("</PRE>");

					if (additionalDetail) {
						String previousDetail = each.getDetailedAlignment();
						detaillines =  detaillines.insert(0, previousDetail);
					}
					each.setDetailedAlignment(detaillines.toString());

					index++;
					if (endofResult || index>=hits.size()) {
						endofResult = false;
						break;
					}

				}
				line = br.readLine();

				blastDataset.add(hits);
				count++;
			} while (count < totalSequenceNum);
			br.close();
			return blastDataset;
		} catch (FileNotFoundException e) {
			log.error("file "+filename+"not found.");
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	static private String getDbId(String line) {
		// (1) first URL (2) anchor text
		Pattern p = Pattern
				.compile(".+?\\<a\\s.+?href=\"(http:.+?)\"\\s\\>(.+?)\\</a\\>.+?");
		Matcher m = p.matcher(line);
		if (m.matches()) {
			String id = m.group(2);
			return id.split("\\|")[0];
		} else {
			log.error("no anchor matched: " + line);
			return null;
		}
	}

	static private String[] getTD(String line) {
		if (line == null)
			return new String[0];
		int index1 = line.toLowerCase().indexOf("<td");
		int index2 = line.toLowerCase().lastIndexOf("</td>");
		if (index1 < 0) {
			return new String[0];
		} else if (index2 >= 0) {
			line = line.substring(index1, index2);
		} else {
			line = line.substring(index1);
		}
		// separated by </td><td...>
		String[] td = line.split("\\</[tT][dD]>\\s*<[tT][dD][^\\>]*>"); 
		for (int i = 0; i < td.length; i++) {
			td[i] = td[i].replaceAll("\\<(/?[^\\>]+)\\>", ""); // remove all HTML tag
		}
		return td;
	}

	// assume the <tr> tag has been read, meaning it is already after <tr>
	static private String readTR(BufferedReader br) {
		final String END_TAG = "</form><!-- this is the end tag for the <form in blastcgi templates -->";

		try {
			String line = br.readLine();
			StringBuffer sb = new StringBuffer();
			while (line != null && !line.toLowerCase().contains("</tr>") && !line.startsWith(END_TAG) ) {
				sb.append(line);
				line = br.readLine();
			}
			if (line != null && line.toLowerCase().contains("</tr>")) {
				sb.append(line
						.substring(0, line.toLowerCase().indexOf("</tr>")));
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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
	
	public int getHitCount(){
		return totalHitCount;
	}

}
