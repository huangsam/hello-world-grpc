package hello.world;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
    private static final Logger LOG = Logger.getLogger(HelloWorldClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /** Construct client for accessing HelloWorld server using the existing channel. */
    public HelloWorldClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /** Say hello to server. */
    public void greet(String name, int age) {
        LOG.info("Attempt to greet " + name + " via " + HelloWorldServer.class.getName());
        HelloRequest request = HelloRequest.newBuilder().setName(name).setAge(age).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            LOG.log(Level.WARNING, "[Failure] {0}", e.getStatus());
            return;
        }
        LOG.info("[Success] Got message '" + response.getMessage() + "' with intent " + response.getIntent());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting. The second argument is the target server.
     */
    public static void main(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        String target = "localhost:" + GrpcPort.STRING_VALUE;
        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.printf("""
                        Usage: [name [target]]

                        name    The name you wish to be greeted by. Defaults to %s
                        target  The server to connect to. Defaults to %s
                        """, user, target);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 1) {
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet(user, 10);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
