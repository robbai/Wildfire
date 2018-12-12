package rlbotexample.input;

import rlbot.flat.GameTickPacket;

public class DataPacket {

    public final CarData car;
    public final BallData ball;
    public final int team;
    public final int playerIndex;
    
    /**List of all the cars in the game*/ 
    public final CarData[] cars;

    public DataPacket(GameTickPacket request, int playerIndex){
        this.ball = new BallData(request.ball());
        
        //Get all the cars (apart from this bot)
        this.cars = new CarData[request.playersLength()];
        for(int i = 0; i < request.playersLength(); i++){
//            if(i != playerIndex) this.cars[i] = new CarData(request.players(i), request.gameInfo().secondsElapsed());
            this.cars[i] = new CarData(request.players(i), request.gameInfo().secondsElapsed());
        }
        
        //This bot's info
        this.playerIndex = playerIndex;
        rlbot.flat.PlayerInfo myPlayerInfo = request.players(playerIndex);
        this.team = myPlayerInfo.team();
        this.car = new CarData(myPlayerInfo, request.gameInfo().secondsElapsed());
    }
    
}