package com.macd.dummytrigger;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

class ResetReproducerTest {

    @Test
    void test()
        throws Exception {

        ExecutorService newSingleThreadExecutorServer = newSingleThreadExecutor();
        Runnable command = new Runnable() {

            @Override
            public void run() {
                DummyTriggerServer.main( null );
            }
        };
        newSingleThreadExecutorServer.execute( command );

        Thread.sleep( 1000 );
        DummyTriggerClient.main( null );

        newSingleThreadExecutorServer.shutdownNow();
        
        assertEquals( 1, DummyTriggerClient.getCounter() );

    }

}
