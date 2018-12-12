package rlbotexample.wildfire;

import java.awt.Point;

import rlbot.manager.BotLoopRenderer;
import rlbot.render.Renderer;

public class WRenderer extends Renderer {
	
	private Renderer r;
	public boolean twoD, threeD;

	public WRenderer(Wildfire wildfire, boolean twoD, boolean threeD){
		super(wildfire.getIndex());
		this.r = BotLoopRenderer.forBotLoop(wildfire); 
		this.twoD = twoD;
		this.threeD = threeD;
	}
	
	public void drawLine2d(java.awt.Color color, Point start, Point end){
		if(twoD) r.drawLine2d(color, start, end);
    }

    public void drawLine3d(java.awt.Color color, rlbot.vector.Vector3 start, rlbot.vector.Vector3 end){
    	if(threeD) r.drawLine3d(color, start, end);
    }

    public void drawLine2d3d(java.awt.Color color, Point start, rlbot.vector.Vector3 end){
    	if(twoD && threeD) r.drawLine2d3d(color, start, end);
    }

    public void drawRectangle2d(java.awt.Color color, Point upperLeft, int width, int height, boolean filled){
    	if(twoD) r.drawRectangle2d(color, upperLeft, width, height, filled);
    }

    public void drawRectangle3d(java.awt.Color color, rlbot.vector.Vector3 upperLeft, int width, int height, boolean filled){
        if(threeD) r.drawRectangle3d(color, upperLeft, width, height, filled);
    }

    public void drawCenteredRectangle3d(java.awt.Color color, rlbot.vector.Vector3 position, int width, int height, boolean filled){
        if(threeD) r.drawCenteredRectangle3d(color, position, width, height, filled);
    }

    public void drawString2d(String text, java.awt.Color color, Point upperLeft, int scaleX, int scaleY){
    	if(twoD) r.drawString2d(text, color, upperLeft, scaleX, scaleY);
    }

    public void drawString3d(String text, java.awt.Color color, rlbot.vector.Vector3 upperLeft, int scaleX, int scaleY){
    	if(threeD) r.drawString3d(text, color, upperLeft, scaleX, scaleY);
    }

}
