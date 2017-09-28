package me.itzsomebody.spigot.noncefinder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings("serial")
public class Finder extends JFrame {
	private static String nonceID;
	private static List<String> possiblenonces = new ArrayList<String>() {};
    private static Pattern NONCEID_PATTERN = Pattern.compile("([-0-9]|[0-9])[0-9]{6,8}");
    private JTextField field;
    
    public static void main(String[] args) {
        createGUI();
    }
    
    private static void createGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception ex) {}
                Finder finder = new Finder();
                finder.setTitle("SpigotMC NonceID Finder");
                finder.setResizable(false);
                finder.setSize(440, 100);
                finder.setLocationRelativeTo(null);
                finder.setDefaultCloseOperation(3);
                finder.getContentPane().setLayout(new FlowLayout());
                JLabel label = new JLabel("Select File:");
                finder.field = new JTextField();
                finder.field.setEditable(false);
                finder.field.setColumns(18);
                JButton selectButton = new JButton("Select");
                selectButton.setToolTipText("Select jar file");
                selectButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser chooser = new JFileChooser();
                        if (finder.field.getText() != null && !finder.field.getText().isEmpty()) {
                            chooser.setSelectedFile(new File(finder.field.getText()));
                        }
                        chooser.setMultiSelectionEnabled(false);
                        chooser.setFileSelectionMode(0);
                        int result = chooser.showOpenDialog(finder);
                        if (result == 0) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    finder.field.setText(chooser.getSelectedFile().getAbsolutePath());
                                }
                            });
                        }
                    }
                });
                JButton startButton = new JButton("Start");
                startButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (finder.field.getText() == null || finder.field.getText().isEmpty() || !finder.field.getText().endsWith(".jar")) {
                            JOptionPane.showMessageDialog(null, "You must select a valid jar file!", "Error", 0);
                            return;
                        }
                        try {
                            File input = new File(finder.field.getText());
                            if (!input.getName().endsWith(".jar")) {
                                throw new IllegalArgumentException("File must be a jar.");
                            }
                            if (!input.exists()) {
                                throw new FileNotFoundException("The file " + input.getName() + " doesn't exist.");
                            }
                            process(input);
                            if (Finder.nonceID == null) {
                                JOptionPane.showMessageDialog(null, "Could not find any nonces.", "Done", 1);
                            } else {
                            	JTextArea textArea = new JTextArea(12, 45);
                            	textArea.setText("Nonces found: " + JOptionStringBuilder());
                            	textArea.setLineWrap(false);
                                textArea.setEditable(false);
                            	JScrollPane scrollPane = new JScrollPane(textArea);  
                            	JOptionPane.showMessageDialog(null, scrollPane, "Done", 1);
                            }
                        }
                        catch (Throwable t) {
                            JOptionPane.showMessageDialog(null, t, "Error", 0);
                            t.printStackTrace();
                        }
                        finally {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    finder.field.setText("");
                                }
                            });
                            Finder.nonceID = null;
                            Finder.possiblenonces.clear();
                        }
                    }
                });
                JPanel panel = new JPanel(new FlowLayout());
                panel.add(label);
                panel.add(finder.field);
                panel.add(selectButton);
                JPanel panel2 = new JPanel(new FlowLayout());
                panel2.add(startButton);
                JPanel border = new JPanel(new BorderLayout());
                border.add(panel, "North");
                border.add(panel2, "South");
                finder.getContentPane().add(border);
                finder.setVisible(true);
            }
        });
    }
    
    private static String JOptionStringBuilder () {
    	String returnthis = "\n";
    	for (int i = 0; i < possiblenonces.size(); i++) {
    		returnthis = returnthis + "   " + possiblenonces.get(i) + "\n";
    	}
    	return returnthis;
    }
    
    private static void process(File jarFile) throws Throwable {
        ZipFile zipFile = new ZipFile(jarFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(in);
                        ClassNode classNode = new ClassNode();
                        cr.accept(classNode, 0);
                        findID(classNode);
                    }
                }
            }
        }
        finally {
            zipFile.close();
        }
    }
    
    private static boolean findID(ClassNode classNode) throws Throwable {
    	for (MethodNode methodNode : classNode.methods) {
    		Iterator<AbstractInsnNode> insnIterator = methodNode.instructions.iterator();
	        while (insnIterator.hasNext()) {
	          AbstractInsnNode insnNode = (AbstractInsnNode)insnIterator.next();
	          String str;
	          if ((insnNode.getType() == 9)) {
	        	str = ((LdcInsnNode)insnNode).cst.toString();
	            Matcher matcher = NONCEID_PATTERN.matcher(str);
	            if (matcher.find()) {
	              possiblenonces.add(str);
	              nonceID = "Whoo";
	              return true;
	            }
	          }
	        }
    	}
        return false;
    }
}