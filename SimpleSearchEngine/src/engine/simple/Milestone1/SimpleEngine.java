package engine.simple.Milestone1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.tartarus.snowball.ext.PorterStemmer;

/**
 * A very simple search engine. Uses an inverted index over a folder of JSON files.
 * 
 * @author vaibhavjain
 * @version 1.1
*/
public class SimpleEngine {
	final PositionalIndex index = new PositionalIndex();
	//final static Pattern nonAlphaNumeric = Pattern.compile("(^[^a-zA-Z0-9]*)(.*?)([^a-zA-Z0-9]*)");
	final static Pattern nonAlphaNumeric = Pattern.compile("^([\\W]*)(.*?)([\\W]*)$");
	final static Pattern nonAlphaNumericForQuery = Pattern.compile("(^[^a-zA-Z0-9-]*)(.*?)([^a-zA-Z0-9]*$)");
	final List<String> fileNames = new ArrayList<String>();
	int mDocumentID  = 0;
	int fileIndex = 0;
	
		
	public List<String> visitAllFiles(Path currentWorkingPath) throws IOException {
		// the list of file names that were processed
		// This is our standard "walk through all .json files" code.
		FileOutputStream docWeightStream = new FileOutputStream(new File(currentWorkingPath.toString(), "docWeights.bin"));
		ByteArrayOutputStream docWeightArrayStream = new ByteArrayOutputStream();
		Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
			
			
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				// make sure we only process the current working directory
				if (currentWorkingPath.equals(dir)) {
					return FileVisitResult.CONTINUE;
				}
				return FileVisitResult.SKIP_SUBTREE;
			}

			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				// only process .json files
				if (file.toString().endsWith(".json")) {
					// we have found a .json file; add its name to the fileName list,
					// then index the file and increase the document ID counter.
					fileIndex++;
					System.out.println("Indexing file " + file.getFileName());
					fileNames.add(file.getFileName().toString());
					double ld = indexFile(file.toFile(), index, mDocumentID);
					mDocumentID++;
					try {
						docWeightArrayStream.write(ByteBuffer.allocate(8).putDouble(ld).array());
					} catch (IOException ioe) {
							ioe.printStackTrace();
					}
				}
				return FileVisitResult.CONTINUE;
			}
			
			// don't throw exceptions if files are locked/other errors occur
			public FileVisitResult visitFileFailed(Path file, IOException e) {
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				System.out.println("Total Files Indexed: " + fileIndex);
				docWeightArrayStream.flush();
				docWeightStream.write(docWeightArrayStream.toByteArray());
				docWeightStream.close();
				return super.postVisitDirectory(dir, exc);
			}
		});
		return fileNames;
	}
	
	/**
   	Indexes a file by reading a series of tokens from the file, treating each 
   	token as a term, and then adding the given document's ID to the inverted
   	index for the term.
   	@param file a File object for the document to index.
   	@param index the current state of the index for the files that have already
   	been processed.
   	@param docID the integer ID of the current document, needed when indexing
   	each term from the document.
   	@return The Euclidian Normalization value Ld for this file.
	*/
	private static double indexFile(File file, PositionalIndex index, int docID) {
		// finish this method for indexing a particular file.
		// Construct a SimpleTokenStream for the given File.
		// Read each token from the stream and add it to the index.
		
		JSONParser parser = new JSONParser();
		HashMap<String, Integer> termFrequencyMap = new HashMap<String, Integer>();
		try{
			JSONObject jObject = (JSONObject) parser.parse(new FileReader(file));
			String document = jObject.get("body").toString();
			SimpleTokenStream tokenStream = new SimpleTokenStream(document);
			String token;
			int positionToAdd = 1;
			while(tokenStream.hasNextToken()) {
				token = removeApostrophes(stripNonAlphaNumeric(tokenStream.nextToken()));
				
				if(token != null && token.contains("-")  && token.length() > 1){
					String token1 = token.substring(0, token.indexOf("-")).toLowerCase();
					String token2 = token.substring(token.indexOf("-") + 1, token.length()).toLowerCase();
					String token3 = token.replaceAll("-", "").toLowerCase();
					
					if(token1.length() > 0 && token2.length() > 0 && token3.length() > 0) {
						String term1 = applyPorter2Stemmer(removeApostrophes(stripNonAlphaNumeric(token1)));
						String term2 = applyPorter2Stemmer(removeApostrophes(stripNonAlphaNumeric(token2)));
						String term3 = applyPorter2Stemmer(removeApostrophes(stripNonAlphaNumeric(token3)));
						
						if(term3.length() > 0) {
							index.addTerm(term3, docID, positionToAdd);
							addOrUpdateTermFreqMap(termFrequencyMap, term3);
						}
						
						if(term1.length() > 0) {
							index.addTerm(term1, docID, positionToAdd++);
							addOrUpdateTermFreqMap(termFrequencyMap, term1);
						}
						
						if(term2.length() > 0) {
							index.addTerm(term2, docID, positionToAdd++);
							addOrUpdateTermFreqMap(termFrequencyMap, term2);
						}
					}
				} else if(token !=null && !token.contains("-")){
					token = token.toLowerCase();
					String term = applyPorter2Stemmer(token);
					if(term.length() > 0) {
						index.addTerm(term, docID, positionToAdd++);
						addOrUpdateTermFreqMap(termFrequencyMap, term);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return calculateDocWeight(termFrequencyMap);
	}
	
	/**
	 * Strips all the non alpha-numeric characters from beginning and end. '-' is allowed for NOT queries.
	 * @param token - Token to strip non alpha-numeric characters from.
	 * @return token
	*/
	public static String stripNonAlphaNumeric(String token) {
		if(token == null)
			return null;
		Matcher matcher = nonAlphaNumeric.matcher(token);
	   	return stripChracters(matcher);
	}
	
	/**
	 * Strips all the non-alpha numeric characters. The exceptions is a hyphen ("-"). This is because a hyphen is allowed for not queries and
	 * hyphenated queries.
	 * @param token - A token to remove characters from.
	 * @return - A term that is perfectly accepted by the search engine.
	 */
	public static String stripNonAlphaNumericForQuery(String token) {
		Matcher matcher = nonAlphaNumericForQuery.matcher(token);
		return stripChracters(matcher);
	}
	
	
	private static String stripChracters(Matcher matcher) {
		String newString= null;
		if(matcher.matches()) {
			newString = matcher.group(2);
		}	
		return newString;
	}
   
	/**
	 * Removes Apostrophes from a token.
	 * @param token
	 * @return
	 */
	public static String removeApostrophes(String token) {
		if(token == null)
			return null;
		if(token.contains("'")) {
			token = token.replaceAll("'", "");
		}
		return token;
	}
   
	/**
	 * An Apache Lucene version of the Porter2 Stemmer. Stems the given token and returns the respective term.
	 * @param token A token to stem.
	 * @return term A Stemmed term.
	 */
	public static String applyPorter2Stemmer(String token) {
		String term = null;
		PorterStemmer stemmer = new PorterStemmer();
		stemmer.setCurrent(token);
		stemmer.stem();
		term = stemmer.getCurrent();
		return term;
	}
   
	public String[] getDictionary() {
		return index.getDictionary();
	}
   
	public PositionalIndex getIndex() {
		return index;
	}
   
	/**
	 * Adds(if absent) or updates(if present) a term in the vocabulary map.
	 * @param map
	 * @param term
	 */
	private static void addOrUpdateTermFreqMap(HashMap<String, Integer> map, String term) {
		if(map.containsKey(term)) {
			map.put(term, map.get(term) + 1);
		} else {
			map.put(term, 1);
		}
	}
   
	/**
	 * Calculates the Ld value for current document.
	 * @param docWeightMap
	 * @return Ld value
	 */
	private static double calculateDocWeight(HashMap<String, Integer> docWeightMap) {
		if(docWeightMap.isEmpty())
			return 0;
		double docLength = 0;
		for(String term : docWeightMap.keySet()) {
			docLength += calculateTermWeightSquared(docWeightMap.get(term));
		}
		return Math.sqrt(docLength);
	}
   
	private static double calculateTermWeightSquared(Integer termFreq) {
		double weight = 1 + Math.log(termFreq);
		return weight * weight;
	}
   
	public List<String> getFileNamesList() {
		return fileNames;
	}
}
