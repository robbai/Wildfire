package wildfire;

import rlbot.Bot;
import rlbot.manager.BotManager;
import rlbot.pyinterop.DefaultPythonInterface;
import wildfire.wildfire.Wildfire;

public class WildfirePythonInterface extends DefaultPythonInterface {

    public WildfirePythonInterface(BotManager botManager){
        super(botManager);
    }

    protected Bot initBot(int index, String botType, int team){
    	System.out.println("Initialising Wildfire [index = " + index + ", name = '" + botType + "', team = " + team + "]");
        return new Wildfire(index, team, botType.toLowerCase().contains("team"));
    }
    
}
