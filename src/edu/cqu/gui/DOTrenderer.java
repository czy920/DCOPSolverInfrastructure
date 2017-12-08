/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2014  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<http://frodo2.sourceforge.net/>
 */

package edu.cqu.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/** 
 * Renders and displays DOT code
 * @author Andreas Schaedeli, Thomas Leaute
 * @todo Increase scrolling speed. 
 */
public class DOTrenderer extends JFrame implements ActionListener {

	/**Serial version ID of JFrame*/
	private static final long serialVersionUID = -8149101075827511346L;


	/**String determining name of temporary files*/
	private static final String TEMP_DOT_FILE_NAME = ".temp";

	/**Integer used to create unique file names*/
	private static int TEMP_FILE_ID = 0;

	/**Name of temporary .dot file of this DOTrenderer instance*/
	private String dotFileName;


	/**Image displayed by this DOTrenderer instance*/
	private Image dotImage;

	/**Component displaying the image*/
	private JLabel content;

	/**Currently displayed image; size may be different from original image*/
	private ImageIcon currentDotImage;

	/** The name of the Graphviz executable */
	private String layout;

	/** Lock to synchronize access to xPos and yPos */
	private final static Object posLock = new Object ();

	/** The x position of the next window */
	private static int xPos = 0;

	/** The y position of the next window */
	private static int yPos = 30;


	/**
	 * Constructor of this JFrame instance. Determines size and components of the Frame. Uses the dot layout. 
	 * 
	 * @param title JFrame title
	 * @param dotCode DOT formatted code to be displayed as image
	 */
	public DOTrenderer(String title, String dotCode) {
		
		this (title, dotCode, "dot");
	}

	/**
	 * Constructor of this JFrame instance. Determines size and components of the Frame. 
	 * 
	 * @param title 	JFrame title
	 * @param dotCode 	DOT formatted code to be displayed as image
	 * @param layout 	one of the following: dot, neato, twopi, circo, fdp, sfdp
	 */
	public DOTrenderer(String title, String dotCode, String layout) {
		super(title);
		
		this.layout = layout;
		
		// Display each new window at a slightly different position on the screen so that the newest one cannot hide the previous ones
		synchronized (posLock) {
			this.setLocation(xPos, yPos);
			if(yPos>500){
				xPos=30;
				yPos=30;
			}
			xPos += 30;
			yPos += 30;
		}

		this.setLayout(new BorderLayout());

		//JScrollPane is used as the image is usually larger than the frame size
		JScrollPane center;

		//Creates the frame's button panel
		JPanel south = new JPanel();
		south.setLayout(new BorderLayout());
		JButton exportButton = new JButton("Save as GIF");
		exportButton.addActionListener(this);
		south.add(exportButton, BorderLayout.EAST);

		JPanel zoomPanel = new JPanel();
		zoomPanel.setLayout(new GridLayout(1, 2));
		JButton in = new JButton("+");
		in.addActionListener(this);
		JButton out = new JButton("-");
		out.addActionListener(this);
		zoomPanel.add(in);
		zoomPanel.add(out);
		south.add(zoomPanel, BorderLayout.WEST);


		//Creates a temporary file containing the given dot code
		this.dotFileName = TEMP_DOT_FILE_NAME + increaseFileID() + ".dot";

		//File could not be created; an error message is shown in the Frame
		if(!createDOTFile(dotCode, dotFileName)) {
			content = new JLabel("Could not create temporary DOT formatted file");
			exportButton.setEnabled(false);
			in.setEnabled(false);
			out.setEnabled(false);
			this.setSize(300, 100);
		}

		else {
			currentDotImage = createDOTfigure();

			//Image could not be created; an error message is shown in the Frame
			if(currentDotImage == null) {
				content = new JLabel("Could not create graphic from .dot file");
				exportButton.setEnabled(false);
				in.setEnabled(false);
				out.setEnabled(false);
				this.setSize(300, 100);
			}

			//Image is displayed as a content of the label; Frame size is set according to the size of the image
			else {
				content = new JLabel(currentDotImage);
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				int width = Math.max(300, Math.min(dim.width - 100, this.currentDotImage.getIconWidth() + 50));
				int height = Math.max(100, Math.min(dim.height - 150, this.currentDotImage.getIconHeight() + 100));
				this.setSize(width, height);
			}
		}

		//All components are added to the Frame and the Frame is displayed
		center = new JScrollPane(content);

		this.add(center, BorderLayout.CENTER);
		this.add(south, BorderLayout.SOUTH);

		this.setVisible(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	/**
	 * This method is called whenever a button is pressed. According to which button is concerned, the corresponding action is taken.
	 * 
	 * @param e Action event
	 */
	public void actionPerformed(ActionEvent e) {

		//If the user desires to export the image, he can choose a file directory and name to save it
		if(e.getActionCommand().equals("Save as GIF")) {
			JFileChooser fileChooser = new JFileChooser(".");
			fileChooser.showSaveDialog(this);
			if(fileChooser.getSelectedFile() != null) {
				File saveFile = fileChooser.getSelectedFile();
				if(saveFile.exists()) {
					saveFile.delete();
				}
				copyFile(new File(dotFileName + ".gif"), saveFile);
			}
		}

		//Zoom in
		else if(e.getActionCommand().equals("+")) {
			currentDotImage = zoomFigure(currentDotImage, true);
			content.setIcon(currentDotImage);
		}

		//Zoom out
		else if(e.getActionCommand().equals("-")) {
			currentDotImage = zoomFigure(currentDotImage, false);
			content.setIcon(currentDotImage);
		}
	}

	/**
	 * This method creates a temporary file from a string representing DOT code
	 *  
	 * @param dotCode String representing DOT formatted code
	 * @param fileName Name of temporary file where code should be saved
	 * @return <b>true</b> if creation of file was successful, else <b>false</b>
	 */
	private boolean createDOTFile(String dotCode, String fileName) {
		try {
			PrintWriter writer = new PrintWriter(new File(fileName));
			writer.println(dotCode);
			writer.close();
			new File(fileName).deleteOnExit();	//Assures the temporary file is deleted when JVM exits
		} 
		catch (FileNotFoundException e) {
			System.err.println("Could not create DOT output file with name " + fileName);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * This method calls the layout executable in order to create an image from the DOT code given in a temporary file.
	 * 
	 * @return ImageIcon representing the image created from DOT code
	 */
	private ImageIcon createDOTfigure() {
		dotImage = null;
		ImageIcon dotImageIcon = null;
		int status = -1;
		File input = new File(dotFileName);

		try {
			//Call to the layout executable, passing as arguments the image format (.gif), the path to the source file, and -O indicating that an output file should be generated
			Process p = Runtime.getRuntime().exec(new String[] {this.layout, "-Tgif", input.getAbsolutePath(), "-O"});

			//Reads the layout process' error stream and displays it
			final Scanner err = new Scanner(p.getErrorStream());
			new Thread(new Runnable() {
				public void run() {
					while(err.hasNextLine()) {
						System.err.println(err.nextLine());
					}
				}
			}).start();

			status = p.waitFor();	//0 if creation of image was successful
			status = 0;
		} 
		catch (IOException e) {
			if(e.getMessage().contains("Cannot run program")) {
				System.err.println("Can not run \"" + this.layout + "\". Probably, the program is not installed, or it is not on the search path.");
			}
			else {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {}

		if(status == 0) {
			dotImageIcon = new ImageIcon(dotFileName + ".gif");
			//Image needs to be stored, as scaling should be done from this image in order not to loose too much quality
			dotImage = dotImageIcon.getImage();
			new File(dotFileName + ".gif").deleteOnExit();	//Assures the temporary .gif file will be deleted when JVM exits
		}

		return dotImageIcon;
	}

	/**
	 * 
	 * @param figure Currently displayed image
	 * @param zoomIn <b>true</b> if user wants to zoom in, <b>false</b> if he wants to zoom out
	 * @return Scaled instance of the image
	 */
	private ImageIcon zoomFigure(ImageIcon figure, boolean zoomIn) {
		int width;
		int height;
		
		double factor = 1.25; // the zoom factor (> 1)

		//Zoom in multiplies current dimensions by 1.5
		if(zoomIn) {
			width = (int) (figure.getIconWidth() * factor);
			height = (int) (figure.getIconHeight() * factor);

			//Maximum zoom is size of original image
			if(width > dotImage.getWidth(null)) {
				width = dotImage.getWidth(null);
				height = dotImage.getHeight(null);
			}
		}

		//Zoom out divides current dimensions by 1.5
		else {
			width = (int) (figure.getIconWidth() / factor);
			height = (int) (figure.getIconHeight() / factor);
		}

		//Creates a scaled instance from the original image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.getGraphics().drawImage(dotImage, 0, 0, width, height, null);
		return new ImageIcon(image);
	}

	/**
	 * @return Unique identifier for temporary files
	 */
	private synchronized int increaseFileID() {
		TEMP_FILE_ID++;
		return TEMP_FILE_ID - 1;
	}

	/**
	 * This method is used to copy a file. This is needed, as all the files created are only temporary and will be deleted when JVM exits. Therefore, to
	 * keep them, they need to be copied to files that will not be deleted. For this, the input file is read and the content is written to the destination
	 * file.
	 * 
	 * @param srcFile Source file (temporary)
	 * @param destFile Destination file (durable)
	 */
	private void copyFile(File srcFile, File destFile) {
		try{
			InputStream in = new FileInputStream(srcFile);
			OutputStream out = new FileOutputStream(destFile);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
		catch(Exception e) {
			System.err.println("Could not save file");
			e.printStackTrace();
		}
	}
}
