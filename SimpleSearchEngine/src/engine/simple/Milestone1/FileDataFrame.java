package engine.simple.Milestone1;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * This Frame class is used for displaying file contents to the user when user double-clicks on a file in the result of a search query.
 * @author vaibhavjain
 * @version 1.0
 */
public class FileDataFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private File filename;
	private JTextArea fileContent;
	
	public FileDataFrame(File filename){
		this();
		setTitle(filename.getName());
		this.filename = filename;
	}
	
	public FileDataFrame() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 671, 498);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		fileContent = new JTextArea();
		fileContent.setFont(new Font("Arial", Font.PLAIN, 15));
		fileContent.setBounds(12, 13, 629, 425);
		fileContent.setLineWrap(true);
		fileContent.setWrapStyleWord(true);
		fileContent.setEditable(false);
		
		JScrollPane scrollPane = new JScrollPane(fileContent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBounds(12, 13, 630, 425);
		contentPane.add(scrollPane);
	}
	
	/**
	 * Sets the textarea with file contents.
	 */
	public void initData() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			JSONParser parser = new JSONParser();
			JSONObject jObject = (JSONObject)parser.parse(line);
			String content = jObject.get("body").toString();
			fileContent.setText(content);
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
