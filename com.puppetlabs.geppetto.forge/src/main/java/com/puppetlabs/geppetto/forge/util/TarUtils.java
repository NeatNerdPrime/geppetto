/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.forge.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import com.puppetlabs.geppetto.common.os.FileUtils;
import com.puppetlabs.geppetto.common.os.OsUtil;
import com.puppetlabs.geppetto.common.os.StreamUtil;

public class TarUtils {
	/**
	 * An interface that can be used when individual files are to be extracted from an archive, possibly
	 * without storing them to disk.
	 */
	public interface FileCatcher {
		/**
		 * Implementors should return <tt>true</tt> or <tt>false</tt> to indicate if this
		 * file is of interest or not.
		 *
		 * @param fileName
		 *            The name of the file as it occurs in the archive.
		 * @return a flag indicating if the file is accepted
		 */
		boolean accept(String fileName);

		/**
		 * This method will be called for files that accepted
		 *
		 * @param fileName
		 *            The name of the accepted file
		 * @param fileData
		 *            A stream from which the file data can be read.
		 * @return <tt>false</tt> to indicate that processing should continue or <tt>true</tt> to indicate that further
		 *         processing is of no interest
		 *         (i.e. the read terminates here).
		 */
		boolean catchData(String fileName, InputStream fileData) throws IOException;
	}

	private static void append(File file, FileFilter filter, int baseNameLen, String addedTopFolder,
			TarArchiveOutputStream tarOut) throws IOException {

		String name = file.getAbsolutePath();
		if(name.length() <= baseNameLen)
			name = "";
		else
			name = name.substring(baseNameLen);
		if(File.separatorChar == '\\')
			name = name.replace('\\', '/');
		if(addedTopFolder != null)
			name = addedTopFolder + '/' + name;

		if(FileUtils.isSymlink(file)) {
			String linkTarget = FileUtils.readSymbolicLink(file);
			if(linkTarget != null) {
				TarArchiveEntry entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
				entry.setName(name);
				entry.setLinkName(linkTarget);
				tarOut.putArchiveEntry(entry);
			}
			return;
		}

		ArchiveEntry entry = tarOut.createArchiveEntry(file, name);
		tarOut.putArchiveEntry(entry);
		File[] children = file.listFiles(filter);
		if(children != null) {
			tarOut.closeArchiveEntry();
			// This is a directory. Append its children
			for(File child : children)
				append(child, filter, baseNameLen, addedTopFolder, tarOut);
			return;
		}

		// Append the content of the file
		InputStream input = new FileInputStream(file);
		try {
			StreamUtil.copy(input, tarOut);
			tarOut.closeArchiveEntry();
		}
		finally {
			StreamUtil.close(input);
		}
	}

	private static void chmod(Map<File, Map<Integer, List<String>>> chmodMap) throws IOException {
		for(Map.Entry<File, Map<Integer, List<String>>> entry : chmodMap.entrySet())
			for(Map.Entry<Integer, List<String>> dirEntry : entry.getValue().entrySet())
				for(List<String> files : splitList(dirEntry.getValue(), MAX_FILES_PER_COMMAND))
					OsUtil.chmod(entry.getKey(), dirEntry.getKey().intValue(), files.toArray(new String[files.size()]));
	}

	private static <T> List<String> getFileList(Map<File, Map<T, List<String>>> map, File dir, T key) {
		Map<T, List<String>> dirMap = map.get(dir);
		if(dirMap == null)
			map.put(dir, dirMap = new HashMap<T, List<String>>());
		List<String> files = dirMap.get(key);
		if(files == null)
			dirMap.put(key, files = new ArrayList<String>());

		return files;
	}

	public static void pack(File sourceFolder, OutputStream output, boolean includeTopFolder) throws IOException {
		pack(sourceFolder, output, null, includeTopFolder, null);
	}

	public static void pack(File sourceFolder, OutputStream output, FileFilter filter, boolean includeTopFolder,
			String addedTopFolder) throws IOException {
		TarArchiveOutputStream tarOut = new TarArchiveOutputStream(output);
		tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		String absName = sourceFolder.getAbsolutePath();
		int baseNameLen = absName.length() + 1;
		if(includeTopFolder)
			baseNameLen -= (sourceFolder.getName().length() + 1);

		try {
			append(sourceFolder, filter, baseNameLen, addedTopFolder, tarOut);
		}
		finally {
			StreamUtil.close(tarOut);
		}
	}

	private static void registerChmodFile(Map<File, Map<Integer, List<String>>> chmodMap, File dir, Integer mode,
			String file) {
		getFileList(chmodMap, dir, mode).add(file);
	}

	private static List<List<String>> splitList(List<String> files, int limit) {
		List<List<String>> result = new ArrayList<List<String>>();
		int top = files.size();
		int start = 0;
		while(start < top) {
			int max = Math.min(limit, top - start);
			result.add(files.subList(start, start + max));
			start += max;
		}
		return result;
	}

	/**
	 * Unpack the content read from <i>source</i> into <i>targetFolder</i>. If the
	 * <i>skipTopFolder</i> is set, then don't assume that the archive contains one
	 * single folder and unpack the content of that folder, not including the folder
	 * itself.
	 *
	 * @param source
	 *            The input source. Must be in <i>TAR</i> format.
	 * @param targetFolder
	 *            The destination folder for the unpack. Not used when a <tt>fileCatcher</tt> is provided
	 * @param skipTopFolder
	 *            Set to <code>true</code> to unpack beneath the top folder
	 *            of the archive. The archive must consist of one single folder and nothing else
	 *            in order for this to work.
	 * @param fileCatcher
	 *            Used when specific files should be picked from the archive without writing them to disk. Can be
	 *            <tt>null</tt>.
	 * @throws IOException
	 */
	public static void unpack(InputStream source, File targetFolder, boolean skipTopFolder, FileCatcher fileCatcher)
			throws IOException {
		String topFolderName = null;
		Map<File, Map<Integer, List<String>>> chmodMap = new HashMap<File, Map<Integer, List<String>>>();
		TarArchiveInputStream in = new TarArchiveInputStream(source);
		try {
			TarArchiveEntry te = in.getNextTarEntry();
			if(te == null) {
				throw new IOException("No entry in the tar file");
			}
			do {
				if(te.isGlobalPaxHeader())
					continue;

				String name = te.getName();
				if(name.startsWith("./._"))
					// MacOS specific extended attributes addition. Just skip it
					continue;

				if(skipTopFolder) {
					int firstSlash = name.indexOf('/');
					if(firstSlash < 0)
						throw new IOException("Archive doesn't contain one single folder");

					String tfName = name.substring(0, firstSlash);
					if(topFolderName == null)
						topFolderName = tfName;
					else if(!tfName.equals(topFolderName))
						throw new IOException("Archive doesn't contain one single folder");
					name = name.substring(firstSlash + 1);
				}
				if(name.length() == 0)
					continue;

				String linkName = te.getLinkName();
				if(linkName != null) {
					if(linkName.trim().equals(""))
						linkName = null;
				}

				if(fileCatcher != null) {
					if(linkName == null && !te.isDirectory() && fileCatcher.accept(name)) {
						if(fileCatcher.catchData(name, in))
							// We're done here
							return;
					}
					continue;
				}

				File outFile = new File(targetFolder, name);
				if(linkName != null) {
					if(!OsUtil.link(targetFolder, name, te.getLinkName()))
						throw new IOException("Archive contains links but they are not supported on this platform");
				}
				else {
					if(te.isDirectory()) {
						outFile.mkdirs();
					}
					else {
						outFile.getParentFile().mkdirs();
						OutputStream target = new FileOutputStream(outFile);
						StreamUtil.copy(in, target);
						target.close();
						outFile.setLastModified(te.getModTime().getTime());
					}
					registerChmodFile(chmodMap, targetFolder, Integer.valueOf(te.getMode()), name);
				}
			} while((te = in.getNextTarEntry()) != null);
		}
		finally {
			StreamUtil.close(in);
		}
		chmod(chmodMap);
	}

	private static final int MAX_FILES_PER_COMMAND = 20;
}
