package pt.tecnico.sauron.silo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;
import java.util.Scanner;

public class SiloServerApp {

	public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {
		System.out.println(SiloServerApp.class.getSimpleName());
		long gossipInterval = 30;

		// check arguments
		if (args.length < 6) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", SiloServerApp.class.getName());
			return;
		}
		if (args.length > 7) {
			System.err.println("Argument(s) in excess!");
			System.err.printf("Usage: java %s port%n", SiloServerApp.class.getName());
			return;
		}
		if (!args[1].matches("[0-9]+")) {
			System.out.println("invalid zooPort format");
			return;
		}
		if (!args[3].matches("[0-9]+")) {
			System.out.println("invalid port format");
			return;
		}
		if (!args[4].matches("[0-9]+")) {
			System.out.println("invalid replicaId format");
			return;
		}
		if (!args[5].matches("[0-9]+")) {
			System.out.println("invalid numReplicas format");
			return;
		}
		if (args.length == 7) {
			if (!args[6].matches("[0-9]+")) {
				System.out.println("invalid numReplicas format");
				return;
			}
			gossipInterval = Long.parseLong(args[6]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final String port = args[3];
		final int instance = Integer.parseInt(args[4]);
		final int numReplicas = Integer.parseInt(args[5]);

		if (numReplicas < 1) {
			System.out.println("invalid number of replicas");
			return;
		}
		if (instance < 1 || instance > numReplicas) {
			System.out.println("invalid instance: must be greater than 1 and less than numReplicas");
			return;
		}

		final String path = "/grpc/sauron/silo/" + instance;
		final BindableService impl = new SiloServiceImpl(zooHost, zooPort, instance, numReplicas, gossipInterval);
		ZKNaming zkNaming = null;

		try {

			zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, port);

			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

			// Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");

			new Thread(() -> {
				// wait for a newline char, to shutdown the server
				new Scanner(System.in).nextLine();
				((SiloServiceImpl) impl).closeThreads();
				server.shutdown();
			}).start();

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();
		} catch (ZKNamingException e) {
			System.out.println(e.getMessage());
		} finally {
			if (zkNaming != null) {
				zkNaming.unbind(path, host, String.valueOf(port));
			}
		}

	}

}
