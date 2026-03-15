package ro.eternelle.website;

import ro.eternelle.couple.CoupleProfile;

import java.time.format.DateTimeFormatter;

public class WeddingWebsiteDTO {

    // ── Authenticated couple response ──────────────────────────────────────

    public static class Response {
        public String id;
        public String heroMessage;
        public String coverPhotoUrl;
        public String story;
        public String ceremonyDate;
        public String ceremonyLocation;
        public String receptionDate;
        public String receptionLocation;
        public boolean published;
        public int rsvpsSubmitted;
        public int visitorCount;
        public String subdomain;
        public boolean subdomainCustomized;

        public static Response from(WeddingWebsite w) {
            Response r = new Response();
            r.id                = w.id.toString();
            r.heroMessage       = w.heroMessage;
            r.coverPhotoUrl     = w.coverPhotoUrl;
            r.story             = w.story;
            r.ceremonyDate      = w.ceremonyDate;
            r.ceremonyLocation  = w.ceremonyLocation;
            r.receptionDate     = w.receptionDate;
            r.receptionLocation = w.receptionLocation;
            r.published         = w.published;
            r.rsvpsSubmitted    = w.rsvpsSubmitted;
            r.visitorCount      = w.visitorCount;
            r.subdomain         = w.couple.websiteSubdomain;
            r.subdomainCustomized = w.couple.subdomainCustomized;
            return r;
        }
    }

    // ── Public page response (includes couple profile data) ───────────────

    public static class PublicResponse {
        public String heroMessage;
        public String coverPhotoUrl;
        public String story;
        public String ceremonyDate;
        public String ceremonyLocation;
        public String receptionDate;
        public String receptionLocation;
        // From couple profile
        public String partner1Name;
        public String partner2Name;
        public String weddingDate;
        public String weddingLocation;
        public String couplePhoto;
        public String subdomain;

        public static PublicResponse from(WeddingWebsite w) {
            PublicResponse r = new PublicResponse();
            r.heroMessage       = w.heroMessage;
            r.coverPhotoUrl     = w.coverPhotoUrl;
            r.story             = w.story;
            r.ceremonyDate      = w.ceremonyDate;
            r.ceremonyLocation  = w.ceremonyLocation;
            r.receptionDate     = w.receptionDate;
            r.receptionLocation = w.receptionLocation;

            CoupleProfile cp = w.couple;
            r.partner1Name   = cp.partner1Name;
            r.partner2Name   = cp.partner2Name;
            r.weddingDate    = cp.weddingDate != null
                    ? cp.weddingDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : null;
            r.weddingLocation = cp.weddingLocation;
            r.couplePhoto     = cp.profilePicture;
            r.subdomain       = cp.websiteSubdomain;
            return r;
        }
    }

    // ── Update request ─────────────────────────────────────────────────────

    public static class UpdateRequest {
        public String subdomain;
        public String heroMessage;
        public String story;
        public String ceremonyDate;
        public String ceremonyLocation;
        public String receptionDate;
        public String receptionLocation;
        public Boolean published;
    }
}
