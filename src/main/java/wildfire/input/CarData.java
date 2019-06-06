package wildfire.input;


import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class CarData {
	
    public final Vector3 position;
    public final Vector3 velocity;
    
    public final CarOrientation orientation;
    public final CarOrientation angularVelocity;
    
    public final double boost;

    public final float elapsedSeconds;
    
    public final boolean hasWheelContact;
    public final boolean isSupersonic;
    public final boolean isDemolished;
    public final boolean doubleJumped;
    
    public final int team;
    public final int index;
    
	public final String name;

    public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds, int index){
        this.position = Vector3.fromFlatbuffer(playerInfo.physics().location());
        this.velocity = Vector3.fromFlatbuffer(playerInfo.physics().velocity());
        
        this.orientation = CarOrientation.fromFlatbuffer(playerInfo);
        this.angularVelocity = CarOrientation.fromFlatbufferAngularVelocity(playerInfo);
        
        this.boost = playerInfo.boost();
        
        this.elapsedSeconds = elapsedSeconds;
        
        this.hasWheelContact = playerInfo.hasWheelContact();
        this.isSupersonic = playerInfo.isSupersonic();
        this.isDemolished = playerInfo.isDemolished();
        this.doubleJumped = playerInfo.doubleJumped();
        
        this.team = playerInfo.team();
        this.index = index;
        
        this.name = playerInfo.name();
    }
    
    public double forwardMagnitude(){
    	double forwardComponent = Math.cos(orientation.noseVector.flatten().correctionAngle(velocity.flatten()));
    	return velocity.magnitude() * forwardComponent;
    }
    
    public double magnitudeInDirection(Vector2 direction){
    	double component = Math.cos(direction.correctionAngle(velocity.flatten()));
    	return velocity.magnitude() * component;
    }
    
    public boolean isDrifting(){
    	Vector2 horizontal = this.orientation.rightVector.flatten();
    	double v = Math.cos(this.velocity.flatten().correctionAngle(horizontal)) * this.velocity.magnitude();
    	return v > 300;
    }
    
}
