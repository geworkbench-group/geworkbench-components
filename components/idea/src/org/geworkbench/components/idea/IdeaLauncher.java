package org.geworkbench.components.idea;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.commons.math.MathException;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;

public class IdeaLauncher {

	/**
	 * Stand alone version.
	 * 
	 * @author zm2165
	 * @version $id$
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws MathException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			MathException {
		String dir = System.getProperty("user.dir");
		
		// initialize input file names with default setting 
		String networkFile = dir + "\\input\\network.txt"; // to be processed file
		// prepare the expression data
		String expressionFile = dir + "\\input\\bcell_mas5_254_filtered_classinfo.exp";
		String annotationFile = dir + "\\input\\HG_U95Av2.na23.annot.csv";
		// prepare the phenotype data
		String phenotypeFile = dir + "\\input\\myPhenotype.txt"; 


		if (args.length != 0 && args.length != 4) {
			System.out
					.println("Usage:\n"
							+ "java IdeaLauncher network_file expression_file annotation_file phenotype_file"
							+ "\nor: java IdeaLauncher");
			System.exit(0);
		} else if (args.length == 4)  {
			networkFile = args[0];
			expressionFile = args[1];
			annotationFile = args[2];
			phenotypeFile = args[3];
		}

		// ArrayList<Gene> geneList=new ArrayList<Gene>();
		final int HEADCOL = 2;
		double[][] expData = null;
		int expColLength = 0, expRowLength = 0;

		final String PHENO_INCLUDE = "Include";
		final String PHENO_EXCLUDE = "Exclude";

		final int EXP_ROW_START = 40; // the first row in exp file to count for
										// gene expression data

		TreeSet<Gene> preGeneList = new TreeSet<Gene>();
		ArrayList<Edge> edgeIndex = new ArrayList<Edge>();
		Map<String, String> probe_chromosomal = new HashMap<String, String>();

		FileReader prereader = new FileReader(networkFile);
		Scanner prein = new Scanner(prereader);
		while (prein.hasNextLine()) {
			String line = prein.nextLine();

			int headLine = line.indexOf("Gene1");
			if (headLine == -1) {// there is no key word
				// System.out.println(line);

				String[] tokens = line.split("\\s");
				String first = tokens[0];
				String second = tokens[1];
				try {
					int geneNo1 = Integer.parseInt(first);
					int geneNo2 = Integer.parseInt(second);
					Gene gene1 = new Gene(geneNo1);
					Gene gene2 = new Gene(geneNo2);

					preGeneList.add(gene1);
					preGeneList.add(gene2);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}// end of while

		File dataFile = new File(annotationFile);

		AnnotationParser.processAnnotationData("other", dataFile, preGeneList,
				probe_chromosomal);

		String expFirstCol = "";
		FileReader expreader = new FileReader(expressionFile);
		Scanner expin = new Scanner(expreader);

		while (expin.hasNextLine()) {
			String line = expin.nextLine();
			String[] tokens = line.split("\\s");
			expColLength = tokens.length;
			expFirstCol += tokens[0] + "\t";
			expRowLength++;
		}
		System.out.println(expFirstCol);

		Iterator<Gene> iter = preGeneList.iterator();
		while (iter.hasNext()) {
			Gene g = iter.next();

			String s = g.getProbeIds();
			String[] ids = s.split("\\s");
			if (ids.length > 0) {
				for (int i = 0; i < ids.length; i++) {
					String[] exps = expFirstCol.split("\\s");
					for (int j = EXP_ROW_START; j < exps.length; j++) {
						if (ids[i].equals(exps[j])) {
							String rows = g.getExpRows();
							rows += j + "\t";
							g.setExpRows(rows);
						}
					}
				}
			}
			// System.out.println(g.getGeneNo()+":"+g.getExpRows());
		}

		expData = new double[expRowLength][expColLength]; // expData saves the
															// whole exp file
															// except strings
															// which are set to
															// 0
		String[] expCol0 = new String[expRowLength]; // save the exp file
														// column0, because
														// those are probeId
														// names

		Map<String, String> probe_symbol = new HashMap<String, String>();

		expreader = new FileReader(expressionFile);
		expin = new Scanner(expreader);
		int row = 0;
		while (expin.hasNextLine()) {
			String line = expin.nextLine();
			String[] items = line.split("\\s");
			// expData[row]=new double[items.length];
			expCol0[row] = items[0];
			probe_symbol.put(items[0], items[1]);// the second column should be
													// gene symbol
			for (int i = 0; i < expColLength; i++) {
				try {
					expData[row][i] = Double.parseDouble(items[i]);
				} catch (NumberFormatException e) {
					expData[row][i] = 0;
				}
			}
			row++;
		}// while

		String outDir = dir + "\\output";

		File file = new File(outDir);
		boolean exists = file.exists();
		if (!exists) {
			(new File(outDir)).mkdir();
		}

		String midLog = dir + "\\output\\myEdgeIndex.txt"; // expand edgeIndex
															// from network.txt
		PrintWriter midOut = new PrintWriter(midLog);

		FileReader reader = new FileReader(networkFile);
		Scanner in = new Scanner(reader);// process network second time
		while (in.hasNextLine()) {
			String line = in.nextLine();

			int headLine = line.indexOf("Gene1");
			if (headLine == -1) {// there is no key word
				// System.out.println(line);

				String[] tokens = line.split("\\s");
				String first = tokens[0];
				String second = tokens[1];
				String forth = tokens[3]; // not defined clearly yet,
											// direction?transitional factor?

				int geneNo1 = Integer.parseInt(first);
				int geneNo2 = Integer.parseInt(second);
				int ppi = Integer.parseInt(forth);
				Gene gene1 = null;
				Gene gene2 = null;

				Iterator<Gene> anIter = preGeneList.iterator();
				while (anIter.hasNext()) {
					Gene g = anIter.next();
					if (g.getGeneNo() == geneNo1) {
						gene1 = g; // gene1 points to preGeneList
					} else if (g.getGeneNo() == geneNo2) {
						gene2 = g;
					}
				}

				String[] expRowsG1 = gene1.getExpRows().split("\\s");// expRowsG1
																		// expand
																		// entrez
																		// gene
																		// to
																		// rows
																		// in
																		// exp
																		// file
				String[] expRowsG2 = gene2.getExpRows().split("\\s");

				if ((expRowsG1.length > 0) && (gene1.getExpRows() != "")
						&& (expRowsG2.length > 0) && (gene2.getExpRows() != "")) {
					for (int i = 0; i < expRowsG1.length; i++) {
						for (int j = 0; j < expRowsG2.length; j++) {
							try {
								int rowG1 = Integer.parseInt(expRowsG1[i]); // the
																			// value-6
																			// equals
																			// the
																			// sample
																			// edgeIndex
																			// value
																			// of
																			// matlab
																			// code
								int rowG2 = Integer.parseInt(expRowsG2[j]);
								String probeId1 = expCol0[rowG1];
								String probeId2 = expCol0[rowG2];
								DSGeneMarker marker1 = null;
								DSGeneMarker marker2 = null;
								Edge anEdge = new Edge(geneNo1, geneNo2,
										marker1, marker2, rowG1, rowG2,
										probeId1, probeId2, ppi);// marker1,marker2
																	// are no
																	// use here,
																	// just for
																	// consistent
																	// with
																	// IDEAAnalysis
								edgeIndex.add(anEdge);// after calcu Null
														// distribution, if
														// there is a null file,
														// the edges in
														// edgeIndex will be
														// expired
								midOut.println(geneNo1 + "\t" + geneNo2 + "\t"
										+ rowG1 + "\t" + rowG2 + "\t" + ppi);
								gene1.addEdge(anEdge);// add the edge to related
														// gene in preGeneList
								gene2.addEdge(anEdge);
							} catch (Exception e) {
								System.out
										.println("exption:processing gene-exp row Number!");
								e.printStackTrace();
							}
						}
					}
				}

			}
		}// end of while

		midOut.close();
		for (Gene g : preGeneList) {
			String geneEdges = "";
			for (Edge e : g.getEdges()) {
				geneEdges += e.getGeneNo1() + " " + e.getGeneNo2() + " "
						+ e.getExpRowNoG1() + " " + e.getExpRowNoG2() + "\n";
			}
			System.out.println(g.getGeneNo() + "\tprobeIds:\t"
					+ g.getProbeIds() + "\nedges:\t" + geneEdges);
		}
		System.out.println("total gene:" + preGeneList.size());

		FileReader phenoreader = new FileReader(phenotypeFile);
		Scanner phenoin = new Scanner(phenoreader);
		Phenotype phenoType = new Phenotype();

		while (phenoin.hasNextLine()) {
			String line = phenoin.nextLine();
			String[] tokens = line.split("\\s");
			int phenoItemLength = tokens.length;
			if (line.indexOf(PHENO_INCLUDE) != -1) {
				int[] expCols = new int[phenoItemLength - 1];
				for (int i = 0; i < phenoItemLength - 1; i++) {
					expCols[i] = Integer.parseInt(tokens[i + 1]) - 1; // because
																		// the
																		// exp
																		// columns
																		// in
																		// phenotype
																		// file
																		// is
																		// from
																		// 1,
																		// however
																		// they
																		// are
																		// from
																		// 2 in
																		// exp
																		// file,
																		// not
																		// fix
																		// the
																		// major
																		// part
																		// yet
				}
				phenoType.setExpCols(expCols);
			} else if (line.indexOf(PHENO_EXCLUDE) != -1) {
				int[] expExcludeCols = new int[phenoItemLength - 1];
				for (int i = 0; i < phenoItemLength - 1; i++) {
					expExcludeCols[i] = Integer.parseInt(tokens[i + 1]) - 1;// same
																			// as
																			// above,
				}
				phenoType.setExcludeCols(expExcludeCols);
			}
		}

		double[] x = new double[expColLength - HEADCOL
				- phenoType.getExcludeCols().length];
		double[] y = new double[expColLength - HEADCOL
				- phenoType.getExcludeCols().length];
		int[] t = new int[expColLength - HEADCOL
				- phenoType.getExcludeCols().length];
		int jj = 0;
		for (int i = 0; i < expColLength - HEADCOL; i++) {
			boolean exclude = false;
			for (int j = 0; j < phenoType.getExcludeCols().length; j++) {
				if (i == (phenoType.getExcludeCols()[j]))
					exclude = true;
			}
			if (!exclude) {
				t[jj] = i;
				jj++;
			}
		}

		phenoType.setAllExpCols(t);

		for (int i = 0; i < expColLength - HEADCOL
				- phenoType.getExcludeCols().length; i++) {
			x[i] = expData[7270][t[i] + HEADCOL];
			y[i] = expData[1567][t[i] + HEADCOL];

		}
		MutualInfo mutual = new MutualInfo(x, y);
		double mi = mutual.getMI();

		System.out.println("first MI is " + mi);

		// ************Key process********************
		NullDistribution nullDist = new NullDistribution(preGeneList,
				edgeIndex, expData, phenoType, HEADCOL);
		nullDist.calcNullDist();
		edgeIndex = nullDist.getEdgeIndex();

		// *******************************************
		String edgeStr = "";
		edgeStr += "Gene1\tGene2\texpRow1\texpRow2\tDeltaCorr\tNormCorr\tzDeltaCorr\tLoc\tGoc\n";
		String strLoc = "";
		String strGoc = "";
		for (Edge e : edgeIndex) {// for debug
			if (e.isLoc())
				strLoc = "T";
			else
				strLoc = "";
			if ((e.isGoc()))
				strGoc = "T";
			else
				strGoc = "";
			edgeStr += e.getGeneNo1() + "\t" + e.getGeneNo2() + "\t"
					+ e.getExpRowNoG1() + "\t" + e.getExpRowNoG2() + "\t"
					+ e.getDeltaCorr() + "\t" + e.getNormCorr() + "\t"
					+ e.getzDeltaCorr() + "\t" + strLoc + "\t" + strGoc + "\n";
		}
		String fstr = dir + "\\output\\edgesReport.txt"; // expand edgeIndex
															// from network.txt
		PrintWriter out = new PrintWriter(fstr);
		out.println(edgeStr);
		out.close();

		List<Edge> locList = new ArrayList<Edge>();
		List<Edge> gocList = new ArrayList<Edge>();
		for (Edge anEdge : edgeIndex) {
			if (anEdge.isLoc())
				locList.add(anEdge);
			else if (anEdge.isGoc())
				gocList.add(anEdge);
		}
		Collections.sort(locList, new SortByZ());
		edgeStr = "";
		// edgeStr+="LOC---------------\n";
		edgeStr += "Probe1\tGene1\tProbe2\tGene2\tMI\tDeltaMI\tNormDelta\tZ-score\n";
		for (Edge e : locList) {
			edgeStr += e.getProbeId1() + "\t"
					+ probe_symbol.get(e.getProbeId1()) + "\t"
					+ e.getProbeId2() + "\t"
					+ probe_symbol.get(e.getProbeId2()) + "\t" + e.getMI()
					+ "\t" + e.getDeltaCorr() + "\t" + e.getNormCorr() + "\t"
					+ e.getzDeltaCorr() + "\n";
		}
		fstr = dir + "\\output\\output1_loc.txt"; // expand edgeIndex from
													// network.txt
		out = new PrintWriter(fstr);
		out.println(edgeStr);
		out.close();

		Collections.sort(gocList, new SortByZa());
		edgeStr = "";
		// edgeStr+="GOC---------------\n";
		edgeStr += "Probe1\tGene1\tProbe2\tGene2\tMI\tDeltaMI\tNormDelta\tZ-score\n";
		for (Edge e : gocList) {
			edgeStr += e.getProbeId1() + "\t"
					+ probe_symbol.get(e.getProbeId1()) + "\t"
					+ e.getProbeId2() + "\t"
					+ probe_symbol.get(e.getProbeId2()) + "\t" + e.getMI()
					+ "\t" + e.getDeltaCorr() + "\t" + e.getNormCorr() + "\t"
					+ e.getzDeltaCorr() + "\n";
		}
		fstr = dir + "\\output\\output1_goc.txt"; // expand edgeIndex from
													// network.txt
		out = new PrintWriter(fstr);
		out.println(edgeStr);
		out.close();

		for (Gene g : preGeneList) {// edge in preGeneList need update from
									// edgeIndex, because edgeIndex may be
									// updated from null distribution
			ArrayList<Edge> edges = new ArrayList<Edge>();
			for (Edge anEdge : g.getEdges()) {
				for (Edge eInEdgeIndex : edgeIndex) {
					if ((eInEdgeIndex.compareTo(anEdge) == 0)
							&& (eInEdgeIndex.getGeneNo1() == g.getGeneNo())) {
						edges.add(eInEdgeIndex);
					}
				}
			}
			g.setEdges(edges);// replace the old edges

		}

		TreeSet<ProbeGene> probes = new TreeSet<ProbeGene>();// process probes,
																// which is a
																// alternative
																// way to
																// evaluate
																// genes other
																// than entrez
																// genes which I
																// call gene in
																// this code
		for (Edge e : edgeIndex) {
			ProbeGene p1 = new ProbeGene(e.getProbeId1());
			ProbeGene p2 = new ProbeGene(e.getProbeId2());
			probes.add(p1);
			probes.add(p2);
		}

		for (ProbeGene p : probes) {
			// System.out.println(p.getProbeId());
			ArrayList<Edge> edges = new ArrayList<Edge>();
			for (Edge e : edgeIndex) {
				if ((p.getProbeId() == e.getProbeId1())
						|| (p.getProbeId() == e.getProbeId2()))
					edges.add(e);
			}
			p.setEdges(edges);
		}

		for (ProbeGene p : probes) { // enrichment to find the significant probe
			int locs = 0;
			int gocs = 0;
			for (Edge anEdge : p.getEdges()) {
				if (anEdge.isLoc()) {
					locs++;
				} else if (anEdge.isGoc()) {
					gocs++;
				}
			}

			p.setLocs(locs);// divided by 2 because each egde has two genes, the
							// LOC were counted twice
			p.setGocs(gocs);

		}

		int allLoc = 0;
		int allGoc = 0;
		for (Gene g : preGeneList) { // enrichment to find the significant
										// entrez genes
			int locs = 0;
			int gocs = 0;
			for (Edge anEdge : g.getEdges()) {
				if (anEdge.isLoc()) {
					locs++;
				} else if (anEdge.isGoc()) {
					gocs++;
				}
			}

			g.setLocs(locs);// divided by 2 because each egde has two genes, the
							// LOC were counted twice
			g.setGocs(gocs);
			allLoc += g.getLocs();
			allGoc += g.getGocs();
		}

		int allLoc2 = 0; // vv remove the following 10 lines should be removed
							// after test
		int allGoc2 = 0;
		for (Edge anEdge : edgeIndex) {
			if (anEdge.isLoc()) {
				allLoc2++;
				// System.out.println(anEdge.getGeneNo1()+"\t"+anEdge.getGeneNo2()+"\t"+(anEdge.getExpRowNoG1()-6)+"\t"
				// +(anEdge.getExpRowNoG2()-6)+"\tDeltaCorr:\t"+anEdge.getDeltaCorr()+"\tNormCorr:\t"+anEdge.getNormCorr()+"\tLOC:"+anEdge.getLoc()+"\tGOC:"+anEdge.getGoc());
			} else if (anEdge.isGoc())
				allGoc2++;
		}
		System.out.println("loc1:" + allLoc + "\tloc2:" + allLoc2);
		System.out.println("goc1:" + allGoc + "\tgoc2:" + allGoc2); // ^^remove

		int N = edgeIndex.size();
		int Sl = allLoc;
		int Sg = allGoc;
		// calculate LOC p-value using fisher exact test to evaluate entrez
		// genes
		for (Gene g : preGeneList) {
			int H = g.getEdges().size();
			FisherExact fe = new FisherExact(2 * edgeIndex.size());
			if (g.getLocs() > 0) {
				int Dl = g.getLocs();
				double cumulativeP = fe.getCumlativeP(Dl, H - Dl, Sl - Dl, N
						- Sl - H + Dl);
				g.setCumLoc(cumulativeP);
				// System.out.println("*"+g.getGeneNo()+"\nLOC\tpValue:\t"+cumulativeP+"\n"+g.getProbeIds()+"\na:"+Dl+"\tb:"+(H-Dl)+"\tc:"+(Sl-Dl)+"\td:"+(N-Sl-H+Dl));
				// System.out.println();
			}
			if (g.getGocs() > 0) {
				int Dg = g.getGocs();
				double cumulativeP = fe.getCumlativeP(Dg, H - Dg, Sg - Dg, N
						- Sg - H + Dg);
				g.setCumGoc(cumulativeP);
				// System.out.println("*"+g.getGeneNo()+"\nGOC\tpValue:\t"+cumulativeP+"\n"+g.getProbeIds()+"\na:"+Dg+"\tb:"+(H-Dg)+"\tc:"+(Sg-Dg)+"\td:"+(N-Sg-H+Dg));
				// System.out.println();
			}
		}

		for (ProbeGene p : probes) {
			int H = p.getEdges().size();
			FisherExact fe = new FisherExact(2 * edgeIndex.size());
			if (p.getLocs() > 0) { // calculate LOC p-value using fisher exact
									// test to evaluate probe
				int Dl = p.getLocs();
				double cumulativeP = fe.getCumlativeP(Dl, H - Dl, Sl - Dl, N
						- Sl - H + Dl);
				p.setCumLoc(cumulativeP);
				// System.out.println("*"+g.getGeneNo()+"\nLOC\tpValue:\t"+cumulativeP+"\n"+g.getProbeIds()+"\na:"+Dl+"\tb:"+(H-Dl)+"\tc:"+(Sl-Dl)+"\td:"+(N-Sl-H+Dl));
				// System.out.println();
			}
			if (p.getGocs() > 0) {
				int Dg = p.getGocs();
				double cumulativeP = fe.getCumlativeP(Dg, H - Dg, Sg - Dg, N
						- Sg - H + Dg);
				p.setCumGoc(cumulativeP);
				// System.out.println("*"+g.getGeneNo()+"\nGOC\tpValue:\t"+cumulativeP+"\n"+g.getProbeIds()+"\na:"+Dg+"\tb:"+(H-Dg)+"\tc:"+(Sg-Dg)+"\td:"+(N-Sg-H+Dg));
				// System.out.println();
			}
			double locnes = -Math.log(p.getCumLoc());
			double gocnes = -Math.log(p.getCumGoc());
			double nes = locnes + gocnes;
			p.setNes(nes);
		}

		List<ProbeGene> probeNes = new ArrayList<ProbeGene>();
		for (ProbeGene p : probes) {
			probeNes.add(p);
		}
		Collections.sort(probeNes, new SortByNes());

		String nodeStr = "";
		nodeStr += "Probe\tGene\tChrBand\tConn\tNes\tLoc\tLoCHits\tLoCEs\tLoCNes\tGoc\tGoCHits\tGoCEs\tGoCNes\n";
		for (ProbeGene p : probeNes) {// present significant nodes
			int locHits = 0;
			int gocHits = 0;
			for (Edge e : p.getEdges()) {
				if (e.getDeltaCorr() < 0)
					locHits++;
				else if (e.getDeltaCorr() > 0)
					gocHits++;
			}
			double locnes = -Math.log(p.getCumLoc());
			double gocnes = -Math.log(p.getCumGoc());

			// if((p.getLocs()>0&&p.getCumLoc()<0.05)||(p.getGocs()>0&&p.getCumGoc()<0.05)){
			nodeStr += p.getProbeId() + "\t" + probe_symbol.get(p.getProbeId())
					+ "\t";
			nodeStr += probe_chromosomal.get(p.getProbeId()) + "\t"
					+ p.getEdges().size() + "\t" + p.getNes() + "\t"
					+ p.getLocs() + "\t" + locHits + "\t" + p.getCumLoc()
					+ "\t" + locnes + "\t" + p.getGocs() + "\t" + gocHits
					+ "\t" + p.getCumGoc() + "\t" + gocnes + "\n";
			// }
		}
		fstr = dir + "\\output\\output2.txt"; // expand edgeIndex from
												// network.txt
		out = new PrintWriter(fstr);
		out.println(nodeStr);
		out.close();

		nodeStr = "";
		nodeStr += "Gene1\tGene2\tconn_type\tLoc\tGoc\n";
		for (ProbeGene p : probes) {// present significant node with its edges
			if ((p.getCumLoc() < 0.05) || (p.getCumGoc() < 0.05)) {
				// nodeStr+=p.getProbeId()+"\n";
				for (Edge e : p.getEdges()) {
					String isLoc = "";
					String isGoc = "";
					String ppi = "";
					if (e.isLoc())
						isLoc = "X";
					if (e.isGoc())
						isGoc = "X";
					if (e.getPpi() == 1)
						ppi = "ppi";
					else
						ppi = "pdi";

					nodeStr += e.getProbeId1() + "\t" + e.getProbeId2() + "\t"
							+ ppi + "\t" + isLoc + "\t" + isGoc + "\n";
				}
				// System.out.println(nodeStr);

			}
		}
		fstr = dir + "\\output\\output3.txt"; // expand edgeIndex from
												// network.txt
		out = new PrintWriter(fstr);
		out.println(nodeStr);

		out.close();

		System.out.println("Done!");

	}// end of main

}// end of class testIdea

class SortByZ implements Comparator<Edge> {

	public int compare(Edge o1, Edge o2) {
		if ((o1.getzDeltaCorr() - o2.getzDeltaCorr()) == 0)
			return 0;
		else if ((o1.getzDeltaCorr() - o2.getzDeltaCorr()) > 0)
			return 1;
		else
			return -1;
	}
}

class SortByZa implements Comparator<Edge> {

	public int compare(Edge o1, Edge o2) {
		if ((o1.getzDeltaCorr() - o2.getzDeltaCorr()) == 0)
			return 0;
		else if ((o1.getzDeltaCorr() - o2.getzDeltaCorr()) < 0)
			return 1;
		else
			return -1;
	}
}

class SortByNes implements Comparator<ProbeGene> {

	@Override
	public int compare(ProbeGene p1, ProbeGene p2) {
		// TODO Auto-generated method stub
		if (p1.getNes() == p2.getNes())
			return 0;
		else if (p1.getNes() < p2.getNes())
			return 1;
		else
			return -1;
	}

}
