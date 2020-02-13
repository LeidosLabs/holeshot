# Simple HTTP example
#
# A simple example using the HTTP plugin that shows the retrieval of a single page via HTTP.
# More complex HTTP scripts are best created with the TCPProxy.
 
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from net.grinder.plugin.http import HTTPPluginControl
from net.grinder.plugin.http import HTTPUtilities
import random
#from tamtam import Metronom


# The default is to have a session per run.  Unfortunately this will cause Cognito to crap out with 
# "TooManyRequestsException: Rate exceeded".  Change this to create a session per thread.
grinder.SSLControl.shareContextBetweenRuns = 1 
test1 = Test(1, "Distributed Test")
request1 = HTTPRequest()
test1.record(request1)
#metronom = Metronom( 'RAMPING', "1,300 5,300 25,300 75,300 100,300 200,300 300,300 400,300 500,300 750,300 1000,300 1500,300 2000,300 2500,300 3000,300 3500,300 4000,300" )

def readTestFile():
	testData=[]
	fp = open("tests/testImages")
	try:
		for line in fp:
			testData.append(line.split())
	finally:
		fp.close()
	return testData

class TestRunner:
	TESTDATA_FILESIZE_INDEX=2
	TESTDATA_FILENAME_INDEX=3
	testFile=readTestFile()

	@staticmethod
	def randomTestData():
		return random.choice(TestRunner.testFile)

	def __call__(self):
		#metronom.getToken()
		httpUtilities = HTTPPluginControl.getHTTPUtilities()
		currentTestData = TestRunner.randomTestData()
		#testUrl = 'https://robertsrg--tileserver.leidoslabs.com/tileserver/%s?skipcache' % currentTestData[TestRunner.TESTDATA_FILENAME_INDEX]
		testUrl = 'https://robertsrg--tileserver.leidoslabs.com/tileserver/%s' % currentTestData[TestRunner.TESTDATA_FILENAME_INDEX]
		# grinder.logger.info("testUrl = %s" % testUrl)
		expectedSize = long(currentTestData[TestRunner.TESTDATA_FILESIZE_INDEX])
		
		result = request1.GET(testUrl, (),
							( httpUtilities.basicAuthorizationHeader('tileservertest', 'T1leserver!!'), ))
		
		resultData = result.getData()
		
		# grinder.logger.info('result.getStatusCode() = %d len(resultData) = %d expectedSize = %d' % (result.getStatusCode(), len(resultData), expectedSize))
		# print 'result.getStatusCode() = %d len(resultData) = %d expectedSize = %d' % (result.getStatusCode(), len(resultData), expectedSize)
		
		if ((result.getStatusCode() != 200) or (resultData is None) or (len(resultData) != expectedSize)):
		 	grinder.statistics.forLastTest.setSuccess(0)


