package au.com.redenergy.smartmeterreader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Class provides the implementation of the SimpleNem12Parser. It parses
 * the NEM12 file which is a compilation of the meter reading of smart meters.
 * 
 * 
 * @author praveeng
 *
 */
public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	static Logger logger = Logger.getLogger("ErrorRecords");

	/**
	 * The incoming file is validated for discrepancies and error are logged if any.
	 * The approach followed is to ignore the entire NMI record type starting with
	 * 200 if there is an issue with either the NMI record type of the containing
	 * volumes. This enables the processing to go on for the remainder of the
	 * records.
	 */
	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		List<MeterRead> meterReadList = new ArrayList<>();

		logger.log(Level.SEVERE, "Logging the Error Records in here");
		AtomicBoolean validate = new AtomicBoolean(true);
		if (!validateFileBasic(simpleNem12File, meterReadList)) {
			return meterReadList;
		}

		try {
			List<String> meterEntry = new ArrayList<String>();
			Scanner scanner = new Scanner(simpleNem12File);

			while (scanner.hasNextLine()) {
				meterEntry.add(scanner.nextLine());
			}
			scanner.close();

			if (!validateFileDetailed(simpleNem12File, meterEntry, meterReadList)) {
				return meterReadList;
			}
			// Filtering out the records starting with 200 and 300
			meterEntry.stream().filter(me -> me.startsWith(ParserConstants.FILE_NMI_START_VALUE)
					|| me.startsWith(ParserConstants.FILE_DAILY_VOLUME_VALUE)).forEach(me -> {
						// Adding each Meter NMI to the collection
						if (me.startsWith(ParserConstants.FILE_NMI_START_VALUE)) {
							String[] nmiValues = me.split(",");
							// resetting the validate flag to cater to the remaining NMI records
							validate.set(true);

							validateAndAddMeterRecord(nmiValues, simpleNem12File, meterReadList, validate);
							// adding 200 records if validations pass
							if (validate.get()) {
								meterReadList.add(new MeterRead(nmiValues[1], EnergyUnit.KWH));
							}

						}
						// Adding the daily meter volume to each NMI record
						if (me.startsWith(ParserConstants.FILE_DAILY_VOLUME_VALUE)) {
							// ignoring the 300 records if validation fails for any 200 record
							if (validate.get()) {
								// same flag used to validate 300 records
								validateAndAddVolumeToMeterRead(me, simpleNem12File, meterReadList, validate);

								// if any 300 record fails remove the last entered 200 record from collection
								// since it will lead to incorrect total volume
								if (!validate.get()) {
									meterReadList.remove(meterReadList.size() - 1);
								}
							}

						}

					});

		} catch (IOException e) {
			e.printStackTrace();
		}

		return meterReadList;

	}

	/**
	 * Validations on incoming file
	 * 
	 * @param simpleNem12File
	 * @param meterReadList
	 * @return
	 */
	private boolean validateFileBasic(File simpleNem12File, List<MeterRead> meterReadList) {
		if (null == simpleNem12File || !simpleNem12File.exists()) {
			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE + " file doesn't exist at the location. Please check the file");
			return false;
		}
		return true;
	}

	/**
	 * Validations included for empty file, invalid start param and invalid end
	 * param of file.
	 * 
	 * @param simpleNem12File
	 * @param meterEntry
	 * @param meterReadList
	 * @return
	 */
	private boolean validateFileDetailed(File simpleNem12File, List<String> meterEntry, List<MeterRead> meterReadList) {

		if (meterEntry.isEmpty()) {
			logger.log(Level.SEVERE, "The " + ParserConstants.CSV_FILE + " file is empty. Please check the file at "
					+ simpleNem12File.getAbsolutePath());
			return false;

		}
		if (!meterEntry.get(0).equals(ParserConstants.FILE_START_VALUE)) {
			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE
							+ " file has an invalid file start parameter. Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Value : "
							+ ParserConstants.FILE_START_VALUE + " Actual value : " + meterEntry.get(0));
			return false;
		}

		if (!meterEntry.get(meterEntry.size() - 1).equals(ParserConstants.FILE_END_VALUE)) {
			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE
							+ " file has an invalid file end parameter. Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Value : " + ParserConstants.FILE_END_VALUE
							+ " Actual value : " + meterEntry.get(meterEntry.size() - 1));
			return false;

		}
		return true;

	}

	/**
	 * Validate the NMI volume 300 record type
	 * 
	 * @param nmiVolumeRecordType
	 * @param simpleNem12File
	 * @param meterReadList
	 * @param validate
	 * @return
	 */
	private AtomicBoolean validateAndAddVolumeToMeterRead(String nmiVolumeRecordType, File simpleNem12File,
			List<MeterRead> meterReadList, AtomicBoolean validate) {

		String[] nmiVolumeValues = nmiVolumeRecordType.split(",");
		if (!nmiVolumeValues[0].equals(ParserConstants.FILE_DAILY_VOLUME_VALUE)) {
			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE
							+ " file has an invalid file Meter NMI Volume record type. Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Value : "
							+ ParserConstants.FILE_DAILY_VOLUME_VALUE + " Actual value : " + nmiVolumeValues[0]);
			validate.set(false);
			return validate;
		}

		if (!(nmiVolumeValues.length == ParserConstants.FILE_METER_VOLUME_PARAMS)) {

			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE + " file has an invalid number of params for record type "
							+ ParserConstants.FILE_DAILY_VOLUME_VALUE + " Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Count : "
							+ ParserConstants.FILE_METER_VOLUME_PARAMS + " Actual Count : " + nmiVolumeValues.length);
			validate.set(false);
			return validate;
		}

		MeterVolume meterVolume = new MeterVolume(new BigDecimal(nmiVolumeValues[2]),
				Quality.valueOf(nmiVolumeValues[3]));
		if (null != meterReadList && meterReadList.size() > 0) {
			meterReadList.get(meterReadList.size() - 1).getVolumes()
					.put(LocalDate.parse(nmiVolumeValues[1], DateTimeFormatter.ofPattern("yyyyMMdd")), meterVolume);

		}
		validate.set(true);
		return validate;

	}

	/**
	 * Validate the NMI record type
	 * 
	 * @param nmiValues
	 * @param simpleNem12File
	 * @param meterReadList
	 * @param validate
	 * @return
	 */
	private AtomicBoolean validateAndAddMeterRecord(String[] nmiValues, File simpleNem12File,
			List<MeterRead> meterReadList, AtomicBoolean validate) {

		if (!nmiValues[0].equals(ParserConstants.FILE_NMI_START_VALUE)) {
			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE
							+ " file has an invalid file Meter NMI record type. Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Value : "
							+ ParserConstants.FILE_NMI_START_VALUE + " Actual value : " + nmiValues[0]);
			validate.set(false);
			return validate;
		}
		if (!(nmiValues[1].length() == ParserConstants.FILE_METER_NMI_LENGTH)) {

			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE + " file has an invalid length of NMI for record type "
							+ ParserConstants.FILE_NMI_START_VALUE + " NMI = " + nmiValues[1]
							+ " Please check the file at " + simpleNem12File.getAbsolutePath() + " Expected Length : "
							+ ParserConstants.FILE_METER_NMI_LENGTH + " Actual Length : " + nmiValues[1].length());
			validate.set(false);
			return validate;

		}
		if (!(nmiValues[2].equals(EnergyUnit.KWH.toString()))) {

			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE + " file has an invalid Energy Unit of NMI for record type "
							+ ParserConstants.FILE_NMI_START_VALUE + " NMI = " + nmiValues[1]
							+ " Please check the file at " + simpleNem12File.getAbsolutePath() + " Expected Value : "
							+ EnergyUnit.KWH.toString() + " Actual Value : " + nmiValues[2]);
			validate.set(false);
			return validate;
		}
		if (!(nmiValues.length == ParserConstants.FILE_METER_READ_PARAMS)) {

			logger.log(Level.SEVERE,
					"The " + ParserConstants.CSV_FILE + " file has an invalid number of params for record type "
							+ ParserConstants.FILE_NMI_START_VALUE + " Please check the file at "
							+ simpleNem12File.getAbsolutePath() + " Expected Count : "
							+ ParserConstants.FILE_METER_READ_PARAMS + " Actual Count : " + nmiValues.length);
			validate.set(false);
			return validate;
		}

		validate.set(true);
		return validate;
	}

}
