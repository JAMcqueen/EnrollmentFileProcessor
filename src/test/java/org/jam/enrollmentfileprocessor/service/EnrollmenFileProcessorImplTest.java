package org.jam.enrollmentfileprocessor.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class EnrollmenFileProcessorImplTest {

	private String inputFolder = "testFileInput";
	private String outputFolder = "testFileOutput";
	
	private String basicFile = "testfile1.csv";
	private String basicFileWithHeader = "testfile1WithHeader.csv";
	
	private String malformedContentFile = "fileWithExtraDelimiter.csv";
		
	private EnrollmentFileProcessorService enrollmentFileProcessorService;
	
	@Before
	public void init() {
		for(File file : new File(outputFolder).listFiles()) {
			file.delete();
		}
	}
	
	@Test
	public void testBasicFilePass() {
		enrollmentFileProcessorService = new EnrollmentFileProcessorServiceImpl(
				inputFolder, 
				outputFolder);
		try {
			enrollmentFileProcessorService.processEnrollmentFile(basicFile);
		} catch (Exception e) {
			fail("No errors should be thrown");
		}
		assertTrue(new File(outputFolder).listFiles().length == 2);
	}
	
	@Test
	public void testBasicHeaderFilePass() {
		enrollmentFileProcessorService = new EnrollmentFileProcessorServiceImpl(
				true,
				true,
				true,
				inputFolder, 
				outputFolder,
				",");
		try {
			enrollmentFileProcessorService.processEnrollmentFile(basicFileWithHeader);
		} catch (Exception e) {
			fail("No errors should be thrown");
		}
		assertTrue(new File(outputFolder).listFiles().length == 2);
	}
	
	@Test
	public void testMalformedContentFilePass() {
		enrollmentFileProcessorService = new EnrollmentFileProcessorServiceImpl(
				inputFolder, 
				outputFolder);
		try {
			enrollmentFileProcessorService.processEnrollmentFile(malformedContentFile);
		} catch (Exception e) {
			fail("No errors should be thrown");
		}
		assertTrue(new File(outputFolder).listFiles().length == 1);
	}
	
	@Test
	public void testMalformedContentFileFail() {
		enrollmentFileProcessorService = new EnrollmentFileProcessorServiceImpl(
				true,
				false,
				true,
				inputFolder, 
				outputFolder,
				",");
		try {
			enrollmentFileProcessorService.processEnrollmentFile(malformedContentFile);
			fail("Error should be thrown");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Malformed line entry"));
		}		
	}		
	
}
