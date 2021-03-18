package app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.io.BufferedReader;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;

/**
 *
 * @author Henrique
 */
public class Main {

    // JSON config file existent
    private static boolean fileNotExists = false;

    // JSON File located at ".MinimapResizer"
    private static File jsonPointerFile;
    private static final String JSON_FILE_DIRECTORY = System.getProperty("user.home") + "\\.MinimapResizer\\MinimapFileDirectory.json";

    // JSON File that contains the minimap information
    private static File jsonMinimapFile;
    private static String jsonMinimapFileDirectory;

    // Current minimap size and current directory
    private static double currentMinimapSize;
    private static String currentDirectory;

    // Components
    private static JFrame mainFrame;

    private static JPanel mainPanel;

    private static JMenuBar mainMenu;

    private static JMenu settingsMenu;
    private static JMenuItem changeDirectory;
    private static JMenuItem resetConfigs;

    private static JLabel currentMinimapSizeLabel;
    private static JLabel scaleValue;
    private static JLabel lockedFileLabel;

    private static JSlider minimapSlider;

    private static JButton changeMinimapSizeButton;
    private static JButton lockFileButton;
    private static JButton unlockFileButton;

    public static void setfont(FontUIResource f) {
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            setfont(new FontUIResource("Verdana", Font.PLAIN, 16));
        } catch (ClassNotFoundException ex) {
            System.out.println("ClassNotFoundException: " + ex);
        } catch (InstantiationException ex) {
            System.out.println("InstantiationException: " + ex);
        } catch (IllegalAccessException ex) {
            System.out.println("IllegalAccessException: " + ex);
        } catch (UnsupportedLookAndFeelException ex) {
            System.out.println("UnsupportedLookAndFeelException: " + ex);
        }

        init();

        mainFrame = createFrame(345, 235, 25, "Minimap Resizer", JFrame.EXIT_ON_CLOSE);

        TitledBorder tb = BorderFactory.createTitledBorder("Select minimap size");
        tb.setTitleJustification(TitledBorder.CENTER);
        
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        
        mainPanel = new JPanel();

        mainMenu = new JMenuBar();

        settingsMenu = new JMenu("Settings");

        changeDirectory = new JMenuItem("Change directory");
        changeDirectory.addActionListener((e) -> {
            changeDirectory();
        });

        settingsMenu.add(changeDirectory);

        resetConfigs = new JMenuItem("Reset configs");
        resetConfigs.addActionListener((e) -> {
            resetConfigs();
        });
        settingsMenu.add(resetConfigs);
        
        mainMenu.add(settingsMenu);

        scaleValue = new JLabel();
        
        minimapSlider = new JSlider(0, 400);
        minimapSlider.setMajorTickSpacing(100);
        minimapSlider.setMinorTickSpacing(25);
        minimapSlider.setPaintTicks(true);
        
        scaleValue.setText(String.format("Minimap scale: %.2f", (double) minimapSlider.getValue()/100));
        
        minimapSlider.addChangeListener((e) -> {
            scaleValue.setText(String.format("Minimap scale: %.2f", (double) minimapSlider.getValue()/100));
        });

        changeMinimapSizeButton = new JButton("Change size");
        changeMinimapSizeButton.addActionListener((e) -> {
            changeMinimapSize();
        });

        mainPanel.setBorder(tb);
        
        mainPanel.add(minimapSlider);
        mainPanel.add(changeMinimapSizeButton);
        mainPanel.add(scaleValue);

        mainPanel.add(currentMinimapSizeLabel);
        
        JPanel secondaryPanel = new JPanel(gbl);
        
        lockFileButton = new JButton("Lock file");
        lockFileButton.addActionListener((e) -> {
            jsonMinimapFile.setReadOnly();
            lockedFileLabel.setText("File is currently " + ((jsonMinimapFile.canWrite()) ? "unlocked" : "locked"));
        });
        c.gridx = 0;
        c.gridy = 0;
        secondaryPanel.add(lockFileButton, c);
        
        unlockFileButton = new JButton("Unlock file");
        unlockFileButton.addActionListener((e) -> {
            jsonMinimapFile.setWritable(true);
            lockedFileLabel.setText("File is currently " + ((jsonMinimapFile.canWrite()) ? "unlocked" : "locked"));
        });
        c.gridx = 1;
        secondaryPanel.add(unlockFileButton, c);
        
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        secondaryPanel.add(lockedFileLabel, c);
        
        mainPanel.add(secondaryPanel);

        mainFrame.setJMenuBar(mainMenu);
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);

    }

    private static void init() {
        currentMinimapSizeLabel = new JLabel();

        jsonPointerFile = new File(JSON_FILE_DIRECTORY);
        
        File temp = new File(jsonPointerFile.getParent());
        if (!temp.exists()) {
            temp.mkdir();
        }
        
        temp = new File(jsonPointerFile.getPath());
        
        if (!temp.exists() || temp.length() < 1) {
            Directory dir = new Directory("C:\\Riot Games\\League of Legends\\Config", "PersistedSettings.json");
            jsonMinimapFile = new File(dir.getFolder() + "\\" + dir.getFile());
            jsonMinimapFileDirectory = dir.getFolder() + "\\" + dir.getFile();
            try (FileWriter fw = new FileWriter(temp)) {
                temp.createNewFile();
                String json = new GsonBuilder().setPrettyPrinting().create().
                        toJson(dir);
                fw.write(json);
            } catch (IOException ex) {
                System.out.println("IOException: " + ex);
            }
        } else {
            try {
                Directory dir = new Gson().fromJson(new FileReader(temp), Directory.class);
                jsonMinimapFile = new File(dir.getFolder() + "\\" + dir.getFile());
                if (!jsonMinimapFile.exists()) {
                    JOptionPane.showMessageDialog(mainFrame, "Couldn't find the \"PersistedSettings.json\" file.\nPlease select it");
                    fileNotExists = true;
                    while (fileNotExists) {
                        changeDirectory();
                    }
                } else {
                    jsonMinimapFileDirectory = jsonMinimapFile.getPath();
                }
            } catch (FileNotFoundException ex) {
                System.out.println("FileNotFoundException: " + ex);
            }
        }

        jsonMinimapFile = new File(jsonMinimapFileDirectory);
        
        lockedFileLabel = new JLabel("File is currently " + ((jsonMinimapFile.canWrite()) ? "unlocked" : "locked"));
        
        currentMinimapSizeLabel.setText("Current minimap size: " + getCurrentMinimapSize());
    }

    private static void changeMinimapSize() {
        jsonMinimapFile = new File(jsonMinimapFileDirectory);
        String content = "";
        try (FileReader fr = new FileReader(jsonMinimapFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (line != null) {
                content += line + System.getProperty("line.separator");
                line = br.readLine();
            }
            content = content.replaceFirst("\"name\"\\s*?:\\s*?\"MinimapScale\",\\s*?\"value\"\\s*?:\\s*?\"[\\d\\.]*\"",
                    "\"name\": \"MinimapScale\"," + System.getProperty("line.separator")
                    + "                            \"value\": \"" + (double) minimapSlider.getValue() / 100 + "\"");
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        try (FileWriter fw = new FileWriter(jsonMinimapFile)) {
            fw.write(content);
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        currentMinimapSizeLabel.setText("Current minimap size: " + (double) minimapSlider.getValue() / 100);
    }

    private static void changeDirectory() {
        JFileChooser fc = new JFileChooser(jsonMinimapFileDirectory);
        fc.setDialogTitle("Select a \"PersistedSettings.json\" file");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int operation = fc.showOpenDialog(mainFrame);
        if (operation == JFileChooser.APPROVE_OPTION) {
            jsonMinimapFile = fc.getSelectedFile();
            while (!jsonMinimapFile.getName().equals("PersistedSettings.json")) {
                JOptionPane.showMessageDialog(fc, "Please select a PersistedSettings.json file!");
                operation = fc.showOpenDialog(mainFrame);
                jsonMinimapFile = fc.getSelectedFile();
                if (operation == JFileChooser.APPROVE_OPTION && jsonMinimapFile.getName().equals("PersistedSettings.json")) {
                    jsonMinimapFileDirectory = jsonMinimapFile.getPath();
                    Directory dir = new Directory(jsonMinimapFile.getParent(), jsonMinimapFile.getName());
                    String json = new GsonBuilder().setPrettyPrinting().create().toJson(dir);
                    try (FileWriter fw = new FileWriter(jsonPointerFile)) {
                        fw.write(json);
                    } catch (FileNotFoundException ex) {
                        System.out.println("FileNotFoundException: " + ex);
                    } catch (IOException ex) {
                        System.out.println("IOException: " + ex);
                    }
                } else if (operation == JFileChooser.CANCEL_OPTION) {
                    if (fileNotExists) {
                        JOptionPane.showMessageDialog(mainFrame, "Unable to start the application");
                        System.exit(0);
                    }
                    break;
                }
            }
            jsonMinimapFileDirectory = jsonMinimapFile.getPath();
            Directory dir = new Directory(jsonMinimapFile.getParent(), jsonMinimapFile.getName());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(dir);
            try (FileWriter fw = new FileWriter(jsonPointerFile)) {
                fw.write(json);
            } catch (FileNotFoundException ex) {
                System.out.println("FileNotFoundException: " + ex);
            } catch (IOException ex) {
                System.out.println("IOException: " + ex);
            }
            fileNotExists = false;
            currentDirectory = jsonMinimapFileDirectory;
        } else if (operation == JFileChooser.CANCEL_OPTION && fileNotExists) {
            JOptionPane.showMessageDialog(mainFrame, "Unable to start the applicatioin");
            System.exit(0);
        }
    }

    private static double getCurrentMinimapSize() {
        String content = new String();
        String str = new String();
        try (FileReader fr = new FileReader(jsonMinimapFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (line != null) {
                content += line + System.getProperty("line.separator");
                line = br.readLine();
            }
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundExcepetion: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        Pattern mmSizePattern = Pattern.compile("\"name\"\\s*?:\\s*?\"MinimapScale\",\\s*?\"value\"\\s*?:\\s*?\"[\\d\\.]*\"");
        Matcher matcher = mmSizePattern.matcher(content);
        if (matcher.find()) {
            str = matcher.group();
        }
        String[] cleanup = str.split(":");
        for (int i = 0; i < cleanup.length; i++) {
            cleanup[i] = cleanup[i].replaceAll("\\s+", "");
            cleanup[i] = cleanup[i].replaceAll("\"", "");
        }

        return Double.parseDouble(cleanup[cleanup.length - 1]);
    }

    /**
     * @param windowWidth the width for the window
     * @param windowHeight the height for the window
     * @param verticalNudge the vertical nudge for the window(positive values
     * makes the window position lower, negative values makes the window
     * position higher)
     * @param title the title for the window
     * @param closeOperation the default close operation for the window
     * @return a non resizable JFrame object with all the methods parameters set
     */
    private static JFrame createFrame(int windowWidth, int windowHeight, int verticalNudge, String title, int closeOperation) {
        JFrame frame = new JFrame(title);

        frame.setSize(windowWidth, windowHeight);

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension dim = tk.getScreenSize();

        int xPad = (dim.width / 2) - (windowWidth / 2);
        int yPad = (dim.height / 2) - (windowHeight / 2);

        frame.setLocation(xPad, yPad + verticalNudge);

        frame.setDefaultCloseOperation(closeOperation);

        frame.setResizable(false);

        return frame;
    }

    private static void resetConfigs() {
        int operation = JOptionPane.showConfirmDialog(mainFrame, "This will reset this app's configurations, are you sure?"
                , "Warning", JOptionPane.YES_NO_OPTION);
        if (operation == JOptionPane.YES_OPTION) {
            jsonPointerFile = new File(JSON_FILE_DIRECTORY);
            File temp = new File(jsonPointerFile.getParent());
            jsonPointerFile.delete();
            temp.delete();
            JOptionPane.showMessageDialog(mainFrame, "Configurations reset!\nPlease restart the application.");
            System.exit(0);
        }
    }

    private static class Directory {

        private final String folder;
        private final String file;

        public Directory(String folder, String file) {
            this.folder = folder;
            this.file = file;
        }

        public String getFolder() {
            return folder;
        }

        public String getFile() {
            return file;
        }
    }

}
