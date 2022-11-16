package org.jam.enrollmentfileprocessor.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jam.enrollmentfileprocessor.model.EnrollmentFileEntry;
import org.jam.enrollmentfileprocessor.util.EnrollmentFileProcessorConstants;

public class EnrollmentFileProcessorServiceImpl implements EnrollmentFileProcessorService {

	private boolean outputInfo = true;
	private boolean hasHeader = false;
	private boolean stopProcessingOnMalformedEntry = false;
	private String inputFolder; 
	private String outputFolder; 
	private String delimiter;
	

	private Map<String, List<EnrollmentFileEntry>> enrollmentFileEntryListMap;
	
	private Comparator<EnrollmentFileEntry> entryComparator =
			Comparator.comparing(EnrollmentFileEntry::getFirstName)
				.thenComparing(EnrollmentFileEntry::getLastName);

	public EnrollmentFileProcessorServiceImpl() {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		inputFolder = EnrollmentFileProcessorConstants.DEFAULT_INPUT_FOLDER; 
		outputFolder = EnrollmentFileProcessorConstants.DEFAULT_OUTPUT_FOLDER;
		
		delimiter = EnrollmentFileProcessorConstants.DELIMITER;
	}

	public EnrollmentFileProcessorServiceImpl(
			String inputFolder,
			String outputFolder) {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		this.inputFolder = inputFolder; 
		this.outputFolder = outputFolder;	
		
		delimiter = EnrollmentFileProcessorConstants.DELIMITER;
	}	
	
	public EnrollmentFileProcessorServiceImpl(
			boolean outputInfo, 
			boolean hasHeader,
			boolean stopProcessingOnMalformedEntry) {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		inputFolder = EnrollmentFileProcessorConstants.DEFAULT_INPUT_FOLDER; 
		outputFolder = EnrollmentFileProcessorConstants.DEFAULT_OUTPUT_FOLDER;
		
		delimiter = EnrollmentFileProcessorConstants.DELIMITER;
		
		this.outputInfo = outputInfo;
		this.hasHeader = hasHeader;
		this.stopProcessingOnMalformedEntry = stopProcessingOnMalformedEntry;	
	}
	
	public EnrollmentFileProcessorServiceImpl(
			boolean outputInfo, 
			boolean hasHeader,
			boolean stopProcessingOnMalformedEntry,
			String inputFolder,
			String outputFolder,
			String delimiter) {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		this.inputFolder = inputFolder; 
		this.outputFolder = outputFolder;
		
		this.delimiter = delimiter;
		
		this.outputInfo = outputInfo;
		this.hasHeader = hasHeader;
		this.stopProcessingOnMalformedEntry = stopProcessingOnMalformedEntry;	
	}	

	@Override
	public void processEnrollmentFile(String fileName) throws Exception {
		if (fileName == null) {
			throw new Exception("File name is null");
		} else if (fileName.length() == 0) {
			throw new Exception("File name is empty");
		}

		String filePath = inputFolder + "\\" + fileName;
		File file = new File(filePath);
		if (!file.exists()) {
			throw new Exception(String.format("File at path %s does not exist", filePath));
		} else if (file.isDirectory()) {
			throw new Exception(String.format("Path %s points to directory", filePath));
		}

		enrollmentFileEntryListMap.clear();
		
		processEnrollmentFile(file);
	}

	private void processEnrollmentFile(File file) throws Exception {				
		// convert file contents into lists of enrollment entries, grouped by insurance company
		populateEnrollmentFileEntryListMap(file);
		
		// only keep highest version of entries for each user id
		filterLowerUserIdVersions();
		
		// sort insurance company lists by first name, last name
		outputInfo("Sorting enrollment entries...");
		for(List<EnrollmentFileEntry> insuranceCompanyList: enrollmentFileEntryListMap.values()) {
			Collections.sort(insuranceCompanyList, entryComparator);
		}
		// write each list to separate csv files
		writeEntryListsToFiles();
	}

	private void populateEnrollmentFileEntryListMap(File file) throws Exception{
		outputInfo("Extracting enrollment entries from input file...");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			int lineCount = 0;
			String line;		
			
			while ((line = br.readLine()) != null) {			
				if (hasHeader && lineCount == 0) {
					lineCount++;
					continue;
				}
				EnrollmentFileEntry enrollmentFileEntry = processEnrollmentFileLine(line);
				if(enrollmentFileEntry != null) {
					addEntryToInsuranceCompanyMapList(enrollmentFileEntry);
				}
				lineCount++;
			}			
		} catch (Exception e) {
			outputInfo(String.format("Error in populateEnrollmentFileEntryListMap: %s", 
				e.getMessage()));
			throw e;
		}
	}

	private EnrollmentFileEntry processEnrollmentFileLine(String line) throws Exception {
		List<String> lineValues = List.of(line.split(delimiter))
			.stream()
			.map(String::strip)
			.collect(Collectors.toList());
		EnrollmentFileEntry enrollmentFileEntry = null;

		try {
			validateFields(lineValues, line);

			enrollmentFileEntry = new EnrollmentFileEntry();
			enrollmentFileEntry.setUserId(
				lineValues.get(EnrollmentFileProcessorConstants.USER_ID_FIELD_INDEX));
			enrollmentFileEntry.setFirstName(
					lineValues.get(EnrollmentFileProcessorConstants.FIRST_NAME_FIELD_INDEX));
			enrollmentFileEntry.setLastName(
					lineValues.get(EnrollmentFileProcessorConstants.LAST_NAME_FIELD_INDEX));
			enrollmentFileEntry.setVersion(Integer.valueOf
				(lineValues.get(EnrollmentFileProcessorConstants.VERSION_FIELD_INDEX)));
			enrollmentFileEntry.setInsuranceCompany(
				lineValues.get(EnrollmentFileProcessorConstants.INSURANCE_COMP_NAME_FIELD_INDEX));
		} catch (Exception e) {
			outputInfo(e.getMessage());
			if (stopProcessingOnMalformedEntry) {
				throw e;
			}
		}

		return enrollmentFileEntry;
	}

	private void validateFields(List<String> lineValues, String line) throws Exception {
		// validate # of fields is correct
		if (lineValues.size() != EnrollmentFileProcessorConstants.ENTRY_FIELD_COUNT) {
			outputInfo(String.format("Entry %s has different number of expected fields", line));
			outputInfo(String.format("Expected: %d, Actual: %d", 
				EnrollmentFileProcessorConstants.ENTRY_FIELD_COUNT,
				lineValues.size()));
			outputInfo(String.format("Check entry matches expected format " + 
				"and fields do not contain the delimeter %s", delimiter));
			throw new Exception(String.format("Malformed line entry: %s", line));
		}

		// validate that fields contain data
		for (int fieldIndex = 0; fieldIndex < lineValues.size(); fieldIndex++) {
			if (lineValues.get(fieldIndex).strip().length() == 0) {
				throw new Exception(String.format("Field %d was blank in line %s", fieldIndex, line));
			}
		}

		// only validating version; other fields can be strings and validating their
		// content is beyond the scope of this example
		try {
			int version = Integer.parseInt(
				lineValues.get(EnrollmentFileProcessorConstants.VERSION_FIELD_INDEX));
			if (version < 0) {
				throw new Exception(String.format("Negative version field in line %s", line));
			}
		} catch (NumberFormatException nfe) {
			throw new Exception(String.format("Version field was not integer in line %s", line));
		}
	}

	private void addEntryToInsuranceCompanyMapList(EnrollmentFileEntry entry) {
		String key = entry.getInsuranceCompany().toLowerCase();
		List<EnrollmentFileEntry> entryList = null;

		if (!enrollmentFileEntryListMap.containsKey(key)) {
			enrollmentFileEntryListMap.put(key, new ArrayList<EnrollmentFileEntry>());
		}

		entryList = enrollmentFileEntryListMap.get(key);
		entryList.add(entry);
	}
	
	private void filterLowerUserIdVersions() {
		outputInfo("Condensing multiple userID entries...");
		Map<String, EnrollmentFileEntry> tempEntryMap = new HashMap<>();
		Map<String, List<EnrollmentFileEntry>> tempEnrollmentFileEntryListMap = 
			new HashMap<String, List<EnrollmentFileEntry>>();
		String currentInsuranceCompany = "";
	
		for(List<EnrollmentFileEntry> insuranceCompanyList: enrollmentFileEntryListMap.values()) {
			currentInsuranceCompany = insuranceCompanyList.get(0).getInsuranceCompany().toLowerCase();
			
			for(EnrollmentFileEntry entry: insuranceCompanyList) {
				if(tempEntryMap.containsKey(entry.getUserId())) {
					EnrollmentFileEntry currentEntry = tempEntryMap.get(entry.getUserId());
					if(entry.getVersion() > currentEntry.getVersion()) {
						tempEntryMap.put(entry.getUserId(), entry);
					}
				} else {
					tempEntryMap.put(entry.getUserId(), entry);
				}
			}
			
			tempEnrollmentFileEntryListMap.put(currentInsuranceCompany, new ArrayList<>(tempEntryMap.values()));		
			tempEntryMap.clear();
		}
		
		enrollmentFileEntryListMap.putAll(tempEnrollmentFileEntryListMap);
	}
	
	
	private void writeEntryListsToFiles() {
		outputInfo("Writing insurance company files...");
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
				EnrollmentFileProcessorConstants.FILE_TIMESTAMP_FORMAT);	
		String fileTimeStamp = dateTimeFormatter.format(LocalDateTime.now());	
		
		for(List<EnrollmentFileEntry> insuranceCompanyList: enrollmentFileEntryListMap.values()) {
			String outputFilePath = outputFolder + "\\" + generateFileName(
					insuranceCompanyList.get(0).getInsuranceCompany(), fileTimeStamp);
			File outputFile = new File(outputFilePath);
			
		    try (PrintWriter printWriter = new PrintWriter(outputFile)) {
		    	insuranceCompanyList.stream()
		        	.map(this::convertEntryToString)
		            .forEach(printWriter::println);
		    } catch (FileNotFoundException e) {
		    	outputInfo(String.format("Output file location %s does not exist", outputFilePath));
			}
		}
	}

	private String generateFileName(String insuranceCompanyName, String fileTimeStamp) {
		StringBuilder sb = new StringBuilder();	
		
		sb.append(EnrollmentFileProcessorConstants.OUTPUT_FILE_PREFIX)
			.append("_")
			.append(insuranceCompanyName.replaceAll("\\s+", "_"))
			.append("_")
			.append(fileTimeStamp)
			.append(".csv");
		
		return sb.toString();
	}
	
	private String convertEntryToString(EnrollmentFileEntry entry) {
		StringBuilder sb = new StringBuilder();	
		
		sb.append(entry.getUserId())
			.append(delimiter)
			.append(entry.getFirstName())
			.append(delimiter)
			.append(entry.getLastName())
			.append(delimiter)
			.append(entry.getVersion())
			.append(delimiter)
			.append(entry.getInsuranceCompany());
		
		return sb.toString();
	}

	private void outputInfo(String info) {
		if (outputInfo) {
			System.out.println(info);
		}
	}
	
	
	public boolean isOutputInfo() {
		return outputInfo;
	}
	public void setOutputInfo(boolean outputInfo) {
		this.outputInfo = outputInfo;
	}

	public boolean isHasHeader() {
		return hasHeader;
	}
	public void setHasHeader(boolean hasHeader) {
		this.hasHeader = hasHeader;
	}

	public boolean isStopProcessingOnMalformedEntry() {
		return stopProcessingOnMalformedEntry;
	}
	public void setStopProcessingOnMalformedEntry(boolean stopProcessingOnMalformedEntry) {
		this.stopProcessingOnMalformedEntry = stopProcessingOnMalformedEntry;
	}	
	
	public String getInputFolder() {
		return inputFolder;
	}
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	public String getOutputFolder() {
		return outputFolder;
	}
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getDelimiter() {
		return delimiter;
	}
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

}
