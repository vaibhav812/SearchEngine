package engine.simple.Milestone1;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Main GUI of the simple search engine. has all functionalities, including selecting a directory to index and search using a boolean query.
 * 
 * This Class has been deprecated in favor of ModeOptionFrame that incorporates Rank Retrieval.
 * @author vaibhavjain
 * @version 1.0
 */

@Deprecated
public class MainFrame extends JFrame{

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField pathTextField;
	private JTextField termSearchTextField;
	private JLabel lblTotalTerms;
	private JList<String> outputList;
	private JPanel searchPanel;
	private long startTime;
	private List<String> filenames;
	private SimpleEngine engine;
	private Path dirPath;

	
	public MainFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 699, 494);
		setTitle("Simple Search Engine");
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblChooseADirectory = new JLabel("Choose a Directory to index:");
		lblChooseADirectory.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblChooseADirectory.setBounds(12, 13, 193, 20);
		contentPane.add(lblChooseADirectory);
		
		pathTextField = new JTextField();
		pathTextField.setEditable(false);
		pathTextField.setBounds(203, 13, 359, 22);
		contentPane.add(pathTextField);
		pathTextField.setColumns(10);
		
		JButton btnChoose = new JButton("Choose");
		btnChoose.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				chooseDirectoryClick();
			}
			
		});
		btnChoose.setBounds(574, 12, 97, 25);
		contentPane.add(btnChoose);
		
		searchPanel = new JPanel();
		searchPanel.setBounds(12, 46, 657, 357);
		searchPanel.setVisible(false);
		contentPane.add(searchPanel);
		searchPanel.setLayout(null);
		
		JLabel lblTermSearch = new JLabel("Enter Text:");
		lblTermSearch.setBounds(51, 13, 107, 19);
		searchPanel.add(lblTermSearch);
		lblTermSearch.setFont(new Font("Tahoma", Font.PLAIN, 15));
		
		termSearchTextField = new JTextField();
		termSearchTextField.setBounds(194, 12, 359, 22);
		searchPanel.add(termSearchTextField);
		termSearchTextField.setColumns(10);
		
		JButton btnSearch = new JButton("Search");
		btnSearch.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnSearch.setBounds(23, 47, 97, 25);
		btnSearch.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SearchTask task = new SearchTask();
				task.execute();
			}
		});
		searchPanel.add(btnSearch);
		
		JButton btnStemToken = new JButton("Stem Token");
		btnStemToken.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String text = termSearchTextField.getText().trim();
				DefaultListModel<String> searchModel = new DefaultListModel<>();
				if(text != null && !text.equals("")) {
					String stemmedText = SimpleEngine.applyPorter2Stemmer(text);
					searchModel.addElement(stemmedText);
				}
				outputList.setModel(searchModel);
				lblTotalTerms.setText("");
			}
		});
		btnStemToken.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnStemToken.setBounds(169, 47, 119, 25);
		searchPanel.add(btnStemToken);
		
		JButton btnIndexDirectory = new JButton("Index Directory");
		btnIndexDirectory.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnIndexDirectory.setBounds(335, 47, 143, 24);
		btnIndexDirectory.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				chooseDirectoryClick();
			}
		});
		searchPanel.add(btnIndexDirectory);
		
		lblTotalTerms = new JLabel("");
		lblTotalTerms.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblTotalTerms.setBounds(12, 318, 156, 26);
		lblTotalTerms.setVisible(true);
		searchPanel.add(lblTotalTerms);
		
		JButton btnVocabulary = new JButton("Vocabulary");
		btnVocabulary.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnVocabulary.setBounds(526, 47, 107, 24);
		btnVocabulary.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				displayVocab();
			}
		});
		searchPanel.add(btnVocabulary);
		
		outputList = new JList<>();
		outputList.setFont(new Font("Arial", Font.BOLD, 15));
		outputList.setBounds(12, 85, 633, 228);
		outputList.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				openFileClick(e);
			}
		});
		JScrollPane scroll = new JScrollPane(outputList);
		scroll.setVerticalScrollBar(scroll.getVerticalScrollBar());
		scroll.setHorizontalScrollBar(scroll.getHorizontalScrollBar());
		scroll.setBounds(12, 90, 633, 215);
		searchPanel.add(scroll);
		
		JButton btnQuit = new JButton("Quit");
		btnQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				exitProgram();
			}
		});
		btnQuit.setBounds(302, 409, 97, 25);
		contentPane.add(btnQuit);
	}
	
	private void exitProgram() {
		System.exit(0);
	}
	
	/**
	 * Opens a file chooser to choose a diectory to index.
	 */
	private void chooseDirectoryClick() {
		JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File("D:\\CSULB\\Sem 3 - Fall 2017\\Search Engine Technology"));
        jFileChooser.setDialogTitle("Select a Directory");
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);      
        jFileChooser.setAcceptAllFileFilterUsed(true);   

        if (jFileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
        	Path dir = jFileChooser.getSelectedFile().toPath().toAbsolutePath();
        	indexDirectory(dir);
        	jFileChooser.setEnabled(false);
        }
        else {
          pathTextField.setText("No File Choosen!");
        }
        termSearchTextField.setText("");
        DefaultListModel<String> defaultModel = new DefaultListModel<>();
        outputList.setModel(defaultModel);
        lblTotalTerms.setText("");
	}
	
	
	private void indexDirectory(Path dir) {
		dirPath = dir;
        pathTextField.setText(dirPath.toString());
        IndexTask indexTask = new IndexTask(dirPath);
        indexTask.execute();
	}
	
	/**
	 * A swingworker class to run indexing in another thread.
	 * @author vaibhavjain
	 *
	 */
	class IndexTask extends SwingWorker<Void, String> {
		
		Path dirPath;
		MainFrame frame;
		int startRange;
		int endRange;
		
		IndexTask(Path dirPath) {
			this.dirPath = dirPath;
		}
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
        	engine = new SimpleEngine();
        	try{
        	startTime = System.currentTimeMillis();
        	filenames = engine.visitAllFiles(dirPath);
        	} catch (Exception e) {
				e.printStackTrace();
			}
            return null;
        }
 
        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            long endTime = System.currentTimeMillis();
            System.out.println("Total time for indexing: " + (endTime - startTime)/1000 + " seconds");
            searchPanel.setVisible(true);
        }
    }
	
	/**
	 * A SwingWorker class for running query searches in another thread.
	 * @author vaibhavjain
	 *
	 */
	class SearchTask extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			QueryParser parser = new QueryParser(engine);
			int totalCount = 0;
			ArrayList<PositionalPosting> documentsData = parser.parseQuery(termSearchTextField.getText().toLowerCase().trim(), 0, false);
			DefaultListModel<String> search = new DefaultListModel<>();
			if(documentsData == null || documentsData.size() == 0) {
				search.addElement("No Documents found!");
				outputList.setModel(search);
				lblTotalTerms.setText("Total: 0");
				return null;
			}
			for (PositionalPosting positionalPosting : documentsData) {
				totalCount++;
				search.addElement(filenames.get(positionalPosting.getDocId()));
			}
			System.out.println("Total number: " + totalCount);
			outputList.setModel(search);
			lblTotalTerms.setText("Total: " + totalCount);
			return null;
		}
		
		@Override
		protected void done() {
			Toolkit.getDefaultToolkit().beep();
			System.out.println("Search done!");
			super.done();
		}
		
	}
	
	/**
	 * Method for opening a selected file in FileDataFrame.
	 * @param evt
	 */
	private void openFileClick(MouseEvent evt) {
		String filename = outputList.getSelectedValue().toString();
		if(evt.getClickCount() == 2 && filename.endsWith(".json")) {
			FileDataFrame frame = new FileDataFrame(new File(dirPath +"\\" + filename));
			frame.initData();
			frame.setVisible(true);
		}
	}
	
	/**
	 * Displays Vocabulary
	 */
	private void displayVocab() {
		String[] allTerms = engine.getDictionary();
		DefaultListModel<String> vocabModel = new DefaultListModel<>(); 
		for (String term : allTerms) {
			vocabModel.addElement(term);
		}
		outputList.setModel(vocabModel);
		lblTotalTerms.setText("Total: "+ allTerms.length);
	}
}
