package com.leidoslabs.holeshot.tileserver.service;

import com.amazonaws.services.s3.model.ListObjectsV2Result;
import junit.framework.TestCase;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.findify.s3mock.S3Mock;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class S3HandlerTest extends TestCase {

    private S3Mock mockS3api;
    private AmazonS3 testClient;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    public void setUp() throws Exception {
        super.setUp();

        //Set up the mock S3 API which will use memory to store the objects rather than the file system
        mockS3api = new S3Mock.Builder().withPort(8001).withInMemoryBackend().build();
        mockS3api.start();


       /* AWS S3 client setup.
         *  withPathStyleAccessEnabled(true) trick is required to overcome S3 default
         *  DNS-based bucket access scheme
         *  resulting in attempts to connect to addresses like "bucketname.localhost"
         *  which requires specific DNS setup.
         */
        EndpointConfiguration endpoint = new EndpointConfiguration("http://localhost:8001", "us-west-2");
        testClient = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpoint)
 //               .withRegion("us-west-2")
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();

        //Put some data in the S3mock
        testClient.createBucket("testbucket");
        testClient.putObject("testbucket", "tile01", "tile01 at root");
        testClient.putObject("testbucket", "file/tile01", "tile01");
        testClient.putObject("testbucket", "file/tile01/metadata.json", "tile01 metadata");
        testClient.putObject("testbucket", "file/tile02", "tile02");
        testClient.putObject("testbucket", "file/tile02/additional folder/data", "Additional folder data");
        testClient.putObject("testbucket", "file/tile02/metadata.json", "tile02 metadata");
        testClient.putObject("testbucket", "file/tile03", "tile03");
        testClient.putObject("testbucket", "file/tile04/random-object", "Random object");
        testClient.putObject("testbucket", "file2/tile05/random-object", "Random object");
        testClient.putObject("testbucket", "file2/tile05/random-object2", "Random object 2");
        testClient.putObject("testbucket", "file2/tile06/tile05/object", "Random object");
        testClient.putObject("testbucket", "file2/tile06/tile05", "Random object");

        ListObjectsV2Result myResults = testClient.listObjectsV2 ("testbucket");
        assertEquals(12, myResults.getKeyCount());
    }

    public void tearDown() throws Exception {
        mockS3api.shutdown();
    }

    public void testGetResponse() {
    }

    public void testWriteResponse() {
    }

    public void testWriteFromStore() {
    }

    public void testPutObject() {
    }

    public void testGetSubFolders() {
        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        System.out.println("Subfolders under file/");
        Iterator<String> childPrefixes = handler.getSubFolders("file/");
        int folderCount = 0;
        while (childPrefixes.hasNext()){
            String prefix = childPrefixes.next();
            System.out.println(prefix);
            folderCount++;
        }
        assertEquals(3,folderCount);

        System.out.println("Subfolders under file/tile01");
        childPrefixes = handler.getSubFolders("file/tile01/");
        folderCount = 0;
        while (childPrefixes.hasNext()){
            String prefix = childPrefixes.next();
            System.out.println(prefix);
            folderCount++;
        }
        assertEquals(0,folderCount);

        System.out.println("Subfolders under file/tile02");
        childPrefixes = handler.getSubFolders("file/tile02/");
        folderCount = 0;
        while (childPrefixes.hasNext()){
            String prefix = childPrefixes.next();
            System.out.println(prefix);
            folderCount++;
        }
        assertEquals(1,folderCount);

        System.out.println("Subfolders under root");
        childPrefixes = handler.getSubFolders("");
        folderCount = 0;
        while (childPrefixes.hasNext()){
            String prefix = childPrefixes.next();
            System.out.println(prefix);
            folderCount++;
        }
        assertEquals(2,folderCount);
    }

    public void testGetSubFoldersByName() {
        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        Iterator<String> folders = handler.getSubFoldersByName("file2/", "tile06");
        int folderCount = 0;
        while (folders.hasNext()){
            String folderName = folders.next();
            System.out.println(folderName);
            folderCount++;
        }
        assertEquals(1,folderCount);

        folders = handler.getSubFoldersByName("file2/", "tile05");
        folderCount = 0;
        while (folders.hasNext()){
            String folderName = folders.next();
            System.out.println(folderName);
            folderCount++;
        }
        assertEquals(1,folderCount);

    }

    public void testGetSubFoldersByNameAsStream() {
        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        assertEquals(1,handler.getSubFoldersByNameAsStream("file2/", "tile06").count());

        assertEquals(1,handler.getSubFoldersByNameAsStream("file2/", "tile05").count());

    }
    public void testListObjects() {

        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        //listObjects - no filters
        Stream<ITile> childObjects = handler.listObjects("");
        //childObjects.forEach(o -> System.out.println(o.getKey()));
        assertEquals(12, childObjects.count());

        //listObjects in folder
        Stream<ITile>  childObjects2 = handler.listObjects("file/");
        assertEquals(7, childObjects2.count());

        //listObjects with regex
        Stream<ITile>  childObjects3 = handler.listObjects("", Pattern.compile("[A-Z,a-z,0-9/]*metadata.json\\b"));
        assertEquals(2,childObjects3.count());

        //listObjects in folder with regex
        Stream<ITile>  childObjects4 = handler.listObjects("file/tile02",  Pattern.compile("[A-Z,a-z,0-9/]*metadata.json\\b"));
        assertEquals(1,childObjects4.count());

    }

    public void testGetSubFoldersMatching() {
        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        Iterator<String> folders = handler.getSubFoldersMatching("file2/tile06/", Pattern.compile(".*"));
        int folderCount = 0;
        while (folders.hasNext()){
            String folderName = folders.next();
            System.out.println(folderName);
            folderCount++;
        }
        assertEquals(1,folderCount);

        folders = handler.getSubFoldersMatching("file2/", Pattern.compile(".*05"));
        folderCount = 0;
        while (folders.hasNext()){
            String folderName = folders.next();
            System.out.println(folderName);
            folderCount++;
        }
        assertEquals(1,folderCount);

        folders = handler.getSubFoldersMatching("file2/", Pattern.compile("object"));
        folderCount = 0;
        while (folders.hasNext()){
            String folderName = folders.next();
            System.out.println(folderName);
            folderCount++;
        }
        assertEquals(0,folderCount);
    }


    public void testGetSubFoldersMatchingAsStream() {
        S3Handler handler = new S3Handler("testbucket", "us-east-1", 20);
        handler.s3Client = testClient;

        assertEquals(1,handler.getSubFoldersMatchingAsStream("file2/tile06/", Pattern.compile(".*")).count());

        assertEquals(1,handler.getSubFoldersMatchingAsStream("file2/", Pattern.compile(".*05")).count());

        assertEquals(0,handler.getSubFoldersMatchingAsStream("file2/", Pattern.compile("object")).count());
    }
}