package au.com.redenergy.smartmeterreader;

/**
 * Commonly used constants across the project
 * 
 * @author prave
 *
 */
public final class ParserConstants {

	public static final String CSV_FILE = "CSV";
	public static final String FILE_START_VALUE = "100";
	public static final String FILE_END_VALUE = "900";
	public static final String FILE_NMI_START_VALUE = "200";
	public static final String FILE_DAILY_VOLUME_VALUE = "300";
	public static final Integer FILE_METER_READ_PARAMS = 3;
	public static final Integer FILE_METER_VOLUME_PARAMS = 4;
	public static final Integer FILE_METER_NMI_LENGTH = 10;
	public static final String ERROR_LOGGING_FILE = "src/main/java/au/com/redenergy/smartmeterreader/ErrorRecords.log";

	private ParserConstants() {

	}
}
