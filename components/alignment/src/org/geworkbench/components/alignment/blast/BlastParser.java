package org.geworkbench.components.alignment.blast;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.geworkbench.bison.datastructure.biocollections.sequences.
        CSSequenceSet;

/**
 * BlastParser is a class that reads in from a text file generated by
 * RemoteBlast of Blast results.  It parses out the Blast hits into a
 * Vector of BlastObj objects.
 */
public class BlastParser {
    /**
     * BlastParser
     *
     * @param aString String
     * @param anObject Object
     */
    public BlastParser(String aString, Object anObject) {
        if (anObject instanceof CSSequenceSet) {
            totalSequenceNum = ((CSSequenceSet) anObject).size();
            filename = aString;
            hits = new Vector();
        }
    }

    /**
     * The primary protein sequence, if used as query seqeunce.
     */
    private ProtSeq querySeq;
    static String NEWLINESIGN = "<BR>";
    static String LINEBREAK = "\n";
    static int MAXNUMBERHITS = 250;
    private int hitCount = 0;
    private int totalHitCount = 0;
    private int totalSequenceNum = 0;
    private boolean hit250More = false;

    /**
     * A Vector of Strings representing each Blast hit by Accession number.
     */
    protected Vector hits;

    /**
     * The file name to write results out to.
     */
    String filename;

    /**
     * The default file name to write results out to.
     */
    private final String DEFAULT_FILENAME = "BLAST_results.txt";

    /**
     * An array of type char.
     */
    private final char[] EMPTY = new char[60];

    /**
     *
     * The new BlastDataSet Array
     */
    private ArrayList blastDataset = new ArrayList(10);
    private int count = 0;

    /**
     * Creates a new BlastParser with querySeq set to specified ProtSeq.
     * Also creates a new hits Vector and sets filename to default value.
     *
     * @param the ProtSeq to set querySeq to.
     */
    public BlastParser(ProtSeq seq) {
        querySeq = seq;
        hits = new Vector();
        this.filename = DEFAULT_FILENAME;
    }

    /**
     * Creates a new BlastParser with querySeq and filename set to specified
     * ProtSeq and String value.  Also creates a new hits Vector.
     *
     * @param the ProtSeq to set querySeq to.
     * @param the String to set filename to.
     */
    public BlastParser(ProtSeq seq, String filename) {
        querySeq = seq;
        hits = new Vector();
        this.filename = filename;
    }

    /**
     * Creates a new BlastParser with filename set to default value.  Also
     * creates a new hits Vector.
     */
    public BlastParser() {
        this.filename = DEFAULT_FILENAME;
        hits = new Vector();
    }

    /**
     * BlastParser
     *
     * @param aString String
     */
    public BlastParser(String aString) {
        filename = aString;
        hits = new Vector();
    }

    /**
     * Returns the hits Vector.
     */
    public Vector getHits() {
        return hits;
    }

    public int getHitCount() {
        return hitCount;
    }

    public ArrayList getBlastDataset() {
        return blastDataset;
    }

    public int getTotalSequenceNum() {
        return totalSequenceNum;
    }

    /**
     * Reads in Blast results from file and parses data into BlastObj objects.
     */
    public boolean parseResults() {

        StringTokenizer st;
        BlastObj each;
        String[] fields;
        int index = 0, index2 = 0, start = 0;
        String query = "", subject = "", align = "";
        String errorline = "";

        try {
            File file = new File(filename);
            //server failure
            if (file.length() < 600) {
                System.out.println("No hit found. try again.");
                return false;
            } else {

                //new BufferedReader for parsing file line by line
                BufferedReader br = new BufferedReader(new FileReader(
                        filename));
                String line;
                String sub;
                line = br.readLine();
                do {
                    hits = new Vector();
                    String noResult = "No hits found";
                    boolean noHitsFound = false;
                    //loop to proceed to beginning of hit list from Blast output file
                    while (true) {
                        line = br.readLine();
                        // System.out.println("test2: " + line);

                        if (line == null ||
                            (line.startsWith(
                                    "Sequences producing significant alignments:"))) {
                            break;
                        }
                        if (line == null || (line.contains("No hits found"))) {
                            noHitsFound = true;
                            break;
                        }
                    }
                    if (!noHitsFound) {

                        /* parsing section of the blast Hit info text*/
                        br.readLine();
                        line = br.readLine();
                        hitCount = 0;
                        while (line != null && !line.trim().startsWith("</PRE>")) {
                            // System.out.println("test3: " + line);
                            String[] strA = line.split("</a>");

                            for (int i = 0; i < strA.length; i++) {
                                //System.out.println(strA[i]);
                            }
                            hitCount++;
                            totalHitCount++;
                            if (hitCount <= MAXNUMBERHITS) {

                                each = new BlastObj(); //create new BlastObj for hit
                                if (strA.length < 3) {
                                    each.setRetriveWholeSeq(false);
                                } else {
                                    each.setRetriveWholeSeq(true);
                                    StringTokenizer st1 = new StringTokenizer(
                                            strA[
                                            0], "\"");
                                    st1.nextToken();
                                    if (st1.hasMoreTokens()) {
                                        String s = st1.nextToken();
                                        each.setInfoURL(new URL(s));
                                        s = s.replaceAll("GenPept", "fasta");
                                        s = s.replaceAll("GenBank", "fasta");
                                        each.setSeqURL(new URL(s));

                                    }
                                    if (st1.hasMoreTokens()) {
                                        StringTokenizer st2 = new
                                                StringTokenizer(
                                                st1.nextToken(), "|");
                                        if (st2.hasMoreTokens()) {
                                            each.setDatabaseID(st2.nextToken().
                                                    substring(1));
                                        }
                                        if (st2.hasMoreTokens()) {
                                            each.setName(st2.nextToken());
                                        }
                                        if (st2.hasMoreTokens()) {
                                            each.setDescription(st2.nextToken());
                                        }
                                    }
                                    StringTokenizer st3 = new StringTokenizer(
                                            strA[
                                            1], "<");
                                    if (st3.hasMoreTokens()) {
                                        each.setDescription(st3.nextToken());
                                    }
                                    if (st3.hasMoreTokens()) {
                                        StringTokenizer st4 = new
                                                StringTokenizer(
                                                st3.nextToken(), ">");

                                        String str = "0";
                                        while (st4.hasMoreTokens()) {
                                            str = st4.nextToken();
                                        }
                                        each.setScore(new Float(str.trim()).
                                                intValue()
                                                );
                                    }
                                    each.setEvalue(strA[2]);

                                    hits.add(each);
                                }
                                //System.out.println(each.getDatabaseID() + each.getDescription()
                                //                   + each.getEvalue() + each.getScore());
                            } else {
                                hit250More = true;
                            }
                            //end of check hitCount;
                            line = br.readLine();

                        } //end of processing summary.
                        //System.out.println(line+ " " + hits.size());
                        index = 0;
                        line = br.readLine();
                        if (line == null) {
                            return false;
                        }
                        boolean endofResult = false;
                        /* parsing detailed alignments Each has <PRE></PRE> */
                        while (line.trim().startsWith("<PRE>")) {
                            //System.out.println("test5" + line);
                            if (line.trim().startsWith("<PRE>  Database")) {
                                endofResult = true;
                                break;
                            }

                            line = br.readLine().trim();
                            if (line.startsWith("Database:")) {
                                //end of the useful information for one blast.
                                endofResult = true;
                                break;
                            }
                            boolean additionalAlignedParts = false;
                            if (line.trim().startsWith("Score")) {
                                index--;
                                additionalAlignedParts = true;
                                //  System.out.println(additionalAlignedParts + line);
                                System.out.println(index + " = index " + line);
                            }
                            //System.out.println("index"  + index + " " + hits.size());
                            String detaillines = "<PRE>" + line;
                            //get BlastObj hit for which alignment is for
                            //System.out.println(index + " = index " + line);
                            each = (BlastObj) hits.get(index);

                            if (count > 60) {
                                //System.out.println(count);
                            }
                            System.out.println(each.getName());
                            //skip the beginning description
                            subject = "";
                            boolean getStartPoint = true;
                            subject = "";
                            int endPoint = 0;
                            while (!(line.trim().startsWith("</PRE>"))) {

                                if (line.startsWith("Database:")) {
                                    //end of the useful information for one blast.
                                    endofResult = true;
                                    break;
                                }
                                if (!additionalAlignedParts) {
                                    if (line.startsWith("Length")) {
                                        each.setLength(new Integer(line.
                                                substring(8).
                                                trim()).intValue());
                                    }
                                    if (line.startsWith("Identities = ")) {
                                        /**todo
                                         * use Matchs pattern later.
                                         */
                                        StringTokenizer st1 = new
                                                StringTokenizer(
                                                line,
                                                "(");
                                        st1.nextToken();
                                        String identity = st1.nextToken();
                                        String[] s = identity.split("%");
                                        each.setPercentAligned(new Integer(s[0]).
                                                intValue());

                                    }
                                    // get the start point, end point and length

                                    if (line.trim().startsWith("Sbjct")) {
                                        st = new StringTokenizer(line);
                                        st.nextToken();
                                        if (getStartPoint) {
                                            each.setStartPoint(Integer.valueOf(
                                                    st.
                                                    nextToken()).intValue());
                                            getStartPoint = false;
                                        } else {
                                            st.nextToken();
                                        }
                                        //concat the aligned parts and get rid of "-"
                                        subject = subject.concat(st.nextToken().
                                                replaceAll("-", ""));

                                        endPoint = Integer.valueOf(st.nextToken()).
                                                intValue();
                                    }

                                }
                                //System.out.println(each.getStartPoint() + "" + each.getEndPoint());
                                String s = br.readLine();
                                line = s.trim();

                                detaillines += s + NEWLINESIGN;
                            }

                            detaillines += "</PRE>";

                            //System.out.println(detaillines);
                            if (additionalAlignedParts) {
                                String previousDetail = each.
                                        getDetailedAlignment();
                                detaillines = previousDetail + detaillines;
                            } else {
                                each.setEndPoint(endPoint);
                                each.setAlignmentLength(Math.abs(each.getEndPoint() - each.
                                        getStartPoint()  + 1));
                                System.out.println("ENDPOINT" + endPoint + " " +
                                        each.getStartPoint() + " " +
                                        each.getAlignmentLength() +
                                        subject);
                                each.setSubject(subject);

                            }
                            each.setDetailedAlignment(detaillines);
                            if (endofResult) {
                                endofResult = false;
                                break;
                            }

                            br.readLine();
                            br.readLine();
                            line = br.readLine();
                            index++;
                        }
                        line = br.readLine();
                        Vector newHits = hits;
                        blastDataset.add(newHits);
                        count++;
//                        System.out.println("run ps" + count + blastDataset.size() +
//                                           " " + newHits.size());
                    } else {
                        blastDataset.add(null);
                        count++;
                        // System.out.println("Found 1 nohits" + count);
                    }
                } while (count < totalSequenceNum);
                //  System.out.println("run out of ps" + count + blastDataset.size());
                return true;
            }

        } catch (FileNotFoundException e) {
            System.out.println("file not found.");
            return false;
            //System.exit(1);
        } catch (IOException e) {
            System.out.println("IOException found in BlastParser.!");
            // e.printStackTrace();
            return false;
        } catch (Exception e) {

            System.out.println("Blastparser" + errorline);
            // e.printStackTrace();

            //find a blast bug, temp change to true.
            return false;

        }

    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public void setBlastDataset(ArrayList blastDataset) {
        this.blastDataset = blastDataset;
    }

    public void setTotalSequenceNum(int totalSequenceNum) {
        this.totalSequenceNum = totalSequenceNum;
    }

    /**
     * getSummary
     *
     * @return String
     */
    public String getSummary() {
        if (hit250More) {
            return "Some sequences have more than 250 hits, only the first 250 hits are displayed. Total hits are " +
                    totalHitCount +
                    ".";
        }
        return "Total hits for all sequences are " + totalHitCount + ".";
    }

    /*public static void main(String[] args) {
            BlastParser test = new BlastParser();
            test.parseResults();
             }*/

}
