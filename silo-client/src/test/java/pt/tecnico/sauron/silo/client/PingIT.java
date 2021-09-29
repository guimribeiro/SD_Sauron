package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.Test;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.grpc.Status.INVALID_ARGUMENT;


public class PingIT {

    @Test
    public void pingOKTest() {
        Frontend frontend;
        try {
            frontend = new Frontend("localhost","2181", "1");
        } catch (ZKNamingException e) {
            System.out.println(e.getMessage());
            return;
        }
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setTxt("friend").build();
        Silo.PingResponse response = frontend.ctrl_ping(request);
        assertEquals("Hello friend!", response.getTxt());
        frontend.close();
    }

    @Test
    public void emptyPingTest() {
        Frontend frontend;
        try {
            frontend = new Frontend("localhost","2181", "1");
        } catch (ZKNamingException e) {
            System.out.println(e.getMessage());
            return;
        }
        Silo.PingRequest request = Silo.PingRequest.newBuilder().setTxt("").build();
        assertEquals(
                INVALID_ARGUMENT.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.ctrl_ping(request))
                        .getStatus()
                        .getCode());
        frontend.close();
    }
}
