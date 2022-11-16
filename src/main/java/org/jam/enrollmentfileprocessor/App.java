package org.jam.enrollmentfileprocessor;

import org.jam.enrollmentfileprocessor.service.EnrollmentFileProcessorService;
import org.jam.enrollmentfileprocessor.service.EnrollmentFileProcessorServiceImpl;

public class App {
	public static void main(String[] args) {
		EnrollmentFileProcessorService processorService = new EnrollmentFileProcessorServiceImpl();
		try {
//			processorService.processEnrollmentFile(
//				"C:\\Users\\jerry_boney\\enrollment-file-processor\\src\\main\\resources\\testfile1.csv");
			processorService.processEnrollmentFile("testfile1.csv");
		} catch (Exception e) {
			System.out.println(String.format("Error in processorService: %s", e.getMessage()));
		}
	}
}
