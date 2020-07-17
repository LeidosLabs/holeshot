package com.leidoslabs.holeshot.tileserver.service;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The ITileStore interface abstracts the underlying tile store implementation from the rest
 * of the code.
 * <p>
 * It refers to the concept of folders which are containers where objects are stored
 * Implementations of this interface may consider the concept of folder differently.
 * For example an object store, such as Amazon S3, would use the key name to
 * recognise the folder. An SMB file store would use a hierarchical set of folders.
 */
public interface ITileStore {

    Response getResponse(ServletRequest request, String wholeKey);

    WriteFrom writeResponse(ServletRequest request, String wholeKey, OutputStream out) throws Exception;

    /**
     * Get SMB object and copy to out, and add to cache if useCache. Ensures retrieved objects are less than
     * MAX_CACHED_TILE_SIZE_IN_BYTES
     *
     * @param wholeKey the key for the object to retrieve
     * @param out      the outstream to copy the object to
     * @param useCache true indicates that the object should be added to the cache too
     * @throws Exception
     */
    void writeFromStore(String wholeKey, OutputStream out, boolean useCache) throws Exception;

    /**
     * Puts an object into the store
     *
     * @param objectName The name that the object should be stored as. This should be the relative
     *                   path from the root of the store
     * @param bis        the object that should be written
     * @param mimeType   the mime type of the object
     */
    void putObject(String objectName, ByteArrayInputStream bis, String mimeType);

    /**
     * Gets an iterator for the names of all the sub folders under the path passed
     * in. This will not include objects within the folder and will only go one level
     * down from the parent
     *
     * @param parentPath the parent folder to search within
     * @return iterator for the names of all the subfolders paths
     */
    Iterator<String> getSubFolders(String parentPath);

    /**
     * Gets an iterator containing all the names of sub folders that contain the string passed in. The string
     * is contained anywhere within the path
     *
     * @param parentPath   the parent folder to search within
     * @param nameContains the string that the name must contain
     * @return iterator containing all the names of subfolders that contain the string passed in
     */
    Iterator<String> getSubFoldersByName(String parentPath, String nameContains);

    /**
     * Gets an iterator containing all the names of sub folders that contain the string passed in. The string
     * is contained anywhere within the path
     *
     * @param parentPath   the parent folder to search within
     * @param nameContains the string that the name must contain
     * @return stream containing all the names of subfolders that contain the string passed in
     */
    Stream<String> getSubFoldersByNameAsStream(String parentPath, String nameContains);

    /**
     * Gets an iterator containing all the names of sub folders that match the regular expression
     *
     * @param parentPath the parent folder to search within
     * @param regExp     the regular expression to search using
     * @return iterator containing the names of subfolders that match the regex string
     */
    Iterator<String> getSubFoldersMatching(String parentPath, Pattern regExp);

    /**
     * Gets a stream containing all the names of sub folders that match the regular expression
     *
     * @param parentPath the parent folder to search within
     * @param regExp     the regular expression to search using
     * @return stream containing the names of subfolders that match the regex string
     */
    Stream<String> getSubFoldersMatchingAsStream(String parentPath, Pattern regExp);

    /**
     * Returns a list of the objects that are in a particular folder
     *
     * @param folder the folder to list the objects from
     * @return a stream of objects that support the ITile interface
     */
    Stream<ITile> listObjects(String folder);

    /**
     * Returns a list of the objects that are in a particular folder
     *
     * @param folder          the folder to list the objects from
     * @param objectNameRegex regexpression to be used to filter objects
     * @return a stream of objects that support the ITile interface
     */
    Stream<ITile> listObjects(String folder, Pattern objectNameRegex);

    enum WriteFrom {CACHE, S3}

}
