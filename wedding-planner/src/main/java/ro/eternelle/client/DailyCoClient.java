package ro.eternelle.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import ro.eternelle.dto.videocall.DailyRoomRequest;
import ro.eternelle.dto.videocall.DailyRoomResponse;

@RegisterRestClient(configKey = "ro.eternelle.client.DailyCoClient")
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DailyCoClient {

    @POST
    DailyRoomResponse createRoom(DailyRoomRequest request);

    @DELETE
    @Path("/{roomName}")
    void deleteRoom(@PathParam("roomName") String roomName);
}
