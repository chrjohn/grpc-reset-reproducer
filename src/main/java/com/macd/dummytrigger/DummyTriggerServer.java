package com.macd.dummytrigger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.macd.dummytrigger.protobuf.DummyTriggerRequestComponent;
import com.macd.dummytrigger.protobuf.DummyTriggerRequestComponent.DummyTriggerRequest;
import com.macd.dummytrigger.protobuf.DummyTriggerResponseComponent;
import com.macd.dummytrigger.protobuf.DummyTriggerResponseComponent.DummyTriggerResponse;
import com.macd.dummytrigger.protobuf.DummyTriggerServiceGrpc.DummyTriggerServiceImplBase;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DummyTriggerServer
    extends
    DummyTriggerServiceImplBase {

    private static final Logger             log    = LoggerFactory.getLogger( DummyTriggerServer.class );
    private static final DummyTriggerServer server = new DummyTriggerServer();
    private static Server                   grpcServer;



    public static void main( String[] args ) {

        log.info( "Starting gRPC server..." );
        InetSocketAddress localAddress = new InetSocketAddress( "0.0.0.0", 33333 );
        EventLoopGroup bossEventLoopGroup = new NioEventLoopGroup();
        EventLoopGroup grpcWorkerEventLoopGroup = new NioEventLoopGroup( 8,
                                                                         new ThreadFactoryBuilder().setUncaughtExceptionHandler( new MyUncaughtExceptionHandler() )
                                                                                                   .setDaemon( true )
                                                                                                   .setNameFormat( "GrpcWorkerExecutor-MDS"
                                                                                                                   + "-%d" )
                                                                                                   .build() );
        Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
        grpcServer = NettyServerBuilder.forAddress( localAddress )
                                       // .keepAliveTime( 30, TimeUnit.SECONDS )
                                       .addService( server )
                                       .bossEventLoopGroup( bossEventLoopGroup )
                                       .workerEventLoopGroup( grpcWorkerEventLoopGroup )
                                       .channelType( channelClass )
                                       // .executor( grpcExecutor )
                                       .build();
        try {
            grpcServer.start();
            log.info ("gRPC server started on port " + grpcServer.getPort() );
        } catch ( IOException e ) {
            log.error( "Exception when starting gRPC server: " + e.getMessage(), e );
        }

        try {
            grpcServer.awaitTermination();
        } catch ( InterruptedException e ) {
            // ignore
        }
    }



    @Override
    public void waitForDummyTrigger( DummyTriggerRequest request,
                                     StreamObserver<DummyTriggerResponse> responseObserver ) {
        SynchronizedStreamObserver<DummyTriggerResponseComponent.DummyTriggerResponse> synchronizedStreamObserver = new SynchronizedStreamObserver<>( responseObserver );
        server.addDummyTrigger( request, synchronizedStreamObserver );
    }



    public boolean addDummyTrigger( final DummyTriggerRequestComponent.DummyTriggerRequest request,
                                    final SynchronizedStreamObserver<DummyTriggerResponseComponent.DummyTriggerResponse> synchronizedStreamObserver ) {

        String requestString = request.getRequest();
        int delay = 1;
        int numberOfConcurrentTriggers = 3;
        ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool( numberOfConcurrentTriggers );
        CountDownLatch startLatch = new CountDownLatch( 1 );

        log.info( "Adding " + numberOfConcurrentTriggers + " dummy triggers for SynchronizedStreamObserver "
                  + synchronizedStreamObserver + " and request " + request + " to fire in " + delay + " seconds..." );

        AtomicBoolean atomicBoolean = new AtomicBoolean( false );
        for ( int i = 0; i < numberOfConcurrentTriggers; i++ ) {
            newScheduledThreadPool.schedule( new RunnableImplementation( synchronizedStreamObserver,
                                                                         startLatch,
                                                                         requestString,
                                                                         delay,
                                                                         atomicBoolean ),
                                             delay,
                                             TimeUnit.SECONDS );
        }

        startLatch.countDown();
        return true;
    }



    private DummyTriggerResponseComponent.DummyTriggerResponse createResponse( String reply ) {
        return DummyTriggerResponseComponent.DummyTriggerResponse.newBuilder().setReply( reply ).build();
    }

    private final class RunnableImplementation
        implements
        Runnable {

        private final SynchronizedStreamObserver<DummyTriggerResponse> synchronizedStreamObserver;
        private final CountDownLatch                                   startLatch;
        private final String                                           requestString;
        private final int                                              delay;
        private final AtomicBoolean                                    atomicBoolean;



        private RunnableImplementation( SynchronizedStreamObserver<DummyTriggerResponse> synchronizedStreamObserver,
                                        CountDownLatch startLatch,
                                        String requestString,
                                        int delay,
                                        AtomicBoolean atomicBoolean ) {
            this.synchronizedStreamObserver = synchronizedStreamObserver;
            this.startLatch = startLatch;
            this.requestString = requestString;
            this.delay = delay;
            this.atomicBoolean = atomicBoolean;
        }



        @Override
        public void run() {

            String threadInfo = Thread.currentThread().getName() + " with " + synchronizedStreamObserver
                                + " and request " + requestString;
            // if ( atomicBoolean.compareAndSet( false, true ) ) {

            try {
                String reply = threadInfo + " is triggered after " + delay + " seconds";
                DummyTriggerResponse response = createResponse( reply );
                // try to start all threads at nearly the same time
                // earlier threads will wait here until the last thread counts down the latch
                startLatch.await();
                synchronizedStreamObserver.onNext( response );
                Thread.sleep( 3 ); // XXX not using sleep() seems to make the problem occur less often
                synchronizedStreamObserver.onCompleted();
                
//                synchronizedStreamObserver.finish( response );
            } catch ( Throwable t ) {
                log.error( "Unable to trigger grpc: " + t.getMessage(), t );
                synchronizedStreamObserver.onError( t );
                // throw t;
            }
            // } else {
            // log.log( Level.SEVERE, threadInfo + " already triggered" );
            // }
        }
    }

    public static class MyUncaughtExceptionHandler
        implements
        UncaughtExceptionHandler {

        @Override
        public void uncaughtException( Thread t, Throwable e ) {
            String message = String.format( "Thread %s: %s", t.getName(), e.getMessage() );
            log.error( message, e );
        }

    }

}
