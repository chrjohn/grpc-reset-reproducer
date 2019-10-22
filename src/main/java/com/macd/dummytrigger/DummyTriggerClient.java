package com.macd.dummytrigger;

import static io.grpc.internal.GrpcUtil.getThreadFactory;

import com.macd.dummytrigger.protobuf.DummyTriggerRequestComponent;
import com.macd.dummytrigger.protobuf.DummyTriggerResponseComponent;
import com.macd.dummytrigger.protobuf.DummyTriggerServiceGrpc;
import com.macd.dummytrigger.protobuf.DummyTriggerServiceGrpc.DummyTriggerServiceStub;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DummyTriggerClient {

    private static ManagedChannel      managedChannel = null;
    private static final Logger        log            = LoggerFactory.getLogger( DummyTriggerClient.class );

    private static final String        host           = "localhost";
    private static final int           port           = 33333;
    private static Instant             triggerStartTime;
    private static final AtomicInteger counter        = new AtomicInteger();



    public static void main( String[] args ) {

        connectToGrpc();

        DummyTriggerServiceStub newStub = DummyTriggerServiceGrpc.newStub( managedChannel );
        DummyTriggerClient.trigger( newStub );
        
        try {
            Thread.sleep( 3000 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }

        if ( managedChannel != null ) {
            managedChannel.shutdownNow();
            log.info( "GRPC connection closed." );
        }

        log.info( "onCompleted() was called " + counter.get() + " times" );
    }



    private static StreamObserver<DummyTriggerResponseComponent.DummyTriggerResponse> trigger( DummyTriggerServiceGrpc.DummyTriggerServiceStub stub ) {

        DummyTriggerRequestComponent.DummyTriggerRequest dummyTriggerRequest = DummyTriggerRequestComponent.DummyTriggerRequest.newBuilder()
                                                                                                                               .setRequest( "dummy" )
                                                                                                                               .build();

        StreamObserver<DummyTriggerResponseComponent.DummyTriggerResponse> triggerObserver = createStreamObserver( "dummy" );

        stub.waitForDummyTrigger( dummyTriggerRequest, triggerObserver );
        triggerStartTime = Instant.now();
        log.info( "Trigger condition started." );

        return triggerObserver;
    }



    private static void connectToGrpc() {
        log.info( "Opening GRPC connection to: " + host + ":" + port );
        managedChannel = getManagedChannel( new InetSocketAddress( host, port ) );
        log.info( "Connection established." );
    }



    private static ManagedChannel getManagedChannel( SocketAddress socketAddress ) {
        return NettyChannelBuilder.forAddress( socketAddress )
                                  .usePlaintext()
                                  .executor( Executors.newFixedThreadPool( 16,
                                                                           getThreadFactory( "ManagedChannelExecutor"
                                                                                             + "-%d",
                                                                                             true ) ) )
                                  .build();
    }



    private static <T> StreamObserver<T> createStreamObserver( String key ) {
        return new StreamObserver<>() {

            @Override
            public void onNext( T response ) {

                if ( response instanceof DummyTriggerResponseComponent.DummyTriggerResponse ) {
                    log.info( "Received trigger response: " + response );
                    log.info( "Triggered after " + ( Instant.now().getEpochSecond() - triggerStartTime.getEpochSecond() )
                         + " seconds." );
                } else {
                    log.info( "Received unknown response: " + response );
                }

            }



            @Override
            public void onError( Throwable throwable ) {
                Status status = Status.fromThrowable( throwable );
                log.error( this.toString() + " " + status + " - " + throwable.getMessage(), throwable );
            }



            @Override
            public void onCompleted() {
                log.info( "Subscription completed. " + counter.incrementAndGet() );
            }
        };
    }
    
    
    public static int getCounter() {
        return counter.get();
    }
}
