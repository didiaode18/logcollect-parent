package org.apache.flume.interceptor;

import java.util.List;

import org.apache.logcollect.Context;
import org.apache.logcollect.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LimitInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(LimitInterceptor.class);

    private static long KB = 1024L;

    private long lastEventSentTick = System.nanoTime();

    private long pastSentLength = 0L;
    private long max;
    private long timeCostPerCheck = 1000000000L;

    private long headerSize = 0L;

    //private boolean flag = true;

    private int num = 0;

    public LimitInterceptor(long limitRate, long headerSize) {
        this.max = (limitRate * KB);
        this.headerSize = headerSize;
    }

    public void initialize() {
    }

    public Event intercept(Event event) {
        this.num += 1;
        //logger.info("intercept =================== " + new String(event.getBody()));
        if (this.pastSentLength > this.max) {
            long nowTick = System.nanoTime();
            long multiple = this.pastSentLength / this.max;
            long missedTime = multiple * this.timeCostPerCheck - (nowTick - this.lastEventSentTick);
            if (missedTime > 0L) {
                try {
                    /*System.out.printf("Limit source send rate, headerLength:%d,pastSentLength:%d,lastEventSentTick:%d,sleepTime:%d, num:%d\n",
                        headerSize, pastSentLength, lastEventSentTick, missedTime / 1000000, num);*/
                    //logger.info("speed fast ,begin slowing.......");
                    Thread.sleep(missedTime / 1000000L, (int) (missedTime % 1000000L));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.num = 0;
            this.pastSentLength = 0L;
            this.lastEventSentTick = (nowTick + (missedTime > 0L ? missedTime : 0L));
        }
        this.pastSentLength += this.headerSize + event.getBody().length;

        return event;
    }

    public List<Event> intercept(List<Event> events) {
    	//logger.info("intercept(List<Event> events)");
        for (Event event : events) {
            intercept(event);
        }
        return events;
    }

    public void close() {
    }

    public static class Builder implements Interceptor.Builder {
        private long limitRate;
        private long headerSize;

        public Interceptor build() {
            return new LimitInterceptor(this.limitRate, this.headerSize);
        }

        public void configure(Context context) {
        	
            this.limitRate = context.getLong(Constants.LIMIT_RATE, Long.valueOf(Constants.DEFAULT_RATE)).longValue();
            this.headerSize = context.getLong(Constants.HEADER_SIZE, Long.valueOf(Constants.DEFAULT_SIZE)).longValue();
            logger.info("================Interceptor configure : the limitRate is " + this.limitRate);
        }

        public static class Constants {
            public static long DEFAULT_RATE = 500L;
            public static long DEFAULT_SIZE = 16L;
            public static String LIMIT_RATE = "limitRate";
            public static String HEADER_SIZE = "headerSize";
        }
    }
}
