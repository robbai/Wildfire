package wildfire.input;


import wildfire.vector.Vector2;
import wildfire.vector.Vector3;

public class CarData {
	
    public final Vector3 position;
    public final Vector3 velocity;
    public final Vector3 angularVelocity;
    
    public final CarOrientation orientation;
    
    public final double boost;

    public final float elapsedSeconds;
    
    public final boolean hasWheelContact;
    public final boolean isSupersonic;
    public final boolean isDemolished;
    public final boolean doubleJumped;
    
    public final int team;
    public final int index;
    
	public final String name;
	
	public final double forwardVelocity;

    public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds, int index){
        this.position = Vector3.fromFlatbuffer(playerInfo.physics().location());
        this.velocity = Vector3.fromFlatbuffer(playerInfo.physics().velocity());
        this.angularVelocity = Vector3.fromFlatbuffer(playerInfo.physics().angularVelocity());
        
        this.orientation = CarOrientation.fromFlatbuffer(playerInfo.physics().rotation());
        
        this.boost = playerInfo.boost();
        
        this.elapsedSeconds = elapsedSeconds;
        
        this.hasWheelContact = playerInfo.hasWheelContact();
        this.isSupersonic = playerInfo.isSupersonic();
        this.isDemolished = playerInfo.isDemolished();
        this.doubleJumped = playerInfo.doubleJumped();
        
        this.team = playerInfo.team();
        this.index = index;
        
        this.name = playerInfo.name();
        
        this.forwardVelocity = this.velocity.dotProduct(this.orientation.noseVector);
    }
    
    public double velocityDir(Vector2 direction){
    	return this.velocity.dotProduct(direction.normalized().withZ(0));
    }
    
}
