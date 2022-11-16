package org.jam.enrollmentfileprocessor.util;

public class EnrollmentFileProcessorConstants {
	public static final String DELIMITER = ",";
	
	public static final int ENTRY_FIELD_COUNT = 5;
	
	public static final int USER_ID_FIELD_INDEX = 0;
	public static final int FIRST_NAME_FIELD_INDEX = 1;
	public static final int LAST_NAME_FIELD_INDEX = 2;
	public static final int VERSION_FIELD_INDEX = 3;
	public static final int INSURANCE_COMP_NAME_FIELD_INDEX = 4;
	
	public static final String DEFAULT_INPUT_FOLDER = "input";
	public static final String DEFAULT_OUTPUT_FOLDER = "output";
	
	public static final String fieldNames[] = 
		new String[]{"user ID", "first name", "last name", "version", "insurance company" };
}
