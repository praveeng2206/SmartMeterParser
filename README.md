# SmartMeterParser


## Description


NEM12 is a file format used to represent meter read data from smart meters.

A sample of a simplified version of the NEM12 file is provided. SmartMeterParser parses the NEM12 file which is a compilation of the meter reading of smart meters.The incoming file is validated for discrepancies and error are logged if any. The approach followed is to ignore the entire NMI record type starting with 200 if there is an issue with either the NMI record type or the containing volumes. This enables the processing to go on for the remainder of the records. The errored records will not be processed and a manual correction in the file will be needed


### SimpleNem12.csv format specifications
* You can assume, for this exercise, that no quotes or extraneous commas appear in the CSV data.
* First item on every line is the RecordType
* RecordType 100 must be the first line in the file
* RecordType 900 must be the last line in the file
* RecordType 200 represents the start of a meter read block.  This record has the following subsequent items (after RecordType).
You can assume each file does not contain more than one RecordType 200 with the same NMI.
  * NMI ("National Metering Identifier") - String ID representing the meter on the site, modelled in `MeterRead.nmi`.  Value should always be 10 chars long.
  * EnergyUnit - Energy unit of the meter reads, modelled in `EnergyUnit` enum type and `MeterRead.energyUnit`.
* RecordType 300 represents the volume of the meter read for a particular date.  This record has the following subsequent items (after RecordType).
  * Date - In the format yyyyMMdd (e.g. 20170102 is 2nd Jan, 2017).  Modelled in `MeterRead.volumes` map key.
  * Volume - Signed number value.  Modelled in `MeterVolume.volume`.
  * Quality - Represents quality of the meter read, whether it's Actual (A) or Estimate (E).  Value should always be A or E.  Modelled in `MeterVolume.quality`

### Questions

* Are the records going to be processed via a batch job?, if yes the project can be included as part of an exisiting job or a new CRON Job scheduler would be needed. It may also be hosted as a spring boot application with a CRON Job scheduler.
* If there are any issues with a 200 type record or the containing 300 type records, does the entire job need to abort by returning an Empty MeterRead Collection?
* Are the errored records needed to be output to a specific file, which may be in an NEM12 format as well for reprocessing
* Do we need to send alerts to any framework in case of errors
* If the job is part of an existing Batch framework how do we decide or Sequencing and pre-requisites of the job.
* Would need a better understanding of how these records are collected from the smart meters and how often are they processed.


### Assumptions
* Each file does not contain more than one RecordType 200 with the same NMI
* Job doesn't need to abort if there are any issues with Record type 200/300. It can keep processing the remainder of the records.


## Environment Details
* Java 1.8
* Eclipse Version: 2021-12 (4.22.0)
* Gradle 6.8


## Execution Steps
* Clone the SmartMeterParser repository to your local
* Import au.com.redenergy.smartmeterreader as a gradle project
* Run au.com.redenergy.smartmeterreader.TestHarness.java file as a java application.
* Validate the values for NMI 6123456789 and NMI 6987654321 in the console.
* Run au.com.redenergy.smartmeterreader.LibraryTest.java as a JUnit Test.
* Validate all the asserted scenarios and console for logging of errored records.