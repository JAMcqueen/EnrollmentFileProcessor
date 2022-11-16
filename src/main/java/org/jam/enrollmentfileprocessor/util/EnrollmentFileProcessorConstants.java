package org.jam.enrollmentfileprocessor.util;

public class EnrollmentFileProcessorConstants {
	public static final String DELIMITER = ",";
	
	public static final int ENTRY_FIELD_COUNT = 5;
	
	public static final int USER_ID_FIELD_INDEX = 0;
	public static final int FIRST_NAME_FIELD_INDEX = 1;
	public static final int LAST_NAME_FIELD_INDEX = 2;
	public static final int VERSION_FIELD_INDEX = 3;
	public static final int INSURANCE_COMP_NAME_FIELD_INDEX = 4;
	
	public static final String DEFAULT_INPUT_FOLDER = "fileInput";
	public static final String DEFAULT_OUTPUT_FOLDER = "fileOutput";
	
	public static final String OUTPUT_FILE_PREFIX = "Enrollment_File";
	
	public static final String FILE_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
}
