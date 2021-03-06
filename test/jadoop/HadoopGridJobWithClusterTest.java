// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is part of Jadoop
// Copyright (c) 2016 Grant Braught. All rights reserved.
// 
// Jadoop is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published
// by the Free Software Foundation, either version 3 of the License,
// or (at your option) any later version.
// 
// Jadoop is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public
// License along with Jadoop.
// If not, see <http://www.gnu.org/licenses/>.
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

package jadoop;

import static org.junit.Assert.*;
import jadoop.HadoopGridJob;
import jadoop.HadoopGridTask;
import jadoop.HadoopGridTaskRunner;
import jadoop.util.SingleRecordSplitSequenceFileInputFormat;
import jadoop.util.TextArrayWritable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HadoopGridJobWithClusterTest {

	/*
	 * NOTE: Several of the tests in this class rely on the
	 * GridTaskRunnerSampleTask.class and GridTaskRunnerTimeoutTask.class files
	 * in the src/jadoop/tests directory. These classes must be compiled by hand
	 * because they must be compiled as if in the default package to work in the
	 * way they are needed in the tests. If they need to be recompiled just use
	 * javac GridTaskRunner*.java in the src/jadoop/tests directory.
	 */

	private static File baseDir;
	private static Configuration conf;
	private static MiniDFSCluster cluster;
	private static FileSystem fs;

	/*
	 * Add all paths that need to be deleted after the test to this list and
	 * they will be deleted in the teardown method. This is done because the
	 * deleteOnExit only happens when the whole test class is finished.
	 */
	private ArrayList<Path> toDelete;

	private HadoopGridJob hgj;
	private Path hdfsHome;
	private Path tempHDFSWorkingDir;

	private File gtrst;
	private File gtrtotClassFile;
	private File mc;
	private File prg;
	private File jar1;
	private File jar2;
	private File prgJar;

	private File gtrstClassFile;

	@BeforeClass
	public static void startCluster() throws Exception {

		// Get the name of this testfile.
		String testName = Thread.currentThread().getStackTrace()[1].getClassName();
		testName = testName.substring(testName.lastIndexOf('.') + 1);

		conf = new HdfsConfiguration();

		baseDir = new File("./target/hdfs/" + testName).getAbsoluteFile();

		File f = new File("./target");
		removeDirectory(f);

		conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
		MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
		cluster = builder.build();

		fs = FileSystem.get(conf);
	}

	@AfterClass
	public static void shutdownCluster() throws Exception {
		cluster.shutdown();
		fs.close();
		File f = new File("./target");
		removeDirectory(f);
	}

	private static void removeDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}

	@Before
	public void setUp() throws Exception {
		hgj = new HadoopGridJob("job", conf);
		hdfsHome = fs.getHomeDirectory();
		tempHDFSWorkingDir = new Path(hdfsHome + "/" + hgj.getJob().getJobName());

		fs.deleteOnExit(tempHDFSWorkingDir);

		// files used for testing
		gtrst = new File("test/GridTaskRunnerSampleTask.java");
		mc = new File("test/jadoop/MockContext.java");
		prg = new File("test/jadoop/Program.java");
		// jar contains GridTaskRunnerSampleTask.class in default package
		prgJar = new File("test/jadoop/GridTaskRunnerSampleTask.jar");

		// jar contains: GridTaskRunnerSampleTask.java
		jar1 = new File("test/jadoop/jar1.jar");
		// jar contains MockContext.java and Program.java
		jar2 = new File("test/jadoop/jar2.jar");

		gtrstClassFile = new File("bin/GridTaskRunnerSampleTask.class");
		gtrtotClassFile = new File("bin/GridTaskRunnerTimeoutTask.class");

		toDelete = new ArrayList<Path>();
	}

	@After
	public void tearDown() throws Exception {
		for (Path p : toDelete) {
			fs.delete(p, true);
		}
	}

	@Test
	public void testRunJobMakeTempWorkingDir() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "makeTempDir");

		Path jobPath = new Path(hdfsHome + "/" + hgj.getJob().getJobName());
		toDelete.add(jobPath);

		assertFalse("a directory with the job's name should NOT exist on HDFS", fs.exists(jobPath));
		hgj.runJob(true);
		assertTrue("a directory with the the job's name should exist on HDFS", fs.exists(jobPath));
	}

	@Test
	public void testRunJobMakeTempWorkingDirWithConflict() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "makeTempDir");

		// create the conflict.
		Path jobPath = new Path(hdfsHome + "/job");
		fs.mkdirs(jobPath);
		Path job1Path = new Path(hdfsHome + "/job1");

		toDelete.add(jobPath);
		toDelete.add(job1Path);

		assertTrue("a directory named job should exist on the HDFS", fs.exists(jobPath));
		assertFalse("a directory named job1 should NOT exist on the HDFS", fs.exists(job1Path));

		hgj.runJob(true);

		assertTrue("a directory named job1 should exist on the HDFS", fs.exists(job1Path));
	}

	@Test
	public void testRunJobMakeTempWorkingDirSeveralConflicts() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "makeTempDir");

		Path jobPath = new Path(hdfsHome + "/job");
		Path jobPath1 = new Path(hdfsHome + "/job1");
		Path jobPath2 = new Path(hdfsHome + "/job2");
		Path jobPath3 = new Path(hdfsHome + "/job3");

		fs.mkdirs(jobPath);
		fs.mkdirs(jobPath1);
		fs.mkdirs(jobPath2);

		toDelete.add(jobPath);
		toDelete.add(jobPath1);
		toDelete.add(jobPath2);
		toDelete.add(jobPath3);

		assertTrue("a directory named job should exist on the HDFS", fs.exists(jobPath));
		assertTrue("a directory named job1 should exist on the HDFS", fs.exists(jobPath1));
		assertTrue("a directory named job2 should exist on the HDFS", fs.exists(jobPath2));
		assertFalse("a directory named job3 should not exist on the HDFS", fs.exists(jobPath3));

		hgj.runJob(true);

		assertTrue("a directory named job3 should exist on the HDFS", fs.exists(jobPath3));
	}

	@Test
	public void testRunJobCopyLocalFilesToHDFS() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "copyFiles"); // includes makeTempDir

		// post runJob path of the files on the HDFS working directory
		Path gtrstHDFS = new Path(tempHDFSWorkingDir + "/" + gtrst.getName());
		Path mcHDFS = new Path(tempHDFSWorkingDir + "/" + mc.getName());
		Path prgHDFS = new Path(tempHDFSWorkingDir + "/" + prg.getName());

		toDelete.add(tempHDFSWorkingDir);

		assertTrue("the file should exist in the local directory", gtrst.exists());
		assertTrue("the file should exist in the local directory", mc.exists());
		assertTrue("the file should exist in the local directory", prg.exists());

		assertFalse("this file should NOT be on the HDFS directory", fs.exists(gtrstHDFS));
		assertFalse("this file should NOT be on the HDFS directory", fs.exists(mcHDFS));
		assertFalse("this file should NOT be on the HDFS directory", fs.exists(prgHDFS));

		hgj.addFile(gtrst);
		hgj.addFile(mc);
		hgj.addFile(prg);

		hgj.runJob(true);

		assertTrue("this file should be on the HDFS directory", fs.exists(gtrstHDFS));
		assertTrue("this file should be on the HDFS directory", fs.exists(mcHDFS));
		assertTrue("this file should be on the HDFS directory", fs.exists(prgHDFS));
	}

	@Test
	public void testRunJobCopyLocalArhiveToHDFS() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "copyArchives"); // includes makeTempDir

		Path jar1HDFS = new Path(tempHDFSWorkingDir + "/" + jar1.getName());
		Path jar2HDFS = new Path(tempHDFSWorkingDir + "/" + jar2.getName());

		toDelete.add(tempHDFSWorkingDir);

		assertTrue("the jar1 should exist in the local directory", jar1.exists());
		assertTrue("the jar2 should exist in the local directory", jar2.exists());

		assertFalse("the jar1 should NOT exist in the HDFS working directory", fs.exists(jar1HDFS));
		assertFalse("the jar2 should NOT exist in the HDFS working directory", fs.exists(jar2HDFS));

		hgj.addArchive(jar1);
		hgj.addArchive(jar2);

		hgj.runJob(true);

		assertTrue("jar1 should be on the HDFS directory", fs.exists(jar1HDFS));
		assertTrue("jar2 archive should be on the HDFS directory", fs.exists(jar2HDFS));
	}

	@Test
	public void testRunJobCreateInputDirectory() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		invokeTestHelper(hgj, "makeInputDir"); // includes makeTempDir

		Path inputDir = new Path(tempHDFSWorkingDir + "/input");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		assertTrue("Input directory should be in the temporary HDFS working directory", fs.exists(inputDir));

		Path[] paths = FileInputFormat.getInputPaths(hgj.getJob());
		assertEquals("Input directory path not set in job.", inputDir, paths[0]);
	}

	@Test
	public void testRunJobWriteTasksFile() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IllegalStateException, ClassNotFoundException,
			IOException, InterruptedException, URISyntaxException {

		// includes makeTempDir & makeInputDir
		invokeTestHelper(hgj, "writeTasksFile");

		HadoopGridTask hgt = new HadoopGridTask("key1", "java ProgramSample", 1000);

		hgj.addTask(hgt);

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		Path seqFilePath = new Path(tempHDFSWorkingDir + "/input/tasks0.seq");

		assertTrue("the sequence file should be in the input directory", fs.exists(seqFilePath));

		SequenceFile.Reader reader = new SequenceFile.Reader(conf, Reader.file(seqFilePath));

		Text mapperKey = new Text();
		TextArrayWritable mapperVal = new TextArrayWritable();

		String[] valsArr = null;
		reader.next(mapperKey, mapperVal);
		valsArr = mapperVal.toStrings();

		assertEquals("key is incorrect.", "key1", mapperKey.toString());
		assertEquals("Incorrect number of values", 5, valsArr.length);
		assertTrue("flag for capture stdout is incorrect", Boolean.parseBoolean(valsArr[0]));
		assertTrue("flag for capture stderr is incorrect", Boolean.parseBoolean(valsArr[1]));
		assertEquals("timeout is incorrect", 1000, Integer.parseInt(valsArr[2]));
		assertEquals("command is incorrect", "java", valsArr[3]);
		assertEquals("first argument is incorrect.", "ProgramSample", valsArr[4]);

		assertFalse("should be no more tasks", reader.next(mapperKey, mapperVal));

		reader.close();
	}

	@Test
	public void testRunJobWritesTasksFileQuotedParameter() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, IOException, InterruptedException, URISyntaxException {
		// includes makeTempDir & makeInputDir
		invokeTestHelper(hgj, "writeTasksFile");

		HadoopGridTask hgt2 = new HadoopGridTask("key2", "java \"ProgramSample 1 2 3\" 4", false, true, 1000);
		hgj.addTask(hgt2);

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		Path seqFilePath = new Path(tempHDFSWorkingDir + "/input/tasks0.seq");

		assertTrue("the sequence file should be in the input directory", fs.exists(seqFilePath));

		SequenceFile.Reader reader = new SequenceFile.Reader(conf, Reader.file(seqFilePath));

		Text mapperKey = new Text();
		TextArrayWritable mapperVal = new TextArrayWritable();

		String[] valsArr = null;
		reader.next(mapperKey, mapperVal);
		valsArr = mapperVal.toStrings();

		assertEquals("key is incorrect.", "key2", mapperKey.toString());
		assertEquals("Incorrect number of values", 6, valsArr.length);
		assertFalse("flag for capture stdout is incorrect", Boolean.parseBoolean(valsArr[0]));
		assertTrue("flag for capture stderr is incorrect", Boolean.parseBoolean(valsArr[1]));
		assertEquals("timeout is incorrect", 1000, Integer.parseInt(valsArr[2]));
		assertEquals("command is incorrect", "java", valsArr[3]);
		assertEquals("first argument is incorrect.", "ProgramSample 1 2 3", valsArr[4]);
		assertEquals("second argument is incorrect.", "4", valsArr[5]);

		assertFalse("should be no more tasks", reader.next(mapperKey, mapperVal));

		reader.close();
	}

	@Test
	public void testRunJobWritesTasksMultipleTasks() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, IOException, InterruptedException, URISyntaxException {
		// includes makeTempDir & makeInputDir
		invokeTestHelper(hgj, "writeTasksFile");

		for (int i = 1; i <= 100; i++) {
			HadoopGridTask hgt = new HadoopGridTask("key" + i, "cmd" + i + " " + 2 * i, true, false, i * 100);
			hgj.addTask(hgt);
		}

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		for (int i = 0; i < 100; i++) {
			Path seqFilePath = new Path(tempHDFSWorkingDir + "/input/tasks" + i + ".seq");

			assertTrue("the sequence file should be in the input directory", fs.exists(seqFilePath));

			SequenceFile.Reader reader = new SequenceFile.Reader(conf, Reader.file(seqFilePath));

			Text mapperKey = new Text();
			TextArrayWritable mapperVal = new TextArrayWritable();

			String[] valsArr = null;
			reader.next(mapperKey, mapperVal);
			valsArr = mapperVal.toStrings();

			// get int for fields, b.c. values are put into hash map so
			// they don't come out in order.
			int k = Integer.parseInt(valsArr[4]) / 2;

			assertEquals("key is incorrect.", "key" + k, mapperKey.toString());
			assertEquals("Incorrect number of values", 5, valsArr.length);

			assertTrue("flag for capture stdout is incorrect", Boolean.parseBoolean(valsArr[0]));
			assertFalse("flag for capture stderr is incorrect", Boolean.parseBoolean(valsArr[1]));

			assertEquals("timeout is incorrect", k * 100, Integer.parseInt(valsArr[2]));

			assertEquals("command is incorrect", "cmd" + k, valsArr[3]);
			assertEquals("first argument", "" + 2 * k, valsArr[4]);

			assertFalse("should be no more tasks", reader.next(mapperKey, mapperVal));

			reader.close();
		}
	}

	@Test
	public void testRunJobDoesJobConfiguration() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes makeTempDir
		invokeTestHelper(hgj, "configureJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		// Set to null when running from class files and not from jar.
		// assertEquals("Job Jar not set.", HadoopGridTaskRunner.class, hgj
		// .getJob().getJar());
		assertEquals("Mapper class not set.", HadoopGridTaskRunner.class, hgj.getJob().getMapperClass());
		assertEquals("InputFormat class not set.", SingleRecordSplitSequenceFileInputFormat.class,
				hgj.getJob().getInputFormatClass());
		assertEquals("OutputKey class not set.", Text.class, hgj.getJob().getOutputKeyClass());
		assertEquals("OutputValue class not set.", MapWritable.class, hgj.getJob().getOutputValueClass());
		assertEquals("OutputFormat class not set.", SequenceFileOutputFormat.class,
				hgj.getJob().getOutputFormatClass());

		Path outputDir = new Path(tempHDFSWorkingDir.toString() + "/output");
		assertEquals("OutputPath not set.", outputDir, FileOutputFormat.getOutputPath(hgj.getJob()));
	}

	@Test
	public void testRunJobMarksJobAndTasksAsStarted() throws IOException, IllegalStateException, ClassNotFoundException,
			InterruptedException, URISyntaxException {
		HadoopGridTask hgt1 = new HadoopGridTask("key1", "java ProgramSample", 1000);
		HadoopGridTask hgt2 = new HadoopGridTask("key2", "java ProgramSample", 1000);

		hgj.addTask(hgt1);
		hgj.addTask(hgt2);

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		assertTrue("Job not marked as started", hgj.wasStarted());
		assertTrue("Task 1 not marked started", hgt1.wasStarted());
		assertTrue("Task 2 not marked started", hgt2.wasStarted());
	}

	/*
	 * The following tests actually test functionality in the JobMonitorThread
	 * and the processResults method.
	 */

	@Test
	public void testRunJobSubmitAndWaitOneSuccessfulCommand() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt = new HadoopGridTask("key1", "ncal -e 2016", 1000);
		hgj.addTask(hgt);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true); // wait for completion.

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be complete", hgj.hasFinished());
		assertTrue("the job should be successfully completed", hgj.wasSuccessful());
		assertFalse("the job should NOT be terminated", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out", hgj.hasTimedout());

		hgt = hgj.getTask("key1");

		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertTrue("the task should be successfully completed", hgt.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt.getExitValue());
		assertEquals("Standard output is incorrect", "March 27 2016", hgt.getStandardOutput());
		assertEquals("Standard error should be empty", "", hgt.getStandardError());
	}

	@Test
	public void testJobMonitorCleansUpTempDir() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt = new HadoopGridTask("key1", "ncal -e 2016", 1000);
		hgj.addTask(hgt);

		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true);

		assertFalse("The temp working directory should be removed from the HDFS", fs.exists(tempHDFSWorkingDir));
	}

	@Test
	public void testRunJobSubmitAndWaitSeveralSuccessfulCommands() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt1 = new HadoopGridTask("key1", "ncal -e 2016", 1000);
		hgj.addTask(hgt1);
		HadoopGridTask hgt2 = new HadoopGridTask("key2", "ncal -e 2015", 1000);
		hgj.addTask(hgt2);
		HadoopGridTask hgt3 = new HadoopGridTask("key3", "ncal -e 2014", 1000);
		hgj.addTask(hgt3);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true); // wait for completion.

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be complete", hgj.hasFinished());
		assertTrue("the job should be successfully completed", hgj.wasSuccessful());
		assertFalse("the job should NOT be terminated", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out", hgj.hasTimedout());

		hgt1 = hgj.getTask("key1");
		hgt2 = hgj.getTask("key2");
		hgt3 = hgj.getTask("key3");

		assertFalse("the task should NOT be running", hgt1.isRunning());
		assertTrue("the task should be complete", hgt1.hasFinished());
		assertTrue("the task should be successfully completed", hgt1.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt1.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt1.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt1.getExitValue());
		assertEquals("Standard output is incorrect", "March 27 2016", hgt1.getStandardOutput());
		assertEquals("Standard error should be empty", "", hgt1.getStandardError());

		assertFalse("the task should NOT be running", hgt2.isRunning());
		assertTrue("the task should be complete", hgt2.hasFinished());
		assertTrue("the task should be successfully completed", hgt2.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt2.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt2.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt2.getExitValue());
		assertEquals("Standard output is incorrect", "April  5 2015", hgt2.getStandardOutput());
		assertEquals("Standard error should be empty", "", hgt2.getStandardError());

		assertFalse("the task should NOT be running", hgt3.isRunning());
		assertTrue("the task should be complete", hgt3.hasFinished());
		assertTrue("the task should be successfully completed", hgt3.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt3.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt3.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt3.getExitValue());
		assertEquals("Standard output is incorrect", "April 20 2014", hgt3.getStandardOutput());
		assertEquals("Standard error should be empty", "", hgt3.getStandardError());
	}

	@Test
	public void testSubmitJobAndWaitFailedCommand() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt = new HadoopGridTask("key1", "ls -D", 1000);
		hgj.addTask(hgt);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true); // wait for completion.

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be complete", hgj.hasFinished());
		assertFalse("the job should NOT be successfully completed", hgj.wasSuccessful());
		assertFalse("the job should NOT be terminated", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out", hgj.hasTimedout());

		hgt = hgj.getTask("key1");

		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertFalse("the task should NOT be successfully completed", hgt.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be 1", 1, hgt.getExitValue());
		assertEquals("Standard output is incorrect", "", hgt.getStandardOutput());
		assertEquals("Standard error is incorrect",
				"ls: illegal option -- D\n" + "usage: ls [-ABCFGHLOPRSTUWabcdefghiklmnopqrstuwx1] [file ...]",
				hgt.getStandardError());
	}

	@Test
	public void testSubmitJobAndWaitBadCommand() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt = new HadoopGridTask("key1", "unknown command", 1000);
		hgj.addTask(hgt);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(true); // wait for completion.

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be complete", hgj.hasFinished());
		assertFalse("the job should NOT be successfully completed", hgj.wasSuccessful());
		assertFalse("the job should NOT be terminated", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out", hgj.hasTimedout());

		hgt = hgj.getTask("key1");

		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertFalse("the task should NOT be successfully completed", hgt.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be -1", -1, hgt.getExitValue());
		assertEquals("Standard output is incorrect", "", hgt.getStandardOutput());
		assertTrue("Standard error is incorrect", hgt.getStandardError().startsWith("Cannot run program \"unknown\""));
	}

	@Test
	public void testRunJobSubmitDontWaitOneSuccessfulCommand() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException,
			IllegalStateException, ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt = new HadoopGridTask("key1", "ncal -e 2016", 1000);
		hgj.addTask(hgt);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(false); // don't wait for completion.

		// Wait for it to finish running.
		while (hgj.isRunning()) {
			assertTrue("the job should be running", hgj.isRunning());
			assertFalse("the job should NOT be complete", hgj.hasFinished());
			assertFalse("the job should NOT be successfully completed", hgj.wasSuccessful());
			Thread.sleep(1000);
		}

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be complete", hgj.hasFinished());
		assertTrue("the job should be successfully completed", hgj.wasSuccessful());
		assertFalse("the job should NOT be terminated", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out", hgj.hasTimedout());

		hgt = hgj.getTask("key1");

		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertTrue("the task should be successfully completed", hgt.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt.getExitValue());
		assertEquals("Standard output is incorrect", "March 27 2016", hgt.getStandardOutput());
		assertEquals("Standard error should be empty", "", hgt.getStandardError());
	}

	@Test
	public void testJobMonitorTimesOutJob() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt2 = new HadoopGridTask("key2", "sleep 30", 50000);
		hgj.addTask(hgt2);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.setJobTimeout(3000); // 3 sec.
		hgj.runJob(true); // wait.

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertFalse("the job should not be successfull", hgj.wasSuccessful());
		assertTrue("the job should have been terminated.", hgj.wasTerminated());
		assertTrue("the job should have timed out.", hgj.hasTimedout());

		assertFalse("task should not be running.", hgt2.isRunning());
		assertTrue("task should be finished", hgt2.hasFinished());
		assertFalse("task should not be succssful.", hgt2.wasSuccessful());
		assertTrue("task should have timed out", hgt2.hasTimedout());
		assertTrue("task should have been terminated.", hgt2.wasTerminated());
	}

	@Test
	public void testJobMonitorTerminatesJob() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		HadoopGridTask hgt2 = new HadoopGridTask("key2", "sleep 30", 50000);
		hgj.addTask(hgt2);

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.runJob(false); // dont wait.
		Thread.sleep(3000);
		hgj.terminate();

		while (hgj.isRunning()) {
			Thread.sleep(100);
		}

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertFalse("the job should not be successfull", hgj.wasSuccessful());
		assertTrue("the job should have been terminated.", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out.", hgj.hasTimedout());

		assertFalse("task should not be running.", hgt2.isRunning());
		assertTrue("task should be finished", hgt2.hasFinished());
		assertFalse("task should not be succssful.", hgt2.wasSuccessful());
		assertFalse("task should not have timed out", hgt2.hasTimedout());
		assertTrue("task should have been terminated.", hgt2.wasTerminated());
	}

	@Test
	public void testFilesCopiedToMapperWorkingDir() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addFile(gtrst);
		hgj.addFile(prg);

		HadoopGridTask hgt = new HadoopGridTask("key1", "test -e " + gtrst.getName(), 1000);
		HadoopGridTask hgt2 = new HadoopGridTask("key2", "test -e " + prg.getName(), 1000);

		hgj.addTask(hgt);
		hgj.addTask(hgt2);

		hgj.runJob(true);

		assertEquals("the exit value of hgt should be 0", 0, hgj.getTask("key1").getExitValue());
		assertEquals("the exit value of hgt2 should be 0", 0, hgj.getTask("key2").getExitValue());
	}

	@Test
	public void testArchiveCopiedToMapperWorkingDir() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addArchive(jar1);
		hgj.addArchive(jar2);

		HadoopGridTask hgt1 = new HadoopGridTask("key1", "test -d " + jar1.getName(), 1000);
		HadoopGridTask hgt1a = new HadoopGridTask("key1a", "test -e jar1.jar/GridTaskRunnerSampleTask.java", 1000);

		HadoopGridTask hgt2 = new HadoopGridTask("key2", "test -d " + jar2.getName(), 1000);
		HadoopGridTask hgt2a = new HadoopGridTask("key2a", "test -e jar2.jar/MockContext.java", 1000);
		HadoopGridTask hgt2b = new HadoopGridTask("key2b", "test -e jar2.jar/Program.java", 1000);

		hgj.addTask(hgt1);
		hgj.addTask(hgt1a);
		hgj.addTask(hgt2);
		hgj.addTask(hgt2a);
		hgj.addTask(hgt2b);

		hgj.runJob(true);

		assertEquals("the exit value of hgt1 should be 0", 0, hgj.getTask("key1").getExitValue());
		assertEquals("the exit value of hgt1a should be 0", 0, hgj.getTask("key1a").getExitValue());
		assertEquals("the exit value of hgt2 should be 0", 0, hgj.getTask("key2").getExitValue());
		assertEquals("the exit value of hgt2a should be 0", 0, hgj.getTask("key2a").getExitValue());
		assertEquals("the exit value of hgt2b should be 0", 0, hgj.getTask("key2b").getExitValue());
	}

	@Test
	public void testRunProgramAddedAsFile() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addFile(gtrstClassFile);

		HadoopGridTask hgt = new HadoopGridTask("key1", "java GridTaskRunnerSampleTask 0 output error", 1000);

		hgj.addTask(hgt);

		hgj.runJob(true);

		System.err.println(hgt.getStandardError());

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertTrue("the job should be successfull", hgj.wasSuccessful());
		assertFalse("the job should NOT have been terminated.", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out.", hgj.hasTimedout());

		// hgt
		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertTrue("the task should be successfully completed", hgt.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt.getExitValue());
		assertEquals("Standard output is incorrect", "output", hgt.getStandardOutput());
		assertEquals("Standard error is incorrect", "error", hgt.getStandardError());
	}

	@Test
	public void testRunProgramFromAddedJar() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addArchive(prgJar);

		HadoopGridTask hgt1 = new HadoopGridTask("key1",
				"java -cp GridTaskRunnerSampleTask.jar GridTaskRunnerSampleTask 0 output \"does not matter\"", 1000);

		hgj.addTask(hgt1);

		hgj.runJob(true);

		// overall job status of hgj
		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertTrue("the job should be successfull", hgj.wasSuccessful());
		assertFalse("the job should NOT have been terminated.", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out.", hgj.hasTimedout());

		assertFalse("the task should NOT be running", hgt1.isRunning());
		assertTrue("the task should be complete", hgt1.hasFinished());
		assertTrue("the task should be successfully completed", hgt1.wasSuccessful());
		assertFalse("the task should NOT be terminated", hgt1.wasTerminated());
		assertFalse("the task should NOT have timed out", hgt1.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt1.getExitValue());
		assertEquals("Standard output is incorrect", "output", hgt1.getStandardOutput());
		assertEquals("Standard error is incorrect", "does not matter", hgt1.getStandardError());
	}

	@Test
	public void testTaskTimesOut() throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IllegalStateException, ClassNotFoundException,
			InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addFile(gtrtotClassFile);

		HadoopGridTask hgt = new HadoopGridTask("key1", "java GridTaskRunnerTimeoutTask 0 output error", 1000);
		hgj.addTask(hgt);

		hgj.runJob(true);

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertFalse("the job should not be successful", hgj.wasSuccessful());
		assertFalse("the job should NOT have been terminated.", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out.", hgj.hasTimedout());

		// hgt
		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertFalse("the task should not be successfully completed", hgt.wasSuccessful());
		assertTrue("the task should be terminated", hgt.wasTerminated());
		assertTrue("the task should have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be -1", -1, hgt.getExitValue());

		assertEquals("Standard output is incorrect", "output", hgt.getStandardOutput());
		assertEquals("Standard error is incorrect", "error", hgt.getStandardError());
	}

	@Test
	public void testOneTaskTimesOutOneFinishes() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IllegalStateException,
			ClassNotFoundException, InterruptedException, URISyntaxException {

		// includes making all dirs and writing task files.
		invokeTestHelper(hgj, "submitJob");

		toDelete.add(tempHDFSWorkingDir);

		hgj.addFile(gtrstClassFile);
		hgj.addFile(gtrtotClassFile);

		HadoopGridTask hgt = new HadoopGridTask("key1", "java GridTaskRunnerTimeoutTask 0 output error", 1000);
		hgj.addTask(hgt);

		HadoopGridTask hgt2 = new HadoopGridTask("key2", "java GridTaskRunnerSampleTask 0 output error", 1000);
		hgj.addTask(hgt2);

		hgj.runJob(true);

		assertFalse("the job should NOT be running", hgj.isRunning());
		assertTrue("the job should be finished", hgj.hasFinished());
		assertFalse("the job should not be successful", hgj.wasSuccessful());
		assertFalse("the job should NOT have been terminated.", hgj.wasTerminated());
		assertFalse("the job should NOT have timed out.", hgj.hasTimedout());

		// hgt
		assertFalse("the task should NOT be running", hgt.isRunning());
		assertTrue("the task should be complete", hgt.hasFinished());
		assertFalse("the task should not be successfully completed", hgt.wasSuccessful());
		assertTrue("the task should be terminated", hgt.wasTerminated());
		assertTrue("the task should have timed out", hgt.hasTimedout());
		assertEquals("the exit value should be -1", -1, hgt.getExitValue());

		assertEquals("Standard output is incorrect", "output", hgt.getStandardOutput());
		assertEquals("Standard error is incorrect", "error", hgt.getStandardError());

		// hgt2
		assertFalse("the task should NOT be running", hgt2.isRunning());
		assertTrue("the task should be complete", hgt2.hasFinished());
		assertTrue("the task should be successfully completed", hgt2.wasSuccessful());
		assertFalse("the task should not be terminated", hgt2.wasTerminated());
		assertFalse("the task should not have timed out", hgt2.hasTimedout());
		assertEquals("the exit value should be 0", 0, hgt2.getExitValue());

		assertEquals("Standard output is incorrect", "output", hgt2.getStandardOutput());
		assertEquals("Standard error is incorrect", "error", hgt2.getStandardError());
	}

	/*
	 * Below here are static methods for invoking some of the private helper
	 * methods in the HadoopGridJob class to help with testing.
	 */

	static void invokeTestHelper(HadoopGridJob hgj, String method) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Method m = hgj.getClass().getDeclaredMethod(method);
		m.setAccessible(true);
		m.invoke(hgj);
	}
}