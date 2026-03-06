package ro.eternelle.messaging;

public class ThreadParticipantDTO {

    public String id;       // ThreadParticipant.id – used when removing a member
    public String userId;
    public String name;     // businessName for vendors, "A & B" for couples
    public String role;     // VENDOR | COUPLE
    public String avatarUrl;

    public static ThreadParticipantDTO from(ThreadParticipant p,
                                            String name,
                                            String role,
                                            String avatarUrl) {
        ThreadParticipantDTO dto = new ThreadParticipantDTO();
        dto.id        = p.id.toString();
        dto.userId    = p.user.id.toString();
        dto.name      = name;
        dto.role      = role;
        dto.avatarUrl = avatarUrl;
        return dto;
    }
}
