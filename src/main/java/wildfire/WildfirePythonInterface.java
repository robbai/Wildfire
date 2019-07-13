package wildfire;

import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.SocketServer;
import wildfire.wildfire.Wildfire;

public class WildfirePythonInterface extends SocketServer {

    public WildfirePythonInterface(int port, BotManager botManager){
        super(port, botManager);
    }

    protected Bot initBot(int index, String botType, int team){
    	System.out.println("Initialising Wildfire [index = " + index + ", name = '" + botType + "', team = " + team + "]");
        return new Wildfire(index, team, botType.toLowerCase().contains("test"));
    }
    
    @Override
    public void shutdown(){
    	if(WildfireJava.getArguments().contains("never-shutdown")){
    		System.out.println("Preventing shut down...");
//    		for(int i = 0; i < WildfireJava.bots.size(); i++) this.retireBot(i);
    		return;
    	}
    	super.shutdown();
    }
    
}
