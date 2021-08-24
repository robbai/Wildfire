package wildfire.wildfire.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public abstract class Table {

	protected final String fileName;

	protected List<CSVRecord> records;

	public Table(String fileName){
		super();
		this.fileName = fileName;

		InputStream in = Table.class.getClassLoader().getResourceAsStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		CSVParser csvParser;
		try{
			csvParser = new CSVParser(reader,
					CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
			this.records = csvParser.getRecords();
		}catch(IOException e){
			e.printStackTrace();
			this.records = null;
		}
	}

}
