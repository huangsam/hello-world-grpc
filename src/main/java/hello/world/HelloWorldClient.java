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
 * Client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
    private static final Logger LOG = Logger.getLogger(HelloWorldClient.class.getName());

    // Access a service running on the local machine on port 50051
    private static final String TARGET = "localhost:" + GrpcPort.STRING_VALUE;

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /**
     * Construct client for accessing HelloWorld server using the existing channel.
     */
    private HelloWorldClient(Channel channel) {
        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /**
     * Say hello to server.
     */
    private void greet(String name, int age) {
        LOG.info("Attempt to greet " + name + " of " + age + " via " + HelloWorldServer.class.getName());
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
     * greeting. The second argument is the age to use in the greeting.
     */
    public static void main(String[] args) throws Exception {
        String user = "world";

        int age = -1;

        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.printf("""
                        Usage: [name [age]]

                        name    The name you wish to be greeted by. Defaults to %s
                        age     The age you wish to be greeted by. Defaults to %d
                        """, user, age);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 1) {
            age = Integer.parseInt(args[1]);
        }

        // Use plaintext insecure credentials to avoid needing TLS certificates
        ManagedChannel channel = Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create()).build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet(user, age);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
