# grpc-reset-reproducer

See XXXX

Either run `mvn clean package` which will start a unit test or you could also start the `main()` method of `DummyTriggerServer` and `DummyTriggerClient`.

`DummyTriggerServer` will start three threads that concurrently call `onNext()` and `onCompleted()` on the passed `StreamObserver`.
