package wildfire.wildfire.grabby;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import wildfire.input.DataPacket;
import wildfire.output.ControlsOutput;

public abstract class Grabber {

	protected Grabby grabby;

	private final String fileName;

	private final String[] headers;

	private ArrayList<Object[]> data;

	public Grabber(Grabby grabby, String fileName, String[] headers){
		this.grabby = grabby;
		this.fileName = fileName;
		this.headers = headers;
		this.data = new ArrayList<Object[]>();
	}

	protected void write(){
		BufferedWriter writer;
		try{
			writer = Files.newBufferedWriter(Paths.get(this.fileName));

			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(this.headers));

			for(Object[] record : this.data){
				csvPrinter.printRecord(record);
			}

			csvPrinter.flush();
			csvPrinter.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	protected void addData(Object[] data){
		this.data.add(data);
	}

	public abstract ControlsOutput processInput(DataPacket input);

}
