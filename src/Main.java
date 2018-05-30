import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {	// expected input from commandline: filename k epsilon
		
		
		if(args.length < 3) {
			System.out.println("usage: filename k epsilon \n");
			return;
		}
		
		ParserSolver solve = new ParserSolver();
		String fileName, k, epsilon;
		// boolean obj;
		
		fileName = args[0];
		// get all file name parts: path parts, name and extension
		String[] fileParts = fileName.split("\\.");
		String[] nameParts  = fileParts[0].split("\\/");
		
		k = args[1];
		epsilon = args[2];
		
		// parse input and run solver
		solve.parseAndSolve(fileName, k, epsilon);
		
		// prepare output file name: <original_filename>_<k>_<epsilon>_result.csv
		String outputFileName = "";
		for (int i=0; i<nameParts.length-1; i++){
			outputFileName += nameParts[i];
			outputFileName += "/";
		}
		outputFileName += "output/";
		outputFileName += nameParts[nameParts.length-1];
		outputFileName += "_"+k+"_"+epsilon+"_result.csv";
	
		File exportFile = new File(outputFileName);
		
		// write output data from solver to output file
		try {
			 
			if(exportFile.createNewFile()) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile));
	
				/* output for Maor's input -- ignore
				writer.write(solve.getNumOfFS() + ",");
				writer.write(solve.getFirstFS() + ",");
				writer.write(solve.getLastFS() + ",");
				writer.write(solve.getTarget() + ",");
				writer.write(solve.getHeuristics() + ",");
				*/

				writer.write(solve.getNumOfFiles() + ",");
				writer.write(solve.getNumOfBlocks() + ",");
				writer.write(solve.getTargetMove() + ",");
				writer.write(solve.getTargetEpsilon() + ",");
				writer.write(solve.getTimeInput() + ",");
				writer.write(solve.getInputSize() + ",");
				writer.write(solve.getTime() + ",");
				writer.write("RAM=,");
				writer.write(solve.getTotalMoveSpace() + ",");
				writer.write(solve.getTotalCopySpace() + ",");
				writer.write(solve.getNumOfFiles() + ",");
				writer.write(solve.getNumOfMoveBlocks() + ",");
				writer.write(solve.getNumOfCopyBlocks()+ ","); 

				writer.close();
				System.out.println("File "+outputFileName+" has been created!");	
			}
		} catch (IOException e1) {
			System.out.println("File" + outputFileName+" already exists.");
		} 

	}
	
}
