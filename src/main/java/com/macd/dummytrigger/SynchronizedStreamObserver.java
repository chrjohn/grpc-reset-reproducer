package com.macd.dummytrigger;

import io.grpc.stub.StreamObserver;

public class SynchronizedStreamObserver<V>
        implements StreamObserver<V> {

    private StreamObserver<V> streamObserver;
    private long              messagesSent = 0;



    public SynchronizedStreamObserver( StreamObserver<V> streamObserver ) {
        this.streamObserver = streamObserver;
    }



    @Override
    public synchronized void onNext( V v ) {
        streamObserver.onNext( v );
        messagesSent++;
    }



    @Override
    public synchronized void onError( Throwable throwable ) {
        streamObserver.onError( throwable );
    }



    @Override
    public synchronized void onCompleted() {
        streamObserver.onCompleted();
    }


    
    public synchronized void finish( V v) {
        streamObserver.onNext( v );
        streamObserver.onCompleted();
    }

    /*
    public boolean isCancelled() {
        return streamObserver instanceof ServerCallStreamObserver
               && ( ( ServerCallStreamObserver )streamObserver ).isCancelled();
    }



    // see io.grpc.internal.AbstractStream.TransportState#DEFAULT_ONREADY_THRESHOLD
    public boolean isReady() {
        return streamObserver instanceof ServerCallStreamObserver
               && ( ( ServerCallStreamObserver )streamObserver ).isReady();
    }



    public long getMessagesSent() {
        return messagesSent;
    }



    public void setOnReadyHandler( Runnable onReadyHandler ) {
        if ( streamObserver instanceof ServerCallStreamObserver ) {
            ( ( ServerCallStreamObserver )streamObserver ).setOnReadyHandler( onReadyHandler );
        }
    }



    public void setOnCancelHandler( Runnable onCancelHandler ) {
        if ( streamObserver instanceof ServerCallStreamObserver ) {
            ( ( ServerCallStreamObserver )streamObserver ).setOnCancelHandler( onCancelHandler );
        }
    }
*/
}
