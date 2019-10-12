package wildfire.input;

import rlbot.flat.GameInfo;
import rlbot.flat.GameTickPacket;
import wildfire.input.ball.BallData;
import wildfire.input.car.CarData;

public class DataPacket {
	
	/**List of all the cars in the game*/ 
    public final CarData[] cars;

	public final GameInfo gameInfo;
    public final CarData car;
    public final BallData ball;
    
    public final int playerIndex;
    public final float elapsedSeconds;

    public DataPacket(GameTickPacket request, int playerIndex){
    	this.gameInfo = request.gameInfo();
        this.ball = new BallData(request.ball());
        this.elapsedSeconds = request.gameInfo().secondsElapsed(); //I could totally remove this, but eh
        
        //Get all the cars
        this.cars = new CarData[request.playersLength()];
        for(int i = 0; i < request.playersLength(); i++){
            this.cars[i] = new CarData(request.players(i), elapsedSeconds, i);
        }
        
        //This bot's info
        this.playerIndex = playerIndex;
        this.car = new CarData(request.players(playerIndex), elapsedSeconds, playerIndex);
    }
    
}