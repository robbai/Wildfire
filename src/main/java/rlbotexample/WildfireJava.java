package rlbotexample;

import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonInterface;
import rlbot.pyinterop.PythonServer;
import rlbotexample.util.PortReader;

/**
 * See JavaAgent.py for usage instructions
 */
public class WildfireJava {

    public static void main(String[] args){
        BotManager botManager = new BotManager();
        PythonInterface pythonInterface = new WildfirePythonInterface(botManager);
        Integer port = PortReader.readPortFromFile("port.cfg");
        PythonServer pythonServer = new PythonServer(pythonInterface, port);
        pythonServer.start();
    }
    
}