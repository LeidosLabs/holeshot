'''
@summary: 
    An implementation of producer/consumers design pattern  with Java blockingQueue
    This implementation has been done to add the throughput management to TheGrinder
    ------------------------------------------------------------------------------------
  
@note:   How to include in TheGrinder code ?

from tamtam import Metronom
...
# 1TPS during 5 seconds, then 5TPS / 10seconds ...
# you can put also 0.2/300 (1 transaction every 5 seconds during 300 seconds)
metronom = Metronom( 'RAMPING', "1,5 5,10 30,10 5,10")
...
class TestRunner:
    def __init__(self):
        ...
    
    def __call__(self):
        #
        # Should be the first 
        # all threads wait for the token from the Metronom producer
        # 
        if metronom.getToken() == -1:
            print "Thread %d - Arghhh! - after %d runs" % (grinder.threadNumber, grinder.runNumber)
            grinder.stopThisWorkerThread()

    
    TODO : manage the threads starvation (not enough threads to consume producer tokens)
    TODO : allow changing throughput dynamically (by scanning property file and then changing Metronom policy)

@author: olivier Merlin,Gemalto, 2010
'''
from java.lang import Thread, Runnable
from java.util.concurrent import LinkedBlockingQueue

class FlatCadencer:
    '''
        Flat means fixed regular interval of time
    '''
    def __init__(self, value=1000):
        self._value = value
    def set(self, value):
        self._value = value;
    def next(self):
        return self._value;

class RythmCadencer:
    '''
        Define a rythm ( in TPS) during a given duration in seconds
        After initialization (TPS, duration), you have a number(self._count) of 
        interval (self._interval)
        You have finished to consume intervals, you return -1  
    '''
    def __init__(self, rythmStr):
        '''  
        @param rythmStr: a formated string "nbTPS,duration"
        '''
        [self.tps, self.duration] = [float(k) for k in rythmStr.split(',')]
        self._interval = 1000/self.tps
        # Number of interval
        self._count=self.duration*self.tps
        
    def getTps(self):
        return self.tps
    def getDuration(self):
        return self.duration
            
    def next(self):
        self._count = self._count - 1
        if self._count<0:
            # it's finished - no more interval
            return -1
        else:
            return self._interval
        
    def __repr__(self):
        return "------ Throughput = %d TPS during %d seconds" % (self.tps, self.duration)

class RampingCadencer:
    """
      Define a complete test rampup/rampdown with different phase
      This is defined as a string of the form
      tps1,duration1 tps2,duration2 tps3,duration3 ... tpsN,durationN
      OR
      tps1,duration1,tps2,duration2,tps3,duration3 ...,tpsN,durationN
    """
    def __init__(self, rampingStr):
        self.rampingStr = rampingStr.replace(' ',',')
        items = self.rampingStr.split(',')
        self.rc=[]
        for k in range(0,len(items),2):
            self.rc.append(RythmCadencer("%s,%s"% (items[k],items[k+1])))
        self.max=len(self.rc)
        self.inx=0
        
    def next(self):
        '''
           Get next sleep interval from RythmCadencer
           If we get -1, that means that we change interval, so we move to next RythmCadencer
           If we were at last RythmCadencer - OK - all is finished        
        '''
        changed=False
        value = self.rc[self.inx].next()
        if value <0:
            self.inx = self.inx+1
            changed=True
            
            if self.inx == self.max:
                value =  -1
            else :
                print self.rc[self.inx]
                value = self.rc[self.inx].next()
        return value,changed

    def __repr__(self):
        return "Ramping string :\n" + self.rampingStr
    

class __Metronom:
    def __init__(self, q, func, nbConsumers,debug):
        print func
        self.t = Thread(__Metronom.__Producer(q,func,nbConsumers,debug), "TheMetronomProducer")         
        self.t.start()
        print "Metronom is started for func=%s with %d consumers..." % (func.__class__.__name__,nbConsumers)      
        self.debug=debug
        #self.isalive = True

    class __Producer(Runnable):
        '''
          A runnable inner class that just product tempo (Oject()) every Cadencer tempo 
        '''
        def __init__(self, q, func,nbConsumers,debug):
            self._queue = q
            self._cadencer = func
            self.nbConsumers=nbConsumers 
            self._inc=0
            self.debug=debug

                    
        def run(self):
            #print self._name
            while True:
                (time_to_sleep,is_break) = self._cadencer.next()
                # Condition to stop : -1
                if time_to_sleep<0:
                    break
                
                # Reset queue when changing rate
                if is_break:
                    self._queue.clear()
                    
                if self.debug:
                    print "Sleeping time for %d ms" % (time_to_sleep)
                Thread.sleep(long(time_to_sleep))
                self._inc = self._inc+1
                if self.debug:
                    print "Putting message %d " % time_to_sleep
                self._queue.put(time_to_sleep)
                if self.debug:
                    print "Bottle object sent"
                
            if self.debug:
                print "OK - i quit and i force consumers to stop ..."
            # The trick is to poison all consumer to force them to stop
            for k in range(self.nbConsumers):
                self._queue.put(-1)
                Thread.sleep(5)
 


class Metronom:
    def __init__(self, rampupType, rampupValue, nbConsumers=1000, debug=False):
        '''
           Create the metronom 
           
        @param rampupType: FLAT, RYTHM, RAMPING 
        @param rampupValue: a value for rampup
        @param nbConsumers: in grinder - gringer.threadNumber
        '''
        self.queue = LinkedBlockingQueue()
        rampupFoo={'FLAT':FlatCadencer,'RYTHM':RythmCadencer,'RAMPING':RampingCadencer}
        try:
            foo = rampupFoo[rampupType]
        except :
            print "%s function does not exists" % (rampupType)
            raise
        self.metronom = __Metronom(self.queue, foo(rampupValue), nbConsumers, debug)
        
    def getToken(self):
        return self.queue.take()
    
    def getQueue(self):
        return self.queue


        