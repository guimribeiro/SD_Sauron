package pt.tecnico.sauron.spotter;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.Frontend;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;
import static java.lang.System.setOut;

public class SpotterApp {
	private static String zooHost;
	private static String zooPort;
	private static String serverId;

	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		serverId = null;

        // validate arguments
		if (args.length == 3) {
			if (!args[2].matches("[0-9]+")) {
				System.out.println("invalid serverId format");
				return;
			}
			serverId = args[2];
		}
		else if (args.length != 2) {
			System.out.println("invalid number of arguments");
			return;
		}
		if (!args[1].matches("[0-9]+")) {
			System.out.println("invalid port format");
			return;
		}

		zooHost = args[0];
		zooPort = args[1];

		Frontend frontend = connect(zooHost, zooPort);

		Scanner scanner = new Scanner(System.in);
		System.out.println("Run the command \'cache_limit <VALUE>\' to define the maximum cache size");
		System.out.println("Press <ENTER> to keep the maximum cache size to its default value (5)");
		String line = scanner.nextLine();
		String[] data = line.split(" ");
		if (data[0].equals("cache_limit")) {
			if (data.length != 2) {
				System.out.println("Invalid number of arguments! Maximum cache size set to its default value");
			}
			else if (!data[1].matches("[0-9]+")) {
				System.out.println("Invalid value. Maximum cache size set to its default value");
			}
			else {
				frontend.setCacheLimit(Integer.parseInt(data[1]));
				System.out.println("Maximum cache size set to " + data[1]);
			}
		}
		else if (data[0].isEmpty()) {
			System.out.println("Maximum cache size set to its default value");
		}
		else {
			System.out.println("Invalid command. Maximum cache size set to its default value");
		}

		search(frontend);
	}

	private static Frontend connect(String zooHost, String zooPort) {
		Frontend frontend = null;
		if (serverId != null) {
			try {
				frontend = new Frontend(zooHost, zooPort, serverId);
			} catch (ZKNamingException | StatusRuntimeException e) {
				System.out.println("Impossible to reach the instance " + serverId);
				exit(0);
			}
		} else {
			boolean connected = false;
			while (!connected) {
				try {
					frontend = new Frontend(zooHost, zooPort);
					connected = true;
				} catch (ZKNamingException | StatusRuntimeException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		return frontend;
	}

	private static void search(Frontend frontend) {
		String line;
		try (Scanner scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				String[] data = line.split(" ");

				ObjectType objectType;
				String id;

				if (line.isBlank()) { // ignore blank lines
					break;
				}
				else if (line.charAt(0) == '#') { // ignore a line that starts with '#'
					continue;
				}
				switch (data[0]) {
					case ("spot"):
						if (data[1].equals("person"))
							objectType = ObjectType.PERSON;
						else if (data[1].equals("car"))
							objectType = ObjectType.CAR;
						else {
							System.out.println("invalid object");
							continue;
						}

						id = data[2];
						if (id.contains("*")) { // if id contains a '*', then it is a partial id
							try {
								List<Obs> obsList = frontend.trackMatch(TrackMatchRequest.newBuilder()
										.setObjectType(objectType)
										.setId(id)
										.build()).getObsListList();

								printObsList(obsList);
							} catch (StatusRuntimeException e) {
								System.out.println(e.getMessage());
							}
						}
						else { // else, it is an exact id
							Obs obs = frontend.track(TrackRequest.newBuilder()
									.setObjectType(objectType)
									.setId(id)
									.build()).getObs();
							List<Obs> obsList = new ArrayList<>(); // create a list to add the observation
							obsList.add(obs);                      // to use the printObsList to print it

							printObsList(obsList);
						}
						break;

					case ("trail"):
						if (data[1].equals("person"))
							objectType = ObjectType.PERSON;
						else if (data[1].equals("car"))
							objectType = ObjectType.CAR;
						else {
							System.out.println("invalid object");
							continue;
						}

						id = data[2];
						List<Obs> obsList = frontend.trace(TraceRequest.newBuilder()
								.setObjectType(objectType)
								.setId(id)
								.build()).getObsListList();

						printObsList(obsList);
						break;

					case ("ping"):
						if (data.length > 2) {
							System.out.println("too many arguments");
						}
						if (data.length == 2) {
							PingResponse response = frontend.ctrl_ping(PingRequest.newBuilder().setTxt(data[1]).build());
							System.out.print(response);
						}
						else {
							PingResponse response = frontend.ctrl_ping(PingRequest.newBuilder().setTxt("World").build());
							System.out.print(response);
						}
						break;

					case ("info"):
						if (data.length != 2) {
							System.out.println("invalid number of arguments");
						}
						else {
							try {
								CamInfoResponse response = frontend.camInfo(CamInfoRequest.newBuilder().setName(data[1]).build());
								System.out.println(response.getCoord1() + " " + response.getCoord2());
							}
							catch (StatusRuntimeException e) {
								System.out.println("**no results**");
							}
						}
						break;

					case ("clear"):
						frontend.ctrl_clear(ClearRequest.newBuilder().build());
						break;

					case ("help"):
						String help_message =
								"\t** Command Spot **\n" +
								"\t# DESCRIPTION: search the object observation with that id or partial id\n" +
								"\t# SYNTAX:\n" +
								"\tspot <object type> <id>/<partial id>\n" +
								"\tobject type: the type of the object that is observed (car or person)\n" +
								"\tid: the sequence of chars that identifies the object\n" +
								"\tid [car] : letters or numbers. three blocks of two digits (of the same type).\n" +
								"\t           maximum two blocks of the same type. (ex: AA11BB)\n" +
								"\tid [person] : integer with a minimum of 63 bits size. (ex: 1234)\n" +
								"\tpartial id: part of an id (with an asterisk) representing any object with that partial id\n" +
								"\n" +
								"\t** Command Trace **\n" +
								"\t# DESCRIPTION: search the path taken by the object with that exact id\n" +
								"\t# SYNTAX:\n" +
								"\ttrail <object type> <id>\n" +
								"\tobject type: car, person\n" +
								"\tid: the sequence of chars that identifies the object\n" +
								"\tid [car] : letters or numbers. three blocks of two digits (of the same type).\n" +
								"\t           maximum two blocks of the same type. (ex: AA11BB)\n" +
								"\tid [person] : integer with a minimum of 63 bits size. (ex: 1234)\n" +
								"\n" +
								"\t** Command Ping **\n" +
								"\t# DESCRIPTION: send a message and waits for the server answer\n" +
								"\t# SYNTAX:\n" +
								"\tping <text>\n" +
								"\ttext: optional argument. text to send to the server\n" +
								"\n" +
								"\t** Command Clear **\n" +
								"\t# DESCRIPTION: clear the server state\n" +
								"\t# SYNTAX:\n" +
								"\tclear\n" +
								"\n" +
								"\t** Command Help **\n" +
								"\t# DESCRIPTION: displays the help message\n" +
								"\t# SYNTAX:\n" +
								"\thelp";
						System.out.println(help_message);

						break;
					default:
						System.out.println("invalid command");
						break;
				}
			}
		}
		catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		frontend.close();
	}

	private static void printObsList(List<Obs> obsList) {
		String objType = "";
		if (!obsList.isEmpty()) {
			for (Obs obs : obsList) {
				if (obs.getObjectType() == ObjectType.PERSON) {
					objType = "person";
				}
				else if (obs.getObjectType() == ObjectType.CAR) {
					objType = "car";
				}

				if (obs.getObjectId().isEmpty()) { // no results found
					System.out.println("**no results**");
					return;
				}
				LocalDateTime dateTime = Instant.ofEpochSecond(obs.getTimestamp()
						.getSeconds())
						.atZone(ZoneId.of("Europe/Lisbon"))
						.toLocalDateTime();
				System.out.println(objType + ","
						+ obs.getObjectId() + ","
						+ dateTime + ","
						+ obs.getCameraName() + ","
						+ obs.getCoord1() + ","
						+ obs.getCoord2());
			}
		}
		else {
			System.out.println("**no results**");
		}
	}
}
