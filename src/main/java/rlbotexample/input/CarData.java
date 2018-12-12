package rlbotexample.input;


import rlbotexample.vector.Vector2;
import rlbotexample.vector.Vector3;

public class CarData {
	
    public final Vector3 position;
    public final Vector3 velocity;
    
    public final CarOrientation orientation;
    public final CarOrientation angularVelocity;
    
    public final double boost;
    
    public final boolean hasWheelContact;
    public final boolean isSupersonic;
    
    public final int team;
    public final float elapsedSeconds;

    public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds){
        this.position = Vector3.fromFlatbuffer(playerInfo.physics().location());
        this.velocity = Vector3.fromFlatbuffer(playerInfo.physics().velocity());
        
        this.orientation = CarOrientation.fromFlatbuffer(playerInfo);
        this.angularVelocity = CarOrientation.fromFlatbufferAngularVelocity(playerInfo);
        
        this.boost = playerInfo.boost();
        this.isSupersonic = playerInfo.isSupersonic();
        this.team = playerInfo.team();
        this.hasWheelContact = playerInfo.hasWheelContact();
        this.elapsedSeconds = elapsedSeconds;
    }
    
    public double forwardMagnitude(){
    	double forwardComponent = Math.cos(orientation.noseVector.flatten().correctionAngle(velocity.flatten()));
    	return velocity.magnitude() * forwardComponent;
    }
    
    public double magnitudeInDirection(Vector2 direction){
    	double component = Math.cos(direction.correctionAngle(velocity.flatten()));
    	return velocity.magnitude() * component;
    }

	@Override
	public String toString() {
		return "[boost=" + (int)boost + ", team=" + team + ", " + position.toString() + "]";
	}
    
}
