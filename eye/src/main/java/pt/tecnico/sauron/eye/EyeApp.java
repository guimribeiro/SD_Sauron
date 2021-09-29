package pt.tecnico.sauron.eye;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.Frontend;
import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;

public class EyeApp {

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		String serverId = null;

		// validate arguments
		if (args.length == 6) {
			if (!args[5].matches("[0-9]+")) {
				System.out.println("invalid serverId format");
				return;
			}
			serverId = args[5];
		}
		else if (args.length != 5) {
			System.out.println("invalid number of arguments");
			return;
		}
		if (!args[1].matches("[0-9]+")) {
			System.out.println("invalid port format");
			return;
		}
		else if (args[2].length() < 3 || args[2].length() > 15) {
			System.out.println("invalid camera name");
			return;
		}
		else if (!args[3].matches("^([+-]?(\\d+\\.)?\\d+)$") ||
				!args[4].matches("^([+-]?(\\d+\\.)?\\d+)$")) {
			System.out.println("invalid coordinates");
			return;
		}

		String zooHost = args[0];
		String zooPort = args[1];
		String name = args[2];
		String coord1 = args[3];
		String coord2 = args[4];

		Frontend frontend = connect(zooHost, zooPort, serverId, name, coord1, coord2);

		double coordinate1 = Double.parseDouble(coord1);
		double coordinate2 = Double.parseDouble(coord2);
		information(frontend, name, coordinate1, coordinate2);
	}

	private static Frontend connect(String zooHost, String zooPort, String serverId, String name, String coord1, String coord2) {
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
		try {
			frontend.camJoin(CamJoinRequest.newBuilder()
					.setName(name)
					.setCoord1(Double.parseDouble(coord1))
					.setCoord2(Double.parseDouble(coord2))
					.build());
		} catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
			exit(0);
		}
		return frontend;
	}

	private static void information(Frontend frontend, String name, double coord1, double coord2) {
		String line;
		List<Obs> obsList = new ArrayList<>();
		try (Scanner scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					String data[] = line.split(",");

					Obs.Builder obsObj;
					String id;

					if (line.isBlank()) { // ignore blank lines
						break;
					}
					if (line.charAt(0) == '#') { // ignore a line that starts with '#'
						continue;
					}
					switch (data[0]) {
						case ("zzz"): // sleep for a while
							int time = Integer.parseInt(data[1]);
							try {
								Thread.sleep(time);
							}
							catch (InterruptedException e) {
								System.out.println(e.getMessage());
							}
							break;

						case ("person"): // add person
							obsObj = Obs.newBuilder();
							id = data[1];
							obsObj.setCameraName(name)
									.setCoord1(coord1)
									.setCoord2(coord2)
									.setObjectId(id)
									.setObjectType(ObjectType.PERSON);
							obsList.add(obsObj.build());
							break;

						case ("car"): // add car
							obsObj = Obs.newBuilder();
							id = data[1];
							obsObj.setCameraName(name)
									.setCoord1(coord1)
									.setCoord2(coord2)
									.setObjectId(id)
									.setObjectType(ObjectType.CAR);
							obsList.add(obsObj.build());
							break;

						default:
							System.out.println("invalid object");
					}
				}
				try {
					frontend.report(ReportRequest.newBuilder()
							.setName(name)
							.addAllObsList(obsList)
							.build());
					obsList.clear();
				} catch (StatusRuntimeException e) {
					System.out.println(e.getMessage());
					obsList.clear();
				}
			}
		}
		catch (StatusRuntimeException e) {
			System.out.println(e.getMessage());
		}
		frontend.close();
	}
}
