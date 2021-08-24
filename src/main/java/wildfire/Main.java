package wildfire;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import rlbot.manager.BotManager;
import wildfire.util.PortReader;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.pitch.Pitch;

public class Main {

	/**
	 * This determines whether the server will be launched.
	 */
	private static final boolean LAUNCH = true;

	private static final Integer DEFAULT_PORT = 25966;

	public static ArrayList<Wildfire> bots = new ArrayList<Wildfire>();

	private static List<String> arguments;

	public static void main(String[] args){
		System.out.println("Args: " + Arrays.toString(args));
		setArguments(args);

		// Bot manager.
		BotManager botManager = new BotManager();
		botManager.setRefreshRate(120);
		Integer port = PortReader.readPortFromArgs(args).orElseGet(() -> {
			System.out.println("Could not read port from args, using default!");
			return DEFAULT_PORT;
		});
		if(LAUNCH){
			PythonInterface pythonInterface = new PythonInterface(port, botManager);
			new Thread(pythonInterface::start).start();

			// Initialise static memory.
			Pitch.initTriangles();
		}

		// Frame.
		JFrame frame = new JFrame("Wildfire");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Panel.
		JPanel panel = new JPanel();
		final int borderSize = 15;
		panel.setBorder(new EmptyBorder(borderSize, borderSize, borderSize, borderSize));
		panel.setLayout(new BorderLayout());

		// Icon
		URL url = Main.class.getClassLoader().getResource("icon.png");
		Image image = Toolkit.getDefaultToolkit().createImage(url);
		frame.setIconImage(image);

		// Labels.
		JPanel dataPanel = new JPanel();
		dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
		dataPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		dataPanel.add(new JLabel(new ImageIcon(image.getScaledInstance(140, 140, 100))), BorderLayout.CENTER);
		dataPanel.add(new JLabel("Wildfire, by r0bbi3#0269"));
		dataPanel.add(new JLabel("Port Number: " + port));
		panel.add(dataPanel, BorderLayout.WEST);

		// Table.
		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.setBounds(borderSize, borderSize, borderSize, borderSize);
		tablePanel.add(new JLabel("Bots Running:"), BorderLayout.CENTER);
		final String columns[] = { "Index", "Name", "Team" };
		JTable table = new JTable(new String[][] {}, columns);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(200, table.getHeight()));
		tablePanel.add(scrollPane);
		panel.add(tablePanel);

		frame.add(panel);
		frame.pack();
		frame.setVisible(true);

		// Listener.
		ActionListener myListener = e -> {
			Set<Integer> runningBotIndices = botManager.getRunningBotIndices();
			OptionalInt maxIndex = runningBotIndices.stream().mapToInt(k -> k).max();

			if(maxIndex.isPresent()){
				int maxIndexInt = maxIndex.getAsInt();
				int botListSize = 0;
				ArrayList<String[]> data = new ArrayList<String[]>();

				// Iterate through all the bots.
				for(int i = 0; i <= maxIndexInt; i++){
					Wildfire bot = getBotAtIndex(i);
					if(bot == null || !runningBotIndices.contains(i))
						continue;
					data.add(new String[] { i + "", bot.isTestVersion() ? "WildfireTest" : "Wildfire",
							bot.team == 0 ? "Blue" : "Orange" });
					botListSize++;
				}

				// Update the table.
				JTable newTable = new JTable(toDataArray(data), columns);
				newTable.setRowHeight(newTable.getRowHeight() * 2);
				scrollPane.setBounds(scrollPane.getX(), scrollPane.getY(), scrollPane.getWidth(),
						Math.max(newTable.getRowHeight() * (botListSize + 2), scrollPane.getHeight()));
				scrollPane.getViewport().add(newTable);
				tablePanel.add(scrollPane);
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
			if(w != null && w.playerIndex == index)
				return w;
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
