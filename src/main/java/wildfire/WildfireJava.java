package wildfire;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonInterface;
import rlbot.pyinterop.PythonServer;
import wildfire.util.PortReader;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.pitch.Pitch;

public class WildfireJava {
	
	/**
	 * This prevents the server from being launched
	 */
	private final static boolean testGui = false;
	
	public static ArrayList<Wildfire> bots = new ArrayList<Wildfire>();

	private static List<String> arguments;

    public static void main(String[] args){
    	System.out.println("Args: " + Arrays.toString(args));
    	setArguments(args);
    	
    	//Bot Manager
    	BotManager botManager = new BotManager();
    	Integer port = PortReader.readPortFromFile("port.cfg");
    	if(!testGui){
	        PythonInterface pythonInterface = new WildfirePythonInterface(botManager);
	        PythonServer pythonServer = new PythonServer(pythonInterface, port);
	        pythonServer.start();
	        
	        Pitch.initTriangles();
    	}
        
        //Frame
        JFrame frame = new JFrame("Wildfire");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Panel
        JPanel panel = new JPanel();
        final int borderSize = 10;
        panel.setBorder(new EmptyBorder(borderSize, borderSize, borderSize, borderSize));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        //Labels
        panel.add(new JLabel("Wildfire, a Java bot by r0bbi3#0269"));
        panel.add(new JLabel("Port Number: " + port));
        panel.add(new JLabel("Bots Running:"));
        
        //Table
        String column[] = {"Index", "Name", "Team"};
        JTable table = new JTable(new String[][] {}, column);    
        table.setBounds(borderSize, borderSize, borderSize, borderSize);     
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(80, 120));
        panel.add(scrollPane);
        
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        
        //Listener
        ActionListener myListener = e -> {
            Set<Integer> runningBotIndices = botManager.getRunningBotIndices();
            OptionalInt maxIndex = runningBotIndices.stream().mapToInt(k -> k).max();
            
            if(maxIndex.isPresent()){
            	int maxIndexInt = maxIndex.getAsInt();
            	int botListSize = 0;
            	ArrayList<String[]> data = new ArrayList<String[]>(); 
                
                //Iterate through all the bots
                for(int i = 0; i <= maxIndexInt; i++){
                	Wildfire bot = getBotAtIndex(i);
                	if(bot == null || !runningBotIndices.contains(i)) continue;
                	data.add(new String[] {i + "", bot.isTestVersion() ? "WildfireTest" : "Wildfire", bot.team == 0 ? "Blue" : "Orange"});
                    botListSize ++;
                }
                
                //Update the table
                JTable newTable = new JTable(toDataArray(data), column);
                newTable.setRowHeight(newTable.getRowHeight() * 2);
                scrollPane.getViewport().add(newTable);
                scrollPane.setBounds(scrollPane.getX(), scrollPane.getY(), scrollPane.getWidth(), Math.min(newTable.getRowHeight() * (botListSize + 2), scrollPane.getHeight()));
                panel.add(scrollPane);
            }
        };
        new Timer(1000, myListener).start();
    }
    
    private static String[][] toDataArray(ArrayList<String[]> arrayList){
    	String[][] array = new String[arrayList.size()][];
    	for(int i = 0; i < arrayList.size(); i++){
    	    array[i] = arrayList.get(i);
    	}
    	return array;
	}

	private static Wildfire getBotAtIndex(int index){
    	for(Wildfire w : bots){
    		if(w != null && w.playerIndex == index) return w;
    	}
    	return null;
    }

	public static List<String> getArguments(){
		return arguments;
	}

	public static void setArguments(String[] args){
		arguments = Arrays.asList(args);
	}
    
}