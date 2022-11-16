package org.jam.enrollmentfileprocessor.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jam.enrollmentfileprocessor.model.EnrollmentFileEntry;
import org.jam.enrollmentfileprocessor.util.EnrollmentFileProcessorConstants;

public class EnrollmentFileProcessorServiceImpl implements EnrollmentFileProcessorService {

	private boolean outputInfo = true;
	private boolean hasHeader = false;
	private boolean stopProcessingOnMalformedEntry = false;
	private String inputFolder; 
	private String outputFolder; 
	
	private Map<String, List<EnrollmentFileEntry>> enrollmentFileEntryListMap;
	
	private Comparator<EnrollmentFileEntry> entryComparator =
			Comparator.comparing(EnrollmentFileEntry::getFirstName)
				.thenComparing(EnrollmentFileEntry::getLastName);

	public EnrollmentFileProcessorServiceImpl() {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		inputFolder = EnrollmentFileProcessorConstants.DEFAULT_INPUT_FOLDER; 
		outputFolder = EnrollmentFileProcessorConstants.DEFAULT_OUTPUT_FOLDER;
	}

	public EnrollmentFileProcessorServiceImpl(boolean outputInfo, boolean hasHeader,
			boolean stopProcessingOnMalformedEntry) {
		enrollmentFileEntryListMap = new HashMap<String, List<EnrollmentFileEntry>>();
		
		inputFolder = EnrollmentFileProcessorConstants.DEFAULT_INPUT_FOLDER; 
		outputFolder = EnrollmentFileProcessorConstants.DEFAULT_OUTPUT_FOLDER;
		
		this.outputInfo = outputInfo;
		this.hasHeader = hasHeader;
		this.stopProcessingOnMalformedEntry = stopProcessingOnMalformedEntry;	
	}

	@Override
	public void processEnrollmentFile(String fileName) throws Exception {
		if (fileName == null) {
			outputInfo("File name is null");
			throw new Exception("File name is null");
		} else if (fileName.length() == 0) {
			outputInfo("File name is empty");
			throw new Exception("File name is empty");
		}

		String filePath = inputFolder + "\\" + fileName;
		File file = new File(filePath);
		if (!file.exists()) {
			outputInfo(String.format("File at path %s does not exist", filePath));
			throw new Exception(String.format("File at path %s does not exist", filePath));
		} else if (file.isDirectory()) {
			outputInfo(String.format("path %s points to directory", filePath));
			throw new Exception(String.format("Path %s points to directory", filePath));
		}

		processEnrollmentFile(file);
	}

	private void processEnrollmentFile(File file) {
		populateEnrollmentFileEntryListMap(file);
		filterDuplicateIds();
		// sort insurance company lists by first name, lastname
		for(List<EnrollmentFileEntry> insuranceCompanyList: enrollmentFileEntryListMap.values()) {
			Collections.sort(insuranceCompanyList, entryComparator);
		}
		String thing = "thing";
		// output file
	}

	private void populateEnrollmentFileEntryListMap(File file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;		
			
			while ((line = br.readLine()) != null) {			
				if (hasHeader) {
					continue;
				}

				EnrollmentFileEntry entry = processEnrollmentFileLine(line);
				addEntryToInsuranceCompanyMapList(entry);
			}			
		} catch (Exception e) {
			outputInfo(String.format("Error in populateEnrollmentFileEntryListMap: %s", e.getMessage()));
		}
	}

	private EnrollmentFileEntry processEnrollmentFileLine(String line) throws Exception {
		String[] lineValues = line.split(EnrollmentFileProcessorConstants.DELIMITER);
		EnrollmentFileEntry enrollmentFileEntry = null;

		try {
			validateFields(lineValues, line);

			enrollmentFileEntry = new EnrollmentFileEntry();
			enrollmentFileEntry.setUserId(
					lineValues[EnrollmentFileProcessorConstants.USER_ID_FIELD_INDEX]);
			enrollmentFileEntry.setFirstName(
					lineValues[EnrollmentFileProcessorConstants.FIRST_NAME_FIELD_INDEX]);
			enrollmentFileEntry.setLastName(
					lineValues[EnrollmentFileProcessorConstants.LAST_NAME_FIELD_INDEX]);
			enrollmentFileEntry.setVersion(Integer.valueOf
					(lineValues[EnrollmentFileProcessorConstants.VERSION_FIELD_INDEX]));
			enrollmentFileEntry.setInsuranceCompany(
					lineValues[EnrollmentFileProcessorConstants.INSURANCE_COMP_NAME_FIELD_INDEX]);
		} catch (Exception e) {
			if (stopProcessingOnMalformedEntry) {
				throw e;
			}
		}

		return enrollmentFileEntry;
	}

	private void validateFields(String[] lineValues, String line) throws Exception {
		// validate # of fields is correct
		if (lineValues.length != EnrollmentFileProcessorConstants.ENTRY_FIELD_COUNT) {
			outputInfo(String.format("Entry %s has different number of expected fields", line));
			outputInfo(String.format("Expected: %d, Actual: %d", EnrollmentFileProcessorConstants.ENTRY_FIELD_COUNT,
					lineValues.length));
			outputInfo(
					String.format("Check entry matches expected format " + 
							"and fields do not contain the delimeter %s",
							EnrollmentFileProcessorConstants.DELIMITER));
			throw new Exception(String.format("Malformed line entry: %s", line));
		}

		// validate fields contain data
		for (int fieldIndex = 0; fieldIndex < lineValues.length; fieldIndex++) {
			if (lineValues[fieldIndex].strip().length() == 0) {
				outputInfo(String.format("Field %d was blank in line %s", fieldIndex, line));
				throw new Exception(String.format("Field %d was blank in line %s", fieldIndex, line));
			}
		}

		// only validating version; other fields can be strings and validating their
		// content
		// is beyond the scope of this example
		try {
			int version = Integer.parseInt(lineValues[EnrollmentFileProcessorConstants.VERSION_FIELD_INDEX]);
			if (version < 0) {
				throw new Exception(String.format(String.format("Negative version field in line %s", line)));
			}
		} catch (NumberFormatException nfe) {
			outputInfo(String.format("version field was not integer in line %s", line));
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
	
	private void filterDuplicateIds() {
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

}
