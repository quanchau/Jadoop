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

import jadoop.util.TextArrayWritable;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configuration.IntegerRanges;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.security.Credentials;


public class MockContext extends Mapper<Text, TextArrayWritable, Text, MapWritable>.Context {
	
	public Text keyFromMapper;
	public MapWritable mapFromMapper;
	private String status;
	
	private static class MockMapper extends Mapper<Text, TextArrayWritable, Text, MapWritable> {
	}
	
	public MockContext() {
		new MockMapper().super();
	}

	public void write(Text arg0, MapWritable arg1) throws IOException, InterruptedException {
		keyFromMapper = arg0;
		mapFromMapper = arg1;
	}
	
	
	// ***********************
	
	@Override
	public InputSplit getInputSplit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TextArrayWritable getCurrentValue() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputCommitter getOutputCommitter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Counter getCounter(Enum<?> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Counter getCounter(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getProgress() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return status;
	}

	@Override
	public TaskAttemptID getTaskAttemptID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStatus(String arg0) {
		// TODO Auto-generated method stub
		status = arg0;
	}

	@Override
	public Path[] getArchiveClassPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getArchiveTimestamps() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI[] getCacheArchives() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI[] getCacheFiles() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Reducer<?, ?, ?, ?>> getCombinerClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RawComparator<?> getCombinerKeyGroupingComparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credentials getCredentials() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path[] getFileClassPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getFileTimestamps() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RawComparator<?> getGroupingComparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends InputFormat<?, ?>> getInputFormatClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getJar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobID getJobID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getJobName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getJobSetupCleanupNeeded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path[] getLocalCacheArchives() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path[] getLocalCacheFiles() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getMapOutputKeyClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getMapOutputValueClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Mapper<?, ?, ?, ?>> getMapperClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxMapAttempts() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxReduceAttempts() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumReduceTasks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Class<? extends OutputFormat<?, ?>> getOutputFormatClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getOutputKeyClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getOutputValueClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Partitioner<?, ?>> getPartitionerClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getProfileEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getProfileParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntegerRanges getProfileTaskRange(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Reducer<?, ?, ?, ?>> getReducerClass()
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RawComparator<?> getSortComparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getSymlink() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getTaskCleanupNeeded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getWorkingDirectory() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void progress() {
		// TODO Auto-generated method stub
		
	}

	
}
