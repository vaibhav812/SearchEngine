package engine.simple.Milestone1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads tokens one at a time from an input stream. Returns tokens with minimal
 * processing: removing all non-alphanumeric characters, and converting to 
 * lowercase.
 * 
 * @version 1.0.1
*/
public class SimpleTokenStream implements TokenStream {
	private Scanner mReader;
	private Pattern nonAlphaPattern = Pattern.compile("^([\\W]*)(.*?)([\\W]*)$");
	/**
		Constructs a SimpleTokenStream to read from the specified file.
	*/
	public SimpleTokenStream(File fileToOpen) throws FileNotFoundException {
		mReader = new Scanner(new FileReader(fileToOpen));
	}	
	
	/**
   		Constructs a SimpleTokenStream to read from a String of text.
	*/
	public SimpleTokenStream(String text) {
		mReader = new Scanner(text);
	}

	/**
   		Returns true if the stream has tokens remaining.
	*/
	@Override
	public boolean hasNextToken() {
		return mReader.hasNext();
	}

	/**
   		Returns the next token from the stream, or null if there is no token
   		available.
	*/
	@Override
	public String nextToken() {
		if (!hasNextToken())
			return null;
		
		//String next = mReader.next().replaceAll("\\W", "").toLowerCase();
		String next = mReader.next();
		Matcher matcher = nonAlphaPattern.matcher(next);
		if(matcher.matches()) {
			next = matcher.group(2);
		}
		return next.length() > 0 ? next : hasNextToken() ? nextToken() : null;
	}
}