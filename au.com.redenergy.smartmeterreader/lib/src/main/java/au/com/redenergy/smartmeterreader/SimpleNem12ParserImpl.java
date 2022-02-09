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
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	static Logger logger = Logger.getLogger("ErrorRecords");
	static FileHandler fhError;

	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		// TODO Auto-generated method stub
		List<MeterRead> meterReadList = new ArrayList<>();
		try {

			fhError = new FileHandler(ParserConstants.ERROR_LOGGING_FILE);
			logger.addHandler(fhError);
			SimpleFormatter formatter = new SimpleFormatter();
			fhError.setFormatter(formatter);
			logger.info("Logging the Error Records in here");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		validateFileBasic(simpleNem12File, meterReadList);

		try {
			List<String> meterEntry = new ArrayList<String>();
			Scanner scanner = new Scanner(simpleNem12File);

			while (scanner.hasNextLine()) {
				meterEntry.add(scanner.nextLine());
			}
			scanner.close();
			validateFileDetailed(simpleNem12File, meterEntry, meterReadList);

			meterEntry.stream().filter(me -> me.startsWith(ParserConstants.FILE_NMI_START_VALUE)
					|| me.startsWith(ParserConstants.FILE_DAILY_VOLUME_VALUE)).forEach(me -> {
						if (me.startsWith(ParserConstants.FILE_NMI_START_VALUE)) {
							validateAndAddMeterRecord(me, simpleNem12File, meterReadList);
						}

						if (me.startsWith(ParserConstants.FILE_DAILY_VOLUME_VALUE)) {

							validateAndAddVolumeToMeterRead(me, simpleNem12File, meterReadList);

						}

					});

			

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return meterReadList;

	}

	private List<MeterRead> validateFileBasic(File simpleNem12File, List<MeterRead> meterReadList) {
		if (null == simpleNem12File || !simpleNem12File.exists()) {
			logger.info(
					"The " + ParserConstants.CSV_FILE + " file doesn't exist at the location. Please check the file at "
							+ simpleNem12File.getAbsolutePath());
			return meterReadList;
		}
		return meterReadList;
	}
	
	private List<MeterRead> validateFileDetailed(File simpleNem12File, List<String> meterEntry,
			List<MeterRead> meterReadList) {

		if (meterEntry.isEmpty()) {
			logger.info("The " + ParserConstants.CSV_FILE + " file is empty. Please check the file at "
					+ simpleNem12File.getAbsolutePath());
			return meterReadList;

		}
		if (!meterEntry.get(0).equals(ParserConstants.FILE_START_VALUE)) {
			logger.info("The " + ParserConstants.CSV_FILE
					+ " file has an invalid file start parameter. Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Value : " + ParserConstants.FILE_START_VALUE
					+ " Actual value : " + meterEntry.get(0));
			return meterReadList;
		}

		if (!meterEntry.get(meterEntry.size() - 1).equals(ParserConstants.FILE_END_VALUE)) {
			logger.info("The " + ParserConstants.CSV_FILE
					+ " file has an invalid file end parameter. Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Value : " + ParserConstants.FILE_END_VALUE
					+ " Actual value : " + meterEntry.get(meterEntry.size() - 1));
			return meterReadList;

		}
		return meterReadList;

	}



	private void validateAndAddVolumeToMeterRead(String nmiVolumeRecordType, File simpleNem12File,
			List<MeterRead> meterReadList) {

		String[] nmiVolumeValues = nmiVolumeRecordType.split(",");
		if (!nmiVolumeValues[0].equals(ParserConstants.FILE_DAILY_VOLUME_VALUE)) {
			logger.info("The " + ParserConstants.CSV_FILE
					+ " file has an invalid file Meter NMI Volume record type. Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Value : " + ParserConstants.FILE_DAILY_VOLUME_VALUE
					+ " Actual value : " + nmiVolumeValues[0]);
		}

		if (!(nmiVolumeValues.length == ParserConstants.FILE_METER_VOLUME_PARAMS)) {

			logger.info("The " + ParserConstants.CSV_FILE + " file has an invalid number of params for record type "
					+ ParserConstants.FILE_DAILY_VOLUME_VALUE + " Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Count : "
					+ ParserConstants.FILE_METER_VOLUME_PARAMS + " Actual Count : " + nmiVolumeValues.length);
		}

		MeterVolume meterVolume = new MeterVolume(new BigDecimal(nmiVolumeValues[2]),
				Quality.valueOf(nmiVolumeValues[3]));

		meterReadList.get(meterReadList.size() - 1).getVolumes()
				.put(LocalDate.parse(nmiVolumeValues[1], DateTimeFormatter.ofPattern("yyyyMMdd")), meterVolume);

	}

	private List<MeterRead> validateAndAddMeterRecord(String nmiRecordType, File simpleNem12File,
			List<MeterRead> meterReadList) {
		String[] nmiValues = nmiRecordType.split(",");
		if (!nmiValues[0].equals(ParserConstants.FILE_NMI_START_VALUE)) {
			logger.info("The " + ParserConstants.CSV_FILE
					+ " file has an invalid file Meter NMI record type. Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Value : " + ParserConstants.FILE_NMI_START_VALUE
					+ " Actual value : " + nmiValues[0]);
			return meterReadList;
		}
		if (!(nmiValues[1].length() == ParserConstants.FILE_METER_NMI_LENGTH)) {

			logger.info("The " + ParserConstants.CSV_FILE + " file has an invalid length of NMI for record type "
					+ ParserConstants.FILE_NMI_START_VALUE + " NMI = " + nmiValues[1] + " Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Length : " + ParserConstants.FILE_METER_NMI_LENGTH
					+ " Actual Length : " + nmiValues[1].length());
			return meterReadList;

		}
		if (!(nmiValues[2].equals(EnergyUnit.KWH.toString()))) {

			logger.info("The " + ParserConstants.CSV_FILE + " file has an invalid Energy Unit of NMI for record type "
					+ ParserConstants.FILE_NMI_START_VALUE + " NMI = " + nmiValues[1] + " Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Value : " + EnergyUnit.KWH.toString()
					+ " Actual Value : " + nmiValues[2]);
			return meterReadList;
		}
		if (!(nmiValues.length == ParserConstants.FILE_METER_READ_PARAMS)) {

			logger.info("The " + ParserConstants.CSV_FILE + " file has an invalid number of params for record type "
					+ ParserConstants.FILE_NMI_START_VALUE + " Please check the file at "
					+ simpleNem12File.getAbsolutePath() + " Expected Count : " + ParserConstants.FILE_METER_READ_PARAMS
					+ " Actual Count : " + nmiValues.length);
			return meterReadList;
		}

		meterReadList.add(new MeterRead(nmiValues[1], EnergyUnit.KWH));
		return meterReadList;
	}

}
