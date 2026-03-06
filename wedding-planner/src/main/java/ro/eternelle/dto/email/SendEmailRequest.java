package ro.eternelle.dto.email;

import java.util.List;

public class SendEmailRequest {

    public String from;
    public List<String> to;
    public String subject;
    public String html;
    public String text;
}
