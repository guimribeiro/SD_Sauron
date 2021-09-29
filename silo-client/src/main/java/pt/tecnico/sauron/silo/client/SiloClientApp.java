package pt.tecnico.sauron.silo.client;


import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String path = args[2];

		Frontend frontend;
		try {
			frontend = new Frontend(zooHost, zooPort, path);
		} catch (ZKNamingException e) {
			System.out.println(e.getMessage());
			return;
		}

		try {
			PingRequest request = PingRequest.newBuilder().setTxt("friend").build();
			PingResponse response = frontend.ctrl_ping(request);
			System.out.println(response);

		}catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " +
					e.getStatus().getDescription());
		}
		frontend.close();
	}
	
}
