package engine.simple.Milestone3;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import engine.simple.Milestone1.FileDataFrame;
import engine.simple.Milestone1.SimpleEngine;
import engine.simple.Milestone2.DiskPosting;
import engine.simple.Milestone2.ModeOptionFrame;

/**
 * A copy of QueryFrame code which has been changed to call TieredIndexWithScore(Boolean and Ranked) instead of TieredIndex.
 * @author vaibhavjain
 *
 */
public class QueryFrameWithScore extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JButton btnSearch;
	private JTextField textField;
	private JRadioButton rdbtnBoolean;
	private JRadioButton rdbtnRanked;
	private String dirPath;
	private File dirFile;
	private JList<String> outputList;
	private JLabel lblTotal;
	private TieredBooleanQueryParserWithScore booleanParser = null;
	private TieredRankedQueryParserWithScore rankedParser = null;
	public static String[] filenames = null;
	private JProgressBar progressBar;
	private TieredDiskInvertedIndexWithScore diskIndex = null;
	
	public TieredDiskInvertedIndexWithScore getDiskIndex() {
		return diskIndex;
	}

	public void setDiskIndex(TieredDiskInvertedIndexWithScore diskIndex) {
		this.diskIndex = diskIndex;
	}

	/**
	 * Create the frame.
	 */
	public QueryFrameWithScore(String dirPath) {
		setTitle("Search Queries With Score");
		this.dirPath = dirPath;
		dirFile = new File(dirPath);
		//Get all the .json filenames from the directory
		filenames = dirFile.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if(name.endsWith(".json"))
					return true;
				else 
					return false;
			}
		});
		
		//Initialize the DiskInvertedIndex
		if(diskIndex == null) {
			diskIndex = new TieredDiskInvertedIndexWithScore(dirPath);
		}
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 565, 395);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblQuery = new JLabel("Query:");
		lblQuery.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblQuery.setBounds(28, 13, 55, 16);
		contentPane.add(lblQuery);
		
		textField = new JTextField();
		textField.setBounds(106, 11, 431, 22);
		contentPane.add(textField);
		textField.setColumns(10);
		
		JLabel lblQueryMode = new JLabel("Query Mode:");
		lblQueryMode.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblQueryMode.setBounds(11, 42, 98, 16);
		contentPane.add(lblQueryMode);
		
		rdbtnBoolean = new JRadioButton("Boolean");
		rdbtnBoolean.setFont(new Font("Tahoma", Font.PLAIN, 15));
		rdbtnBoolean.setBounds(106, 39, 79, 25);
		contentPane.add(rdbtnBoolean);
		
		rdbtnRanked = new JRadioButton("Ranked");
		rdbtnRanked.setFont(new Font("Tahoma", Font.PLAIN, 15));
		rdbtnRanked.setBounds(194, 39, 85, 25);
		contentPane.add(rdbtnRanked);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(rdbtnRanked);
		buttonGroup.add(rdbtnBoolean);
		
		outputList = new JList<>();
		outputList.setFont(new Font("Tahoma", Font.BOLD, 13));
		outputList.setBounds(12, 71, 643, 200);
		outputList.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				openFileClick(e);
			}
		});
		
		JScrollPane scrollBar = new JScrollPane(outputList);
		scrollBar.setBounds(12, 106, 525, 200);
		contentPane.add(scrollBar);
		
		lblTotal = new JLabel("");
		lblTotal.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblTotal.setBounds(11, 319, 191, 16);
		contentPane.add(lblTotal);
		
		btnSearch = new JButton("Search");
		btnSearch.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnSearch.setBounds(12, 71, 97, 25);
		btnSearch.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(textField.getText().trim().length() > 0 && rdbtnBoolean.isSelected()) {
					System.out.println("Executing Boolean Query...");
					BooleanQueryTask task = new BooleanQueryTask();
					progressBar.setVisible(true);
					task.execute();
				} else if (textField.getText().trim().length() > 0 && rdbtnRanked.isSelected()) {
					System.out.println("Executing Ranked Query...");
					RankedQueryTask task = new RankedQueryTask();
					progressBar.setVisible(true);
					task.execute();
				}
			}
		});
		contentPane.add(btnSearch);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(189, 321, 146, 14);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		contentPane.add(progressBar);
		
		JButton btnStemToken = new JButton("Stem Token");
		btnStemToken.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnStemToken.setBounds(129, 71, 118, 25);
		btnStemToken.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String text = textField.getText().trim();
				DefaultListModel<String> searchModel = new DefaultListModel<>();
				if(text != null && !text.equals("")) {
					String stemmedText = SimpleEngine.applyPorter2Stemmer(text);
					searchModel.addElement(stemmedText);
				}
				outputList.setModel(searchModel);
				lblTotal.setText("");
			}
		});
		contentPane.add(btnStemToken);
		
		JButton btnVocabulary = new JButton("Vocabulary");
		btnVocabulary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] vocab = getVocabulary();
				DefaultListModel<String> vocabModel = new DefaultListModel<>();
				for(String term : vocab) {
					vocabModel.addElement(term);
				}
				outputList.setModel(vocabModel);
				lblTotal.setText("Total: " + vocab.length);
			}
		});
		btnVocabulary.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnVocabulary.setBounds(270, 71, 109, 25);
		contentPane.add(btnVocabulary);
		
		JButton btnIndexDirectory = new JButton("Index Directory");
		btnIndexDirectory.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnIndexDirectory.setBounds(400, 71, 137, 25);
		btnIndexDirectory.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ModeOptionFrame frame = new ModeOptionFrame();
				frame.setVisible(true);
				dispose();
			}
		});
		contentPane.add(btnIndexDirectory);
	}
	
	/**
	 * Background thread to run boolean queries only.
	 * @author vaibhavjain
	 *
	 */
	class BooleanQueryTask extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			String query = textField.getText().toLowerCase().trim();
			if(booleanParser == null) {
				booleanParser = new TieredBooleanQueryParserWithScore(new TieredDiskInvertedIndexWithScore(dirPath));
			}
			ArrayList<DiskPosting> documentsData = booleanParser.parseQuery(query, 0, false);
			int totalCount = 0;
			DefaultListModel<String> search = new DefaultListModel<>();
			if(documentsData == null || documentsData.size() == 0) {
				search.addElement("No Documents found!");
				outputList.setModel(search);
				lblTotal.setText("Total: 0");
				return null;
			}
			for (DiskPosting diskPosting : documentsData) {
				totalCount++;
				search.addElement(filenames[diskPosting.getDocId()]);
			}
			System.out.println("Total number: " + totalCount);
			outputList.setModel(search);
			lblTotal.setText("Total: " + totalCount);
			return null;
		}
		@Override
		protected void done() {
			progressBar.setVisible(false);
			super.done();
		}
	}
	
	/**
	 * Background task to parse Ranked queries only.
	 * @author vaibhavjain
	 *
	 */
	class RankedQueryTask extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			String query = textField.getText().toLowerCase().trim();
			if(rankedParser == null) {
				rankedParser = new TieredRankedQueryParserWithScore(diskIndex);
			}
			PriorityQueue<Map.Entry<Integer, Double>> priorQ = rankedParser.parseQuery(query, 1);
			
			DefaultListModel<String> search = new DefaultListModel<>();
			if(priorQ  == null || priorQ.isEmpty()) {
				search.addElement("No Documents found!");
				outputList.setModel(search);
				return null;
			}
			for (int i = 0; i < 20; i++) {
				if(priorQ.isEmpty())
					break;
				Entry<Integer, Double> entry = priorQ.remove();
				search.addElement(filenames[entry.getKey()] + " - " + entry.getValue());
			}
			outputList.setModel(search);
			return null;
		}
		@Override
		protected void done() {
			lblTotal.setText("");
			progressBar.setVisible(false);
			super.done();
		}
	}
	
	public static String[] getFileNames() {
		return filenames;
	}
	
	/**
	 * Method for opening a selected file in FileDataFrame.
	 * @param evt
	 */
	private void openFileClick(MouseEvent evt) {
		String filenameGarbage = outputList.getSelectedValue().toString();
		String filename = filenameGarbage.split("-")[0].trim();
		if(evt.getClickCount() == 2 && filename.endsWith(".json")) {
			FileDataFrame frame = new FileDataFrame(new File(dirPath +"\\" + filename));
			frame.initData();
			frame.setVisible(true);
		}
	}
	
	private String[] getVocabulary() {
		return diskIndex.getDictionary();
	}
}
