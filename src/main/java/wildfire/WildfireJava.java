package wildfire;

import java.awt.event.ActionListener;
import java.util.OptionalInt;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonInterface;
import rlbot.pyinterop.PythonServer;
import wildfire.util.PortReader;
public class WildfireJava {

    public static void main(String[] args){
        BotManager botManager = new BotManager();
        PythonInterface pythonInterface = new WildfirePythonInterface(botManager);
        Integer port = PortReader.readPortFromFile("port.cfg");
        PythonServer pythonServer = new PythonServer(pythonInterface, port);
        pythonServer.start();
        
        JFrame frame = new JFrame("Wildfire");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("This is the GUI for Wildfire, keep this open!"));
        panel.add(new JLabel("Listening on port " + port));
        JLabel botsRunning = new JLabel("Bots running: ");
        panel.add(botsRunning);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        ActionListener myListener = e -> {
            Set<Integer> runningBotIndices = botManager.getRunningBotIndices();
            OptionalInt maxIndex = runningBotIndices.stream().mapToInt(k -> k).max();
            String botsStr = "None";
            if (maxIndex.isPresent()) {
                StringBuilder botsStrBuilder = new StringBuilder();
                for (int i = 0; i <= maxIndex.getAsInt(); i++) {
                    botsStrBuilder.append(runningBotIndices.contains(i) ? "(true) " : "(false) ");
                }
                botsStr = botsStrBuilder.toString();
            }
            botsRunning.setText("Bots running: " + botsStr);
        };
        new Timer(1000, myListener).start();
    }
    
}