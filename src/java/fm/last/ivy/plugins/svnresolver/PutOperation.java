/*
 * Copyright 2008 Last.fm
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package fm.last.ivy.plugins.svnresolver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Operation that represents adding/updating a file in svn.
 */
public class PutOperation {

  /**
   * The file to put.
   */
  private File file;
  
  /**
   * Whether any existing file data in svn should be overwritten or not.
   */
  private boolean overwrite;
  
  /**
   * The contents of the file.
   */
  byte[] data;
  
  /**
   * The folder path of the file.
   */
  private String folderPath;

  /*
   * The full path to the file destination location in svn.
   */
  private String fullDestinationPath;
  
  /**
   * The name of the file (without any folder path).
   */
  private String fileName;
  
  /**
   * The svn base repository path, if not set it is assumed to be the path up to the first "/" in any paths.
   */
  private String baseRepositoryPath;

  /**
   * Constructs a new PutOperation.
   * 
   * @param file The file to be added/updated in svn.
   * @param destination The full svn destination path of the file.
   * @param overwrite Whether any existing file data should be overwritten or not.
   * @param baseRepositoryPath The SVN base repository path, if null, the base with will be assumed to be the path up to
   *          the first "/" in any svn paths (often the case).
   * @throws IOException If the file data cannot be read from disk or the file paths cannot be determined.
   */
  public PutOperation(File file, String destination, boolean overwrite, String baseRepositoryPath)
      throws IOException {
    this.file = file;
    this.fullDestinationPath = destination;
    this.overwrite = overwrite;
    this.baseRepositoryPath = baseRepositoryPath;
    try {
      determinePaths();
    } catch (SVNException e) {
      throw new IOException("Error determing paths from " + destination, e);
    }
    
    // When Ivy does a put it copies the files to a temp location and calls put from there, unfortunately it
    // deletes some files (e.g. hashes) inbetween calls so in order for us to send them as a transaction
    // we store entire file contents in memory.
    if (file != null && file.isFile()) {
      data = new byte[(int) file.length()];
      BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
      fileInputStream.read(data); // convert from file to byte array
      fileInputStream.close();
    } else {
      throw new IOException("No file data found.");
    }
  }

  /**
   * Determine the various file-related paths that are needed to put this file into svn.
   * 
   * @throws SVNException If an error occurs parsing the destination path.
   */
  private void determinePaths() throws SVNException {
    SVNURL destinationUrl = SVNURL.parseURIEncoded(fullDestinationPath);
    String destinationPath = destinationUrl.getPath();
    
    // by default assume the path to the root of the repository is the path up until the first "/"
    int rootFolderIndex = destinationPath.indexOf("/", 1);
    
    if (baseRepositoryPath != null) { // if a rep path has been explicitly set, use it instead
      if (!destinationPath.startsWith(baseRepositoryPath)) {
        throw new IllegalArgumentException("The destination path ('" + destinationPath
            + "') must start with the Subversion repository URL  ('" + baseRepositoryPath + "')");
      }
      rootFolderIndex = baseRepositoryPath.length();
    }

    if (rootFolderIndex < 0) {
      throw new IllegalArgumentException(
          "Invalid repository path, there must be at least an existing root folder in the repository");
    }

    // make sure the destination is a file and not a folder
    int fileNameIndex = destinationPath.lastIndexOf("/");
    if (fileNameIndex == destinationPath.length() - 1) {
      throw new IllegalArgumentException("Can only publish files (not folders), check your publish pattern");
    }

    // now figure out the various different elements of the path
    // String destinationPrefix = destination.substring(0, destination.indexOf(destinationPath));
    // extract the path up until the root (i.e. the part that must exist)
    String rootPath = destinationPath.substring(0, rootFolderIndex);
    // extract the rest of the path before the file name (i.e. folders only)
    this.folderPath = destinationPath.substring(rootPath.length(), fileNameIndex);
    // extract the name of the file
    this.fileName = destinationPath.substring(fileNameIndex + 1);
  }

  /**
   * @return the folderPath
   */
  public String getFolderPath() {
    return folderPath;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @return the overwrite
   */
  public boolean isOverwrite() {
    return overwrite;
  }

  /**
   * @return the data
   */
  public byte[] getData() {
    return data;
  }

}