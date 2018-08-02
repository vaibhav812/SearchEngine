package engine.simple.Milestone1;

import java.awt.EventQueue;

import engine.simple.Milestone2.ModeOptionFrame;

/**
 * Main entry point of the program. Starts the main frame.
 * @author vaibhavjain
 * @version 1.1
 */
public class Main {
	
	public static void main(String[] args) throws Exception{
		//FormCorpus.initCorpus(new File("D:\\CSULB\\Sem 3 - Fall 2017\\Search Engine Technology\\all-nps-sites.json"));
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ModeOptionFrame frame = new ModeOptionFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
