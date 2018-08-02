package engine.simple.Milestone2;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import engine.simple.Milestone1.SimpleEngine;
import engine.simple.Milestone3.TieredIndexWriter;

public class ModeOptionFrame extends JFrame {

	/**
	 * Main Frame
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField pathTextfield;
	private Path dir;
	private JProgressBar progressBar;
	private JButton btnChoose;
	private JRadioButton rdbtnBuildAnIndex;
	private JRadioButton rdbtnQueryIndex;
	private JButton btnOk;

	/**
	 * Create the frame.
	 */
	public ModeOptionFrame() {
		setTitle("Simple Search Engine");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 609, 194);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblChooseAMode = new JLabel("Choose a mode:");
		lblChooseAMode.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblChooseAMode.setBounds(12, 13, 116, 16);
		contentPane.add(lblChooseAMode);
		
		rdbtnBuildAnIndex = new JRadioButton("Build an Index");
		rdbtnBuildAnIndex.setFont(new Font("Tahoma", Font.PLAIN, 15));
		rdbtnBuildAnIndex.setBounds(136, 9, 127, 25);
		contentPane.add(rdbtnBuildAnIndex);
		
		rdbtnQueryIndex = new JRadioButton("Query Index");
		rdbtnQueryIndex.setFont(new Font("Tahoma", Font.PLAIN, 15));
		rdbtnQueryIndex.setBounds(276, 9, 127, 25);
		contentPane.add(rdbtnQueryIndex);
		
		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(rdbtnQueryIndex);
		btnGroup.add(rdbtnBuildAnIndex);
		
		btnOk = new JButton("OK");
		btnOk.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnOk.setBounds(255, 105, 97, 25);
		btnOk.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(rdbtnBuildAnIndex.isSelected() && dir != null) {
		            btnOk.setVisible(false);
		            progressBar.setVisible(true);
		            rdbtnBuildAnIndex.setEnabled(false);
		            rdbtnQueryIndex.setEnabled(false);
		            btnChoose.setEnabled(false);
		            IndexTask task = new IndexTask();
		            task.execute();
				} else if (rdbtnQueryIndex.isSelected() && dir != null) {
					QueryFrame frame = new QueryFrame(dir.toAbsolutePath().toString());
					//QueryFrameWithScore frame = new QueryFrameWithScore(dir.toAbsolutePath().toString());
					frame.setVisible(true);
					dispose();
				}
			}
		});

		contentPane.add(btnOk);
		
		JLabel lblChooseAnIndex = new JLabel("Choose an index directory:");
		lblChooseAnIndex.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblChooseAnIndex.setBounds(12, 60, 179, 20);
		contentPane.add(lblChooseAnIndex);
		
		pathTextfield = new JTextField();
		pathTextfield.setBounds(188, 60, 294, 22);
		pathTextfield.setEditable(false);
		contentPane.add(pathTextfield);
		pathTextfield.setColumns(10);
		
		btnChoose = new JButton("Choose");
		btnChoose.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnChoose.setBounds(488, 58, 97, 25);
		btnChoose.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				chooseDirectoryClick();
			}
		});
		contentPane.add(btnChoose);	
		
		progressBar = new JProgressBar();
		progressBar.setFont(new Font("Tahoma", Font.PLAIN, 15));
		progressBar.setBounds(226, 105, 157, 27);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		contentPane.add(progressBar);
	}
	
	private void chooseDirectoryClick() {
		JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File("D:\\CSULB\\Sem 3 - Fall 2017\\Search Engine Technology"));
        jFileChooser.setDialogTitle("Select a Directory");
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);      
        jFileChooser.setAcceptAllFileFilterUsed(true);   

        if (jFileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
        	dir = jFileChooser.getSelectedFile().toPath().toAbsolutePath();
        	pathTextfield.setText(dir.toString());
        }
        else {
          pathTextfield.setText("No File Choosen!");
        }
	}
	
	/**
	 * Starts positional indexing in background. After that has ben completed, it writes the index to the file.
	 * @author vaibhavjain
	 *
	 */
	class IndexTask extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
            SimpleEngine engine = new SimpleEngine();
			try {
				engine.visitAllFiles(dir.toAbsolutePath());
			} catch(IOException ioe) {
	            	ioe.printStackTrace();
	        }
			//IndexWriter writer = new IndexWriter(dir.toAbsolutePath().toString());
			TieredIndexWriter tier = new TieredIndexWriter(dir.toAbsolutePath().toString());
			//TieredIndexWriterWithScore tier = new TieredIndexWriterWithScore(dir.toAbsolutePath().toString(), engine.getFileNamesList().size());
			System.out.println("Writing index to file...");
	        //writer.buildIndex(engine.getIndex());
			tier.buildIndex(engine.getIndex());
	        System.out.println("Done! ");
			return null;
		}
		
		@Override
		protected void done() {
			btnChoose.setEnabled(true);
			rdbtnBuildAnIndex.setEnabled(true);
			rdbtnQueryIndex.setEnabled(true);
			progressBar.setVisible(false);
			btnOk.setVisible(true);
			super.done();
		}
	}
}
