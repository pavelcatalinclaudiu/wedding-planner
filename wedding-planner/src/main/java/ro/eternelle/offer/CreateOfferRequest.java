package ro.eternelle.offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreateOfferRequest {
    public UUID       leadId;
    public UUID       conversationId;
    public String     packageDetails;
    public BigDecimal price;
    public LocalDate  expiresAt;
}
