package hello.world;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown for {@link GreeterImpl} logic.
 */
public class HelloWorldServer {
    private static final Logger LOG = Logger.getLogger(HelloWorldServer.class.getName());

    private Server server;

    /**
     * Starts the gRPC server instance tied to this class.
     */
    private void start() throws IOException {
        server = Grpc.newServerBuilderForPort(GrpcPort.DEFAULT, InsecureServerCredentials.create())
                .addService(new GreeterImpl())
                .build()
                .start();
        LOG.info("Server started, listening on " + GrpcPort.DEFAULT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                HelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    /**
     * Stops the gRPC server instance tied to this class.
     */
    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        /**
         * Process intent based on the request at hand.
         */
        private HelloReply.REPLY_INTENT processIntent(HelloRequest req) {
            if (req.getName().contains("Hitler")) {
                return HelloReply.REPLY_INTENT.REJECT;
            } else if (req.getAge() < 0) { // Raises IllegalArgumentException since the value is fake
                return HelloReply.REPLY_INTENT.UNRECOGNIZED;
            } else if (req.getAge() < 10) {
                return HelloReply.REPLY_INTENT.GOOD_WISH;
            } else if (req.getAge() < 18) {
                return HelloReply.REPLY_INTENT.QUERY;
            } else if (req.getAge() < 40) {
                return HelloReply.REPLY_INTENT.ASK_OUT;
            }
            return HelloReply.REPLY_INTENT.WELCOME;
        }

        /**
         * Process message with request payload, based on the intent at hand.
         */
        private String processMessage(HelloRequest req, HelloReply.REPLY_INTENT intent) {
            return switch (intent) {
                case WELCOME -> "Hello " + req.getName() + "! " + "You are " + req.getAge() + " years old.";
                case QUERY -> "Good seeing you " + req.getName() + ". How is school treating you?";
                case ASK_OUT -> "Hey " + req.getName() + "! Can we go on a date?";
                case GOOD_WISH -> "Hey little " + req.getName() + " - continue to grow and prosper";
                case REJECT -> "Um...see you later " + req.getName() + "...";
                case UNRECOGNIZED -> "There is nothing for you to see " + req.getName() + ".";
            };
        }

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply.REPLY_INTENT intent = processIntent(req);
            String message = processMessage(req, intent);
            HelloReply reply = HelloReply.newBuilder().setMessage(message).setIntent(intent).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
