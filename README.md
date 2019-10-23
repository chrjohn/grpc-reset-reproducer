# grpc-reset-reproducer

See https://github.com/grpc/grpc-java/issues/6323

Either run `mvn clean package` which will start a unit test or you could also start the `main()` method of `DummyTriggerServer` and `DummyTriggerClient`.

`DummyTriggerServer` will start three threads that concurrently call `onNext()` and `onCompleted()` on the passed `StreamObserver` to provoke a race condition that will cancel the stream by a `RST_STREAM` message.
