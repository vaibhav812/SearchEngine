package engine.simple.Milestone2;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * Class that implements variable  byte encoding technique. It implements the algorithm from "An Introduction to Information Retrieval" with 
 * some minor changes to improve efficiency. 
 * @author vaibhavjain
 * @version 1.0
 */
public class VariableByteEncoding {
	
	/** 
	 * Encodes an integer and returns the value in a variable byte array.
	 * @param number
	 * @return
	 */
	public static byte[] encodeNumber(int number) {
		if (number == 0) {
			byte[] encodedBytes = new byte[1];
			encodedBytes[0] = (byte) 0;
			encodedBytes[0] += 128;
			return encodedBytes;
		}
		//Estimate the number of bytes the integer will occupy.
		//Number of bits required = log base 2(number).
		//but since we need bytes, we divide it with log(128)
		int sizeInBytes = (int) (Math.log(number) / Math.log(128)) + 1;
		byte[] encodedBytes = new byte[sizeInBytes];
		int i = sizeInBytes - 1;
		//Start filling byte array
		while(i >= 0) {
			encodedBytes[i--] = (byte) (number % 128);
			number /= 128;
		}
		//Add 128 to the last byte to change MSB to 1. This is the termination byte.
		encodedBytes[sizeInBytes - 1] += 128;
		return encodedBytes;
	}
	
	/**
	 * Encodes a list of Integer.
	 * @param numbers
	 * @return
	 */
	public static byte[] encode(List<Integer> numbers) { 
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		for (Integer number : numbers) {
			try {
				stream.write(encodeNumber(number));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return stream.toByteArray();
	}
	
	/** 
	 * Returns the next detected integer from a given file.
	 * @param file
	 * @return
	 */
	public static int decodeNextNumber(RandomAccessFile file) {
		int number = 0;
		int decodedNum = -1;
		while (true) {
			try {
				byte b = (byte) file.read();
				//While it is less than 128, keep on adding values to number.
				if ((b & 0xff) < 128) {
					number = 128 * number + b;
				} else {
					//Termination byte detected. Parse and return the whole number.
					decodedNum = (128 * number + ((b - 128) & 0xff));
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return decodedNum;
	}
}
