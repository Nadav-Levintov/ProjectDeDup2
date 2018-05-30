import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

public class ParserSolver {

	private String fileName;												
	private ArrayList<String> output = new ArrayList<String>();				// Save the relevant information about the file system (to the export file).
	private double time = 0;													// execution time.
	private double timeInput = 0;												// duration of creating input to cplex (files&blocks arrays).
	private IloCplex cplex;													
	private long totalMoveSpace = 0;										// total space that is moved 
	private long totalCopySpace = 0;										// total space that is copied 
	private int numOfMoveFiles = 0;											// number of files to be moved 
	private int numOfMoveBlocks = 0, numOfCopyBlocks = 0;					// number of blocks to be copied 
	private ArrayList<Integer> moveFile = new ArrayList<Integer>();			// Array of files to be moved
	private ArrayList<Integer> moveBlock = new ArrayList<Integer>();		// Array of blocks to be moved
	private ArrayList<Integer> copyBlock = new ArrayList<Integer>();		// Array of blocks to be copied
	private long totalSize = 0;												// Total system size
	private int inputSize = 0;												// size of the files&blocks arrays.
	private String numK;
	private String epsilon;
	private long targetMove;
	private long targetEpsilon;
	
	private int nFiles = 0;													// number of input files
	private int nBlocks = 0;												// number of input blocks
	
	/* For Maor's input -- 
	private int firstFileSystem=0;										
	private int lastFileSystem=0;
	private int numOfFS=0;													
	private float target=0; 												
	private String heuristics;
	*/

	public void parseAndSolve(String filename, String K, String eps) {

		fileName = filename;
		File file = new File(fileName);
		BufferedReader br = null;

		int i;

		/*
		long startTime = System.nanoTime();
        long duration = System.nanoTime() - startTime;
        timeInput = duration/1000000000.0;
		 */

		numK = K;
		epsilon = eps;

		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String st;	// reads line after line from inputFile


		try {
			cplex = new IloCplex();

			IloLinearNumExpr blockSizeCopy = cplex.linearNumExpr();	// Sigma(size(i)*c(i))
			IloLinearNumExpr blockSizeMove = cplex.linearNumExpr();	// Sigma(size(i)*m(i))

			try {	// set up the solver
				st = br.readLine();

					while(st.charAt(0)=='#') {	// read the header for info
						String[] t = st.split(" ");
						/* Maor's files -- ignore
						if(t[0].equals("#Output") || (t[1].equals("Output") && t[2].equals("type:"))) { // type of heuristics
							if (t[2].equals("heuristic"))	// Maor's way of saying "block"...
								heuristics = "Block"; 
							else {		// Matan
								heuristics = t[t.length-1];
							}
								
						} 

						if(t[0].equals("#Target")) { // target number of blocks
							target = Float.parseFloat(t[t.length-1]); 
						} 

						if(t[0].equals("#Input") && t[1].equals("files:")) { // input files
							String fs[] = t[2].split(",");
							numOfFS = fs.length;
							firstFileSystem = Integer.parseInt(fs[0]);
							lastFileSystem = Integer.parseInt(fs[fs.length-1]);
						}
						
						if(t[0].equals("#Number") || (t[1].equals("Num"))) {	
							if(t[2].equals("blocks") || t[2].equals("blocks:") || t[2].equals("physical"))
								nBlocks = Integer.parseInt(t[t.length-1]);

							// to continue here with files
						}
						*/ // end of Maor's files
						
						// find number of files and blocks in header
						if(t.length >= 4) {
							if(t[2].equals("files:")) {		// # Num Files: <nFiles>
								nFiles = Integer.parseInt(t[3]);
							}
							if(t[2].equals("blocks:")) {	// # Num Blocks: <nblocks>
								nBlocks = Integer.parseInt(t[3]);
							}
						}
						st = br.readLine();
					}

					// array of cplex variables for blocks to be moved
					IloNumVar[]m = new IloNumVar[nBlocks];
					// array of cplex varialbes for blocks to be copied
					IloNumVar[]c = new IloNumVar[nBlocks];

					int[] blocks = new int[nBlocks];	// initialize blocks array, to mark which block has been read yet and to save their sizes

					// -1 = has not been read yet
					for(i=0; i<nBlocks; i++)
						blocks[i] = -1;

					for(i=0; i<nBlocks; i++)
					{
						c[i] = cplex.numVar(0, 1, IloNumVarType.Int);	// 0 <= c[i] <= 1
						m[i] = cplex.numVar(0, 1, IloNumVarType.Int);	// 0 <= m[i] <= 1
						inputSize+=2;
						
						cplex.addLe(cplex.sum(c[i],m[i]),1); // c[i]+m[i] <= 1 	
					}
					
					while(st.charAt(0)=='F') {	// while at files list

						// nFiles++;	// Maor needs to count files

						// get sizes of blocks and add to total, and add their term to the cplex formula
						String[] fTemp = st.split(",");		//split into chunks between ','

						// [0]F, [1]file id, [2]file name, [3]directory, [4]num of blocks, [5+2i]block i id, [6+2i]block i size
						
						for(i=5; i<fTemp.length; i=i+2) {        					
							int blockSn = Integer.parseInt(fTemp[i]);	// add block id to list of file blocks
							
							if (blocks[blockSn] == -1) {	// block hasn't been seen yet in list of files								
								double temp = Math.ceil(Double.parseDouble(fTemp[i+1])/1024);	// block size in kb
								blocks[blockSn] = (int)(temp);
								totalSize += blocks[blockSn];
								blockSizeCopy.addTerm(blocks[blockSn], c[blockSn]);	// c[i]*size[i]
								blockSizeMove.addTerm(blocks[blockSn], m[blockSn]);	// m[i]*size[i]
								inputSize += 2; 
							}
						}	

						st = br.readLine();
					}// End while(F)


					IloNumVar[]f = new IloNumVar[nFiles];	// initialize files list
					for(i=0; i<f.length; i++)
					{
						f[i] = cplex.numVar(0, 1, IloNumVarType.Int);	// 0 <= f[i] <= 1 
						cplex.addLe(0,f[i]);
						cplex.addLe(f[i],1);
						inputSize++;
					}
					
					while(st.charAt(0)=='B' || st.charAt(0)=='P') {	// while at blocks/phsysical files list	
						String[] bTemp = st.split(",");
						// [0]F, [1]file id, [2]file name, [3]directory, [4]num of blocks, [5+2i]block i id, [6+2i]block i size

						int bsn = Integer.parseInt(bTemp[1]);	
						for(i=4; i<bTemp.length; i++) { 	// list of file sns that the block is contained in
							int fsn = Integer.parseInt(bTemp[i]); 
							cplex.addLe(m[bsn], f[fsn]);						// mi <= fj
							cplex.addLe(f[fsn],cplex.sum(m[bsn],c[bsn]));		// fj <= mi+ci
							inputSize++;
						}

						st = br.readLine();
						if(st == null) break;
					}// End if(B)

				
				cplex.addMinimize(blockSizeCopy); // ask cplex to minimize the total size of copied blocks

				long onePercent;
				
				long lowerbound,upperbound;
				// Calculate K, eps
				if(K.charAt(K.length()-1) == '%') {	//input asked for values in %
					onePercent = totalSize/100;
					K = K.substring(0, K.length()-1);
					targetMove = onePercent*Long.parseLong(K); 
					eps = epsilon.substring(0, Math.min(epsilon.length()-1,5));
					targetEpsilon = (long)(onePercent*Double.parseDouble(eps)); 
				}
				else	// input asked in absolute values
				{
					targetMove = Long.parseLong(K); 
					targetEpsilon = Long.parseLong(eps);
				}

				// constraints: kTemp+epsTemp <= blockSizeMove <= kTemp+epsTemp
				lowerbound = targetMove - targetEpsilon;
				upperbound = targetMove + targetEpsilon;
				cplex.addLe(lowerbound, blockSizeMove);
				cplex.addLe(blockSizeMove, upperbound);
				inputSize += 2;
				
				if (cplex.solve()) {	//run silver

					//	Count and mark the files that move
					for (i=0; i<f.length; i++) {
						if(cplex.getValue(f[i]) == 1) {
							// files.get(i).setDelete(1);
							numOfMoveFiles++;
							moveFile.add(i);
						}
					}


					for (i=0; i<m.length; i++) {
						//	Count and mark the blocks that are moved + their size
						if(cplex.getValue(m[i]) == 1) {
							totalMoveSpace += blocks[i];
							numOfMoveBlocks++;
							moveBlock.add(i);
						}
						//	Count and mark the blocks that are copied + their size
						if(cplex.getValue(c[i]) == 1) {
							totalCopySpace += blocks[i];
							numOfCopyBlocks++;
							copyBlock.add(i);
						}
					}

					time = cplex.getCplexTime();

				}
				else {
					if(JOptionPane.showConfirmDialog(null, "Bad file!\n Problem not solved.", "Error message", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE)==0)
						System.exit(0);
				}
				
				
			} catch (IOException e) { // end readfile try
				e.printStackTrace();
			}
			time = cplex.getCplexTime();
			cplex.end();
		} catch (IloException e) { // end cplex try
			e.printStackTrace();
		}// End try



	}

	// get functions
	
	public ArrayList<String> getOutput() {
		return output;
	}

	
	public String getFileName() {
		return fileName;
	}

	public double getTime() {
		return time;
	}

	public double getTimeInput() {
		return timeInput;
	}

	public long getTotalMoveSpace() {
		return totalMoveSpace;
	}

	public long getTotalCopySpace() {
		return totalCopySpace;
	}

	public int getNumOfMoveFiles() {
		return numOfMoveFiles;
	}

	public int getNumOfFiles() {
		return nFiles;
	}

	public int getNumOfBlocks() {
		return nBlocks;
	}

	public int getNumOfMoveBlocks() {
		return numOfMoveBlocks;
	}

	public int getNumOfCopyBlocks() {
		return numOfCopyBlocks;
	}


	public ArrayList<Integer> getMoveFile() {
		return moveFile;
	}

	public ArrayList<Integer> getMoveBlock() {
		return moveBlock;
	}

	public ArrayList<Integer> getCopyBlock() {
		return copyBlock;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public int getInputSize() {
		return inputSize;
	}

	public String getNumK() {
		return numK;
	}

	public long getTargetMove() {
		return targetMove;
	}
	
	public long getTargetEpsilon() {
		return targetEpsilon;
	}

	/* Maor
	public float getTarget() {
		return target;
	}

	public int getFirstFS() {
		return firstFileSystem;
	}

	public int getLastFS() {
		return lastFileSystem;
	}

	public int getNumOfFS() {
		return numOfFS;
	}
	
	public String getHeuristics() {
		return heuristics;
	}
	*/

}
