package ro.eternelle.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import ro.eternelle.client.ResendClient;
import ro.eternelle.dto.email.SendEmailRequest;

import java.util.List;

@ApplicationScoped
public class EmailService {

    @Inject
    @RestClient
    ResendClient resendClient;

    @ConfigProperty(name = "resend.api.key")
    String apiKey;

    @ConfigProperty(name = "app.email.from", defaultValue = "Eternelle <noreply@eternelle.ro>")
    String fromAddress;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    // ── Verification email ──────────────────────────────────────────────────

    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Verify your email — Eternelle</title>
                </head>
                <body style="margin:0;padding:0;background:#f7f4ef;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f4ef;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.07);">
                          <!-- Header -->
                          <tr>
                            <td style="background:#1c1c1c;padding:32px 40px;text-align:center;">
                              <span style="font-size:28px;font-weight:800;color:#c9a84c;letter-spacing:-1px;">Eternelle</span>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#1c1c1c;">Verify your email address</h1>
                              <p style="margin:0 0 24px;font-size:15px;color:#555;line-height:1.6;">
                                Welcome to Eternelle! Click the button below to confirm your email address and activate your account.
                              </p>
                              <a href="%s"
                                 style="display:inline-block;background:#c9a84c;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:10px;font-size:15px;font-weight:600;">
                                Verify my email
                              </a>
                              <p style="margin:24px 0 0;font-size:13px;color:#999;line-height:1.6;">
                                This link expires in 24 hours. If you didn't create an Eternelle account, you can safely ignore this email.
                              </p>
                              <p style="margin:12px 0 0;font-size:12px;color:#bbb;">
                                Or copy this link: <a href="%s" style="color:#c9a84c;word-break:break-all;">%s</a>
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 40px;border-top:1px solid #f0ece5;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#bbb;">© 2026 Eternelle · Romania's wedding platform</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(verifyUrl, verifyUrl, verifyUrl);

        send(to, "Verify your email — Eternelle", html);
    }

    // ── Password reset email ────────────────────────────────────────────────

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Reset your password — Eternelle</title>
                </head>
                <body style="margin:0;padding:0;background:#f7f4ef;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f7f4ef;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.07);">
                          <!-- Header -->
                          <tr>
                            <td style="background:#1c1c1c;padding:32px 40px;text-align:center;">
                              <span style="font-size:28px;font-weight:800;color:#c9a84c;letter-spacing:-1px;">Eternelle</span>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#1c1c1c;">Reset your password</h1>
                              <p style="margin:0 0 24px;font-size:15px;color:#555;line-height:1.6;">
                                We received a request to reset the password for your Eternelle account. Click the button below to choose a new password.
                              </p>
                              <a href="%s"
                                 style="display:inline-block;background:#c9a84c;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:10px;font-size:15px;font-weight:600;">
                                Reset password
                              </a>
                              <p style="margin:24px 0 0;font-size:13px;color:#999;line-height:1.6;">
                                This link expires in 1 hour. If you didn't request a password reset, you can safely ignore this email — your password won't change.
                              </p>
                              <p style="margin:12px 0 0;font-size:12px;color:#bbb;">
                                Or copy this link: <a href="%s" style="color:#c9a84c;word-break:break-all;">%s</a>
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 40px;border-top:1px solid #f0ece5;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#bbb;">© 2026 Eternelle · Romania's wedding platform</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(resetUrl, resetUrl, resetUrl);

        send(to, "Reset your password — Eternelle", html);
    }

    // ── Internal helper ─────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        SendEmailRequest req = new SendEmailRequest();
        req.from = fromAddress;
        req.to = List.of(to);
        req.subject = subject;
        req.html = html;
        resendClient.send("Bearer " + apiKey, req);
    }
}
