package wildfire.wildfire.training;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Random;

import wildfire.Main;

public class TrainingManager {

	private static final String TRAINING_FOLDER = "./src/main/training/", EXERCISES_FOLDER = (TRAINING_FOLDER + "exercises/"), TEMPLATE_FILE = "/template_training.txt";
	
	private static final String template = readTemplate();
	
	private static final Random random = new Random();
	
	public static void write(TrainingState trainingState){
		// Create the directories.
//		File directory = new File(TRAINING_FOLDER);
//		if(!directory.exists()) directory.mkdir();
		File directory = new File(EXERCISES_FOLDER);
		if(!directory.exists()) directory.mkdir();
		
		Object[] format = getFormat(trainingState);

		Path path = Paths.get(directory.getPath() + "/" + format[0] + ".py");
		System.out.println("Saving to: " + path.toAbsolutePath());
//		System.out.println(Arrays.toString(format));
		
		try{
			// Write the training file.
			String[] lines = MessageFormat.format(template, format).split("\\r?\\n");
			Files.write(path, Arrays.asList(lines), StandardCharsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private static String readTemplate(){
		try{
			URL url = Main.class.getClass().getResource(TEMPLATE_FILE);
			Path path = Paths.get(url.toURI());
			return new String(Files.readAllBytes(path));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private static Object[] getFormat(TrainingState trainingState){
		Object[] format = new Object[43];
		format[0] = randomString(16);
		format[1] = trainingState.ballLocation.x();
		format[2] = trainingState.ballLocation.y();
		format[3] = trainingState.ballLocation.z();
		format[4] = trainingState.ballVelocity.x();
		format[5] = trainingState.ballVelocity.y();
		format[6] = trainingState.ballVelocity.z();
		format[7] = trainingState.ballAngularVelocity.x();
		format[8] = trainingState.ballAngularVelocity.y();
		format[9] = trainingState.ballAngularVelocity.z();
		format[10] = trainingState.ballRotation.pitch();
		format[11] = trainingState.ballRotation.yaw();
		format[12] = trainingState.ballRotation.roll();
		format[13] = trainingState.playerLocation.x();
		format[14] = trainingState.playerLocation.y();
		format[15] = trainingState.playerLocation.z();
		format[16] = trainingState.playerVelocity.x();
		format[17] = trainingState.playerVelocity.y();
		format[18] = trainingState.playerVelocity.z();
		format[19] = trainingState.playerAngularVelocity.x();
		format[20] = trainingState.playerAngularVelocity.y();
		format[21] = trainingState.playerAngularVelocity.z();
		format[22] = trainingState.playerRotation.pitch();
		format[23] = trainingState.playerRotation.yaw();
		format[24] = trainingState.playerRotation.roll();
		format[25] = trainingState.playerJumped;
		format[26] = trainingState.playerDoubleJumped;
		format[27] = trainingState.playerBoost;
		format[28] = trainingState.opponentLocation.x();
		format[29] = trainingState.opponentLocation.y();
		format[30] = trainingState.opponentLocation.z();
		format[31] = trainingState.opponentVelocity.x();
		format[32] = trainingState.opponentVelocity.y();
		format[33] = trainingState.opponentVelocity.z();
		format[34] = trainingState.opponentAngularVelocity.x();
		format[35] = trainingState.opponentAngularVelocity.y();
		format[36] = trainingState.opponentAngularVelocity.z();
		format[37] = trainingState.opponentRotation.pitch();
		format[38] = trainingState.opponentRotation.yaw();
		format[39] = trainingState.opponentRotation.roll();
		format[40] = trainingState.opponentJumped;
		format[41] = trainingState.opponentDoubleJumped;
		format[42] = trainingState.opponentBoost;
		for(int i = 0; i < format.length; i++){
			format[i] = String.valueOf(format[i]).replace("true", "True").replace("false", "False");
		}
		return format;
	}

	private static String randomString(int letters){
		String str = "";
		for(int i = 0; i < letters; i++){
			char c = (char)(random.nextInt(26) + 'a');
			str += c;
		}
		return str;
	}

}
