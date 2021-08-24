package wildfire;

import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.SocketServer;
import wildfire.wildfire.Wildfire;
import wildfire.wildfire.grabby.Grabby;

public class PythonInterface extends SocketServer {

	public PythonInterface(int port, BotManager botManager){
		super(port, botManager);
	}

	protected Bot initBot(int index, String botType, int team){
		Bot bot = (botType.toLowerCase().contains("grabby") ? new Grabby(index)
				: new Wildfire(index, team, botType.toLowerCase().contains("test"),
						Main.getArguments().contains("record-packets")));
		System.out.println("Initialising " + bot.getClass().getSimpleName() + " [index = " + index + ", name = '"
				+ botType + "', team = " + team + "]");
		return bot;
	}

	@Override
	public void shutdown(){
		if(Main.getArguments().contains("never-shutdown")){
			System.out.println("Preventing shut down...");
//    		for(int i = 0; i < Main.bots.size(); i++) this.retireBot(i);
			return;
		}
		super.shutdown();
	}

}
