package org.geworkbench.components.alignment.synteny;

/**
 * <p>Title: Bioworks</p>
 * <p>Description: Modular Application Framework for Gene Expession, Sequence and Genotype Analysis</p>
 * <p>Copyright: Copyright (c) 2003 -2004</p>
 * <p>Company: Columbia University</p>
 * @author not attributable
 * @version 1.0
 */

import java.io.*;
import java.net.*;

public class DAS_Retriver {
    /**
     *
     */
    public void DAS_Retriver() {

    }

    /**
     *
     * @param chr String
     * @param from int
     * @param to int
     * @param fil String
     */

    public static void getHumanAnnotation(String chr, int from, int to,
                                          String fil) {
        /* Forming request */
        String request =
            "http://genome.cse.ucsc.edu/cgi-bin/das/hg16/features?segment=";
        request = request.concat(chr);
        request = request.concat(":");
        request = request.concat(Integer.toString(from));
        request = request.concat(",");
        request = request.concat(Integer.toString(to));
        request = request.concat(";type=ECgene");

        GetItToFile(request, fil);
    }

    public static void getGPAnnotation(String genome, String chr, int from,
                                       int to, String feat,
                                       String fil) {
        /* Forming request */
        String request = new String("http://genome.cse.ucsc.edu/cgi-bin/das/");
        request = request.concat(genome);
        request = request.concat("/features?segment=");
        request = request.concat(chr);
        request = request.concat(":");
        request = request.concat(Integer.toString(from));
        request = request.concat(",");
        request = request.concat(Integer.toString(to));
        request = request.concat(";type=");
        request = request.concat(feat);

        GetItToFile(request, fil);
    }

    public static void getHumanSequence(String chr, int from, int to,
                                        String fil) {

        /* Forming request */
        String request =
            "http://genome.cse.ucsc.edu/cgi-bin/das/hg16/dna?segment=";
//    String request = "http://genome.ucsc.edu/cgi-bin/hgc?hgsid=30936970&g=htcGetDna2&table=&i=mixed&getDnaPos=";
        request = request.concat(chr);
        request = request.concat(":");
        request = request.concat(Integer.toString(from));
        request = request.concat(",");
        request = request.concat(Integer.toString(to));
//    request = request.concat("&db=hg16&hgSeq.cdsExon=1&hgSeq.padding5=0&hgSeq.padding3=0&hgSeq.casing=upper&boolshad.hgSeq.maskRepeats=1&hgSeq.repMasking=lower&boolshad.hgSeq.revComp=1&submit=Get+DNA");

        System.out.println(request);
        GetItToFasta(request, fil);

    }

    public static void getGpSequence(String genome, String chr, int from,
                                     int to, String fil) {

        /* Forming request */
        String request = "http://genome.cse.ucsc.edu/cgi-bin/das/";
        request = request.concat(genome);
        request = request.concat("/dna?segment=");
//    String request = "http://genome.ucsc.edu/cgi-bin/hgc?hgsid=30936970&g=htcGetDna2&table=&i=mixed&getDnaPos=";
        request = request.concat(chr);
        request = request.concat(":");
        request = request.concat(Integer.toString(from));
        request = request.concat(",");
        request = request.concat(Integer.toString(to));
//    request = request.concat("&db=hg16&hgSeq.cdsExon=1&hgSeq.padding5=0&hgSeq.padding3=0&hgSeq.casing=upper&boolshad.hgSeq.maskRepeats=1&hgSeq.repMasking=lower&boolshad.hgSeq.revComp=1&submit=Get+DNA");

        System.out.println(request);
        GetItToFasta(request, fil);

    }


    public static String GetIt(String UrlToGet) {

        int info;
        StringBuffer oneL = new StringBuffer();
        URL infoLink = null;
        InputStream serverIO = null;
        String buf = null;

        try {
            infoLink = new URL(UrlToGet);

            // start the connection with the httpd and talk
            serverIO = infoLink.openStream();

            // Read from server
            while ((info = serverIO.read()) != -1) {
                oneL.append((char) info); /* make note of the info */
            }
            serverIO.close();
            buf = new String(oneL);
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
        return buf;
    }

    public static String GetItSilent(String UrlToGet) {

        int info;
        StringBuffer oneL = new StringBuffer();
        URL infoLink = null;
        InputStream serverIO = null;
        String buf = null;

        try {
            infoLink = new URL(UrlToGet);

            // start the connection with the httpd and talk
            serverIO = infoLink.openStream();

            // Read from server
            while ((info = serverIO.read()) != -1) {
                oneL.append((char) info); /* make note of the info */
            }
            serverIO.close();
            buf = new String(oneL);
        } catch (IOException e) {
            return null;
        }
        return buf;
    }

//    public static String GetIt(String UrlToGet) {
//        int i, j, info;
//        StringBuffer oneL = new StringBuffer();
//        URL infoLink = null;
//        InputStream serverIO = null;
//        String buf = null;
//
//        try {
//            infoLink = new URL(UrlToGet);
//        }
//        catch (MalformedURLException ee) {
//            System.out.println("Malformed URL " + UrlToGet + " : " + ee);
//            return null;
//        }
//
//        // start the connection with the httpd and talk
//        try {
//            serverIO = infoLink.openStream();
//        }
//        catch (IOException e) {
//            System.out.println("Can't open stream " + UrlToGet + " : " + e);
//            return null;
//        }
//
//        /* do the deed - one character at a time. you can use read(byte b[], int off, int len) if you know
//               how many characters you expect from the server. */
//        try {
//            /* Read */
//            while ( (info = serverIO.read()) != -1) {
//                oneL.append( (char) info); /* make note of the info */
//            }
//            serverIO.close();
//            buf = new String(oneL);
//        }
//        catch (IOException e) {
//            System.out.println(e);
//            return null;
//        }
//        return buf;
//    }

    /**
     *
     * @param UrlToGet String
     * @param FileToSaveIn String
     * @return boolean
     */

    public static boolean GetItToFasta(String UrlToGet, String FileToSaveIn) {
        int i, j;
        FileOutputStream f = null;

        /* do the deed - one character at a time. you can use read(byte b[], int off, int len) if you know
               how many characters you expect from the server. */
        try {
            /* Open file */
            f = new FileOutputStream(FileToSaveIn);
            String buf = GetIt(UrlToGet);
            if (buf != null) {
                f.write('>');

                /* extracting and writting the title */
                i = buf.indexOf("<SEQUENCE id=");
                buf = buf.substring(i + 14);
                i = buf.indexOf('>');
                f.write( (buf.substring(0, i)).getBytes());
                f.write('\n');

                buf = buf.substring(i + 1);
                boolean flag = true;

                /*  Now writing averithing without tags */
                for (i = 0, j = 0; i < buf.length(); i++) {
                    if (buf.charAt(i) == '<') {
                        flag = false;
                        continue;
                    }
                    if (buf.charAt(i) == '>') {
                        flag = true;
                        continue;
                    }
                    if (flag && buf.charAt(i) > 32) {
                        f.write(buf.charAt(i));
                        if (j++ == 60) {
                            j = 0;
                            f.write('\n');
                        }
                        continue;
                    }
                }
            }
            f.close();
        }
        catch (IOException e) {
            System.out.println(e);
            return true;
        }
        return true;
    }

    /**
     *
     * @param UrlToGet String
     * @param FileToSaveIn String
     * @return boolean
     */

    public static boolean GetItToFile(String UrlToGet, String FileToSaveIn) {
        int i, j;
        FileOutputStream f = null;

        /* do the deed - one character at a time. you can use read(byte b[], int off, int len) if you know
               how many characters you expect from the server. */
        try {
            /* Open file */
            f = new FileOutputStream(FileToSaveIn);
            String buf = GetIt(UrlToGet);

            /* writting */
            f.write( (buf.toString()).getBytes());

            f.close();
        }
        catch (IOException e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    /**
     * GetItToString
     *
     * @param tURL String
     * @return Object
     */

}
