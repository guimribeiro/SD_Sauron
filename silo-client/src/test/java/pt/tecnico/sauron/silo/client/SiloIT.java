package pt.tecnico.sauron.silo.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;

import pt.tecnico.sauron.silo.grpc.Silo.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SiloIT extends BaseIT {
	private static final String host = "localhost";
	private static final String port = "2181";
	private static final String serverId = "1";
	private static final String camName = "cam1";
	private static final Double coord1 = 1.111111111111;
	private static final Double coord2 = 2.222222222222;
	private static final String personId1 = "123";
	private static final String personId2 = "123123";
	private static final String personId3 = "1223";
	private static final String carId1 = "AA12AA";
	private static final String carId2 = "AA12AB";
	private static final LocalDateTime dateTime = LocalDateTime.of(2020, Month.JANUARY, 1, 10, 0, 0);
	private static final LocalDateTime dateTime2 = LocalDateTime.of(2020, Month.JANUARY, 1, 12, 0, 0);
	private static final LocalDateTime dateTime3 = LocalDateTime.of(2020, Month.JANUARY, 1, 14, 0, 0);
	private static final Timestamp timestamp = Timestamp.newBuilder().
											setSeconds(dateTime.atZone(ZoneId.of("Europe/Lisbon")).
											toInstant().getEpochSecond()).build();
	private static final Timestamp timestamp2 = Timestamp.newBuilder().
											setSeconds(dateTime2.atZone(ZoneId.of("Europe/Lisbon")).
											toInstant().getEpochSecond()).build();
	private static final Cam cam1 = Cam.newBuilder().setName(camName).setCoord1(coord1).setCoord2((coord2)).build();
	private static final Obs obsPerson1 = Obs.newBuilder().
			setTimestamp(timestamp).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.PERSON).
			setObjectId(personId1).build();
	private static final Obs obsPerson2 = Obs.newBuilder().
			setTimestamp(timestamp).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.PERSON).
			setObjectId(personId2).build();
	private static final Obs obsPerson3 = Obs.newBuilder().
			setTimestamp(timestamp).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.PERSON).
			setObjectId(personId3).build();
	private static final Obs obsCar1 = Obs.newBuilder().
			setTimestamp(timestamp).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.CAR).
			setObjectId(carId1).build();


	private static final Obs obsCar2 = Obs.newBuilder().
			setTimestamp(timestamp2).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.CAR).
			setObjectId(carId1).build();

	private static final Obs obs1Car3 = Obs.newBuilder().
			setTimestamp(timestamp).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.CAR).
			setObjectId(carId1).build();

	private static final Obs obs2Car3 = Obs.newBuilder().
			setTimestamp(timestamp2).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.CAR).
			setObjectId(carId1).build();

	private static final Timestamp timestamp3 = Timestamp.newBuilder().
			setSeconds(dateTime3.atZone(ZoneId.of("Europe/Lisbon")).
					toInstant().getEpochSecond()).build();

	private static final Obs obs3Car3 = Obs.newBuilder().
			setTimestamp(timestamp3).
			setCameraName(camName).
			setCoord1(coord1).
			setCoord2(coord2).
			setObjectType(ObjectType.CAR).
			setObjectId(carId1).build();



	// static members
	// TODO	
	
	
	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){
		
	}

	@AfterAll
	public static void oneTimeTearDown() {
		
	}

	private Frontend frontend;
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
		try {
			frontend = new Frontend(host, port, "1");
		} catch (ZKNamingException e) {
			System.out.println(e.getMessage());
			return;
		}
		frontend.ctrl_clear(ClearRequest.newBuilder().build());
	}
	
	@AfterEach
	public void tearDown() {
		frontend.close();
		frontend = null;
	}
		
	// tests
	@Test
	public void testRegularCamJoinAndCamInfo() {
		CamJoinRequest camJoinRequest = CamJoinRequest.newBuilder().
				setName(camName).
				setCoord1(coord1).
				setCoord2(coord2).build();
		frontend.camJoin(camJoinRequest);
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder().setName(camName).build();
		CamInfoResponse camInforesponse = frontend.camInfo(camInfoRequest);
		assertAll(
				() -> assertEquals(camInforesponse.getCoord1(), coord1),
				() -> assertEquals(camInforesponse.getCoord2(), coord2)
		);
	}

	@Test
	public void testInvalidInputCamJoinAndCamInfo() {
		assertAll(
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(CamJoinRequest.newBuilder().
						setName("").
						setCoord1(coord1).
						setCoord2(coord2).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(CamInfoRequest.newBuilder().setName("").build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(CamJoinRequest.newBuilder().
						setName(camName).
						setCoord1(100).
						setCoord2(coord2).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(CamInfoRequest.newBuilder().setName(camName).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(CamJoinRequest.newBuilder().
						setName(camName).
						setCoord1(coord1).
						setCoord2(100).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(CamInfoRequest.newBuilder().setName(camName).build()))
		);
	}

	@Test
	public void testDuplicateCamJoinAndCamInfo() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		CamJoinRequest camJoinRequest = CamJoinRequest.newBuilder().
				setName(camName).
				setCoord1(coord1).
				setCoord2(coord2).build();
		frontend.camJoin(camJoinRequest);
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder().setName(camName).build();
		CamInfoResponse camInforesponse = frontend.camInfo(camInfoRequest);
		assertAll(
				() -> assertEquals(camInforesponse.getCoord1(), coord1),
				() -> assertEquals(camInforesponse.getCoord2(), coord2)
		);
	}

	@Test
	public void testCamNameAlreadyInUseCamJoinCamInfo() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		CamJoinRequest camJoinRequest = CamJoinRequest.newBuilder().
				setName(camName).
				setCoord1(coord2).
				setCoord2(coord2).build();
		assertAll(
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)),
				() -> assertEquals(
						(frontend.camInfo(CamInfoRequest.newBuilder().setName(camName).build()).getCoord1()), coord1)
		);
	}

	@Test
	public void testReportInvalidInput() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		Obs.Builder obs = Obs.newBuilder().
				setObjectType(ObjectType.PERSON).
				setObjectId(personId2);
		ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(camName);
		assertAll(
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.setName("").
						addObsList(obs.build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectId(carId1).build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectId("-1").build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectId("" + Math.pow(2, 64)).build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectType(ObjectType.CAR).setObjectId(carId1).build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectType(ObjectType.CAR).setObjectId(personId1).build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectType(ObjectType.CAR).setObjectId("AAAAAA").build()).build())),
				() -> assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest.
						addObsList(obs.setObjectType(ObjectType.CAR).setObjectId("1234567").build()).build()))
		);
	}

	@Test
	public void testSingleReportWithTrack() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(camName);
		frontend.report(reportRequest.addObsList(obsPerson1).build());
		Obs obsResult = frontend.track(TrackRequest.newBuilder().
				setObjectType(ObjectType.PERSON).
				setId(personId1).build()).
				getObs();
		assertAll(
				() -> assertEquals(obsResult.getCameraName(), camName),
				() -> assertEquals(obsResult.getCoord1(), coord1),
				() -> assertEquals(obsResult.getCoord2(), coord2),
				() -> assertEquals(obsResult.getObjectId(), personId1),
				() -> assertEquals(obsResult.getObjectType(), ObjectType.PERSON)
		);
	}

	@Test
	public void testSeveralReportWithTrack() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(camName);
		frontend.report(reportRequest.addObsList(obsPerson1).addObsList(obsCar1).build());
		Obs obsResult = frontend.track(TrackRequest.newBuilder().
				setObjectType(ObjectType.CAR).
				setId(carId1).build()).
				getObs();
		assertAll(
				() -> assertEquals(obsResult.getCameraName(), camName),
				() -> assertEquals(obsResult.getCoord1(), coord1),
				() -> assertEquals(obsResult.getCoord2(), coord2),
				() -> assertEquals(obsResult.getObjectId(), carId1),
				() -> assertEquals(obsResult.getObjectType(), ObjectType.CAR)
		);
	}

	@Test
	public void testInvalidInputTrack() {


		assertAll(
				()-> assertTrue(frontend.track(TrackRequest.newBuilder()
						.setObjectType(obsCar1.getObjectType())
						.setId("")
						.build()).getObs().getObjectId().isEmpty())	,
				()-> assertTrue(frontend.track(TrackRequest.newBuilder()
						.setObjectType(obsCar1.getObjectType())
						.setId(obsPerson1.getObjectId())
						.build()).getObs().getObjectId().isEmpty())
		);
	}
	@Test
	public void testValidInputTrack(){

		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		frontend.ctrl_init(InitRequest.newBuilder().addObsList(obsCar1).build());
		frontend.ctrl_init(InitRequest.newBuilder().addObsList(obsCar2).build());
		TrackResponse trackRequest = frontend.track(TrackRequest.newBuilder()
				.setObjectType(obsCar2.getObjectType())
				.setId(obsCar2.getObjectId()).build());

		assertAll(
				()->	assertEquals(trackRequest.getObs().getObjectType(), obsCar2.getObjectType()),
				()->	assertEquals(trackRequest.getObs().getObjectId(), obsCar2.getObjectId()),
				()->	assertEquals(trackRequest.getObs().getTimestamp(), obsCar2.getTimestamp())
		);

	}
	@Test
	public void testInvalidInputTrace(){
		assertAll(
				()-> assertTrue(frontend.trace(TraceRequest.newBuilder()
						.setObjectType(obsCar1.getObjectType())
						.setId("")
						.build()).getObsListList().isEmpty()),
				()-> assertTrue(frontend.trace(TraceRequest.newBuilder()
						.setObjectType(obsCar1.getObjectType())
						.setId(obsPerson1.getObjectId())
						.build()).getObsListList().isEmpty())
		);
	}

	@Test
	public void testRegularTrackMatch() {
		frontend.ctrl_init(InitRequest.newBuilder().
				addCamList(cam1).
				addObsList(obsPerson1).
				addObsList(obsPerson2).
				addObsList(obsPerson3).build());
		TrackMatchRequest.Builder trackMatchRequest = TrackMatchRequest.newBuilder().
				setObjectType(ObjectType.PERSON);
		List<Obs> obsList0 = frontend.trackMatch(trackMatchRequest.
				setId("4*").build()).
				getObsListList();
		List<Obs> obsList1 = frontend.trackMatch(trackMatchRequest.
				setId("12*").build()).
				getObsListList();
		List<Obs> obsList2 = frontend.trackMatch(trackMatchRequest.
				setId("*123").build()).
				getObsListList();
		List<Obs> obsList3 = frontend.trackMatch(trackMatchRequest.
				setId("12*23").build()).
				getObsListList();
		assertAll(
				() -> assertEquals(obsList0.size(), 0),
				//expected: empty
				() -> assertEquals(obsList1.size() ,3),
				//expected: 123,123123,1223
				() -> assertEquals(obsList2.size(), 2),
				//expected: 123, 123
				() -> assertEquals(obsList3.size(), 2)
				//expected: 123123, 1223
		);
	}

	@Test
	public void testInvalidInputTrackMatch() {
		TrackMatchRequest.Builder trackMatchRequest = TrackMatchRequest.newBuilder().
				setObjectType(ObjectType.CAR);
		assertAll(
				() -> assertThrows(RuntimeException.class, ()-> frontend.trackMatch(trackMatchRequest.setId("*").build())),
				() -> assertThrows(RuntimeException.class, ()-> frontend.trackMatch(trackMatchRequest.setId("*1*").build()))
		);
	}

	@Test
	public void testValidInputTrace() {
		frontend.ctrl_init(InitRequest.newBuilder().addCamList(cam1).build());
		frontend.ctrl_init(InitRequest.newBuilder().addObsList(obs1Car3).build());
		frontend.ctrl_init(InitRequest.newBuilder().addObsList(obs2Car3).build());
		frontend.ctrl_init(InitRequest.newBuilder().addObsList(obs3Car3).build());
		List<Obs> obs = frontend.trace(TraceRequest.newBuilder()
				.setObjectType(obsCar1.getObjectType())
				.setId(obsCar1.getObjectId()).build()).getObsListList();
		assertAll(
				()->    assertEquals(obs.size(), 3),
				()->    assertTrue(obs.get(0).getTimestamp().getSeconds() > obs.get(1).getTimestamp().getSeconds()),
				()->    assertTrue(obs.get(1).getTimestamp().getSeconds() > obs.get(2).getTimestamp().getSeconds())
		);
	}
}
