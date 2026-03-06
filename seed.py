#!/usr/bin/env python3
"""
seed.py  –  Vœux wedding-planner demo data seeder
=================================================
Requirements:
    pip install mysql-connector-python bcrypt

Usage:
    python seed.py            # insert data (skip if already present)
    python seed.py --reset    # wipe all tables first, then insert
"""

import sys
import uuid
import json
from datetime import date, datetime, timedelta

try:
    import bcrypt
    import mysql.connector
except ImportError:
    print("Missing dependencies. Run:  pip install mysql-connector-python bcrypt")
    sys.exit(1)

# ─── Database connection ───────────────────────────────────────────────────────
DB = dict(
    host               = "localhost",
    port               = 3306,
    database           = "wedding_planner_db",
    user               = "root",
    password           = "root",
    use_pure           = True,   # avoids C-extension segfault on some MySQL versions
    connection_timeout = 10,
)

RESET = "--reset" in sys.argv

# ─── Helpers ──────────────────────────────────────────────────────────────────
def uid() -> str:
    return str(uuid.uuid4())

def pw(plain: str) -> str:
    """BCrypt hash compatible with Quarkus Elytron ($2a prefix)."""
    hashed = bcrypt.hashpw(plain.encode(), bcrypt.gensalt(rounds=10))
    return hashed.decode().replace("$2b$", "$2a$")

def ts(delta_hours: int = 0) -> str:
    return (datetime.now() - timedelta(hours=delta_hours)).strftime("%Y-%m-%d %H:%M:%S")

WEDDING_DATE = date(2026, 9, 14)
NOW = ts()
DEFAULT_PASS = pw("password123")

# ─── Pre-generate all UUIDs so FK references are consistent ───────────────────
# Users
u_couple = uid(); u_photo  = uid(); u_video  = uid()
u_floral = uid(); u_cater  = uid(); u_venue  = uid()
u_cake   = uid(); u_band   = uid(); u_transp = uid()

# Profiles
cp       = uid()   # couple profile
vp_photo = uid(); vp_video  = uid(); vp_floral = uid()
vp_cater = uid(); vp_venue  = uid(); vp_cake   = uid()
vp_band  = uid(); vp_transp = uid()

# Deals
d_photo  = uid(); d_video  = uid(); d_floral = uid(); d_cater = uid()
d_venue  = uid(); d_cake   = uid(); d_band   = uid(); d_transp = uid()

# Quotes
q_photo = uid(); q_cater = uid(); q_band = uid()

# Bookings
b_photo = uid(); b_video = uid(); b_band = uid()

# Reviews
r_photo = uid(); r_band = uid()

# Thread + messages
thread  = uid()
tp_c = uid(); tp_v = uid()
m1 = uid(); m2 = uid(); m3 = uid()

# ─── Main ──────────────────────────────────────────────────────────────────────
def main():
    conn = mysql.connector.connect(**DB)
    cur  = conn.cursor()

    # ── Optional reset ──────────────────────────────────────────────────────
    if RESET:
        print("⚠  Wiping all tables …")
        cur.execute("SET FOREIGN_KEY_CHECKS = 0")
        for t in [
            "reviews", "bookings", "quotes", "deal_status_history", "deals",
            "messages", "thread_participants", "threads",
            "budget_items", "checklist_items", "guests",
            "vendor_partners", "vendor_photos",
            "notifications", "subscriptions", "video_calls",
            "vendor_profiles", "couple_profiles", "users",
        ]:
            cur.execute(f"DELETE FROM `{t}`")
        cur.execute("SET FOREIGN_KEY_CHECKS = 1")
        conn.commit()
        print("   Done.\n")

    # ── 1. USERS ────────────────────────────────────────────────────────────
    print("→ users")
    cur.executemany(
        "INSERT IGNORE INTO users (id, email, password_hash, role, email_verified) "
        "VALUES (%s, %s, %s, %s, %s)",
        [
            (u_couple, "couple@demo.ro",     DEFAULT_PASS, "COUPLE", True),
            (u_photo,  "photo@demo.ro",      DEFAULT_PASS, "VENDOR", True),
            (u_video,  "video@demo.ro",      DEFAULT_PASS, "VENDOR", True),
            (u_floral, "florist@demo.ro",    DEFAULT_PASS, "VENDOR", True),
            (u_cater,  "catering@demo.ro",   DEFAULT_PASS, "VENDOR", True),
            (u_venue,  "venue@demo.ro",      DEFAULT_PASS, "VENDOR", True),
            (u_cake,   "cake@demo.ro",       DEFAULT_PASS, "VENDOR", True),
            (u_band,   "band@demo.ro",       DEFAULT_PASS, "VENDOR", True),
            (u_transp, "transport@demo.ro",  DEFAULT_PASS, "VENDOR", True),
        ],
    )

    # ── 2. COUPLE PROFILE ───────────────────────────────────────────────────
    print("→ couple_profiles")
    cur.execute(
        "INSERT IGNORE INTO couple_profiles "
        "(id, user_id, partner_1_name, partner_2_name, wedding_date, "
        " wedding_location, guest_count, total_budget, plan, website_subdomain) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        (cp, u_couple, "Ana Ionescu", "Mihai Constantin",
         str(WEDDING_DATE), "Sinaia, România", 120, 55000.00, "PREMIUM", "ana-si-mihai"),
    )

    # ── 3. VENDOR PROFILES ──────────────────────────────────────────────────
    print("→ vendor_profiles")
    cur.executemany(
        "INSERT IGNORE INTO vendor_profiles "
        "(id, user_id, business_name, category, city, description, "
        " base_price, tier, avg_rating, review_count, is_verified, is_active) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        [
            (vp_photo,  u_photo,  "Lumina Studios",
             "PHOTOGRAPHER",   "București",
             "Captivating wedding photography with a cinematic documentary style. "
             "We blend into your day and capture every raw, genuine emotion.",
             3500.00, "PROFESSIONAL", 4.9, 42, True, True),

            (vp_video,  u_video,  "Vision Films",
             "VIDEOGRAPHER",   "București",
             "Cinematic wedding films that tell your unique love story. "
             "Specialising in same-day edits and destination weddings.",
             2800.00, "STARTER", 4.6, 18, True, True),

            (vp_floral, u_floral, "La Fleur Studio",
             "FLORIST",        "Sinaia",
             "Lush, romantic floral arrangements and complete venue decoration "
             "using locally sourced seasonal blooms.",
             1200.00, "PROFESSIONAL", 4.8, 31, True, True),

            (vp_cater,  u_cater,  "Saveur Catering",
             "CATERER",        "Brașov",
             "Award-winning catering service blending French technique with "
             "seasonal Romanian cuisine. Up to 300 guests.",
             8500.00, "PREMIUM", 4.7, 57, True, True),

            (vp_venue,  u_venue,  "Eden Garden Estate",
             "VENUE",          "Sinaia",
             "Breathtaking mountain-view estate set across 5 hectares. "
             "Indoor and outdoor spaces for up to 200 guests.",
             12000.00, "PREMIUM", 4.9, 89, True, True),

            (vp_cake,   u_cake,   "Bella Cake Studio",
             "CAKE",           "Ploiești",
             "Bespoke wedding cakes, dessert stations and sweet tables. "
             "Fully custom designs with premium ingredients.",
             650.00, "STARTER", 4.5, 23, False, True),

            (vp_band,   u_band,   "Ritm & Note Orchestra",
             "BAND",           "București",
             "Live 8-piece wedding orchestra covering jazz, pop, soul and "
             "Romanian folk. Also providing DJ setup for the after-party.",
             2200.00, "PROFESSIONAL", 4.7, 35, True, True),

            (vp_transp, u_transp, "Elegance Transport",
             "TRANSPORTATION", "Brașov",
             "Luxury fleet of vintage cars, modern limousines and wedding coaches. "
             "Full-day hire available with professional chauffeurs.",
             450.00, "FREE", 4.3, 12, False, True),
        ],
    )

    # ── 4. DEALS ────────────────────────────────────────────────────────────
    print("→ deals")
    cur.executemany(
        "INSERT IGNORE INTO deals (id, couple_id, vendor_id, status, sealed_at) "
        "VALUES (%s,%s,%s,%s,%s)",
        [
            (d_photo,  cp, vp_photo,  "CONFIRMED",      NOW),
            (d_video,  cp, vp_video,  "DEPOSIT_PAID",   None),
            (d_floral, cp, vp_floral, "QUOTE_ACCEPTED", None),
            (d_cater,  cp, vp_cater,  "QUOTE_SENT",     None),
            (d_venue,  cp, vp_venue,  "CONVERSATION",   None),
            (d_cake,   cp, vp_cake,   "ENQUIRY",        None),
            (d_band,   cp, vp_band,   "CONFIRMED",      NOW),
            (d_transp, cp, vp_transp, "ENQUIRY",        None),
        ],
    )

    # ── 5. QUOTES ───────────────────────────────────────────────────────────
    print("→ quotes")
    expires = (datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S")
    cur.executemany(
        "INSERT IGNORE INTO quotes "
        "(id, deal_id, package_name, total_price, deposit_amount, items, "
        " status, expires_at, accepted_at) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        [
            (q_photo, d_photo, "Lumina Full-Day Package", 3500.00, 700.00,
             json.dumps([
                 {"description": "Full-day coverage (10 hours)", "quantity": 1, "unitPrice": 2500.00},
                 {"description": "Engagement session",           "quantity": 1, "unitPrice":  500.00},
                 {"description": "Online gallery (1 year)",      "quantity": 1, "unitPrice":  500.00},
             ]),
             "ACCEPTED", expires, NOW),

            (q_cater, d_cater, "Saveur Classic Menu", 7600.00, 1520.00,
             json.dumps([
                 {"description": "3-course dinner per person", "quantity": 120, "unitPrice":  55.00},
                 {"description": "Welcome cocktail hour",      "quantity":   1, "unitPrice": 800.00},
                 {"description": "Midnight snack bar",         "quantity":   1, "unitPrice": 600.00},
             ]),
             "PENDING", expires, None),

            (q_band, d_band, "Ritm & Note Premium Night", 2200.00, 440.00,
             json.dumps([
                 {"description": "Live band performance (5 hours)", "quantity": 1, "unitPrice": 1800.00},
                 {"description": "DJ setup for after-party",        "quantity": 1, "unitPrice":  400.00},
             ]),
             "ACCEPTED", expires, NOW),
        ],
    )

    # ── 6. BOOKINGS ─────────────────────────────────────────────────────────
    print("→ bookings")
    wd = str(WEDDING_DATE)
    cur.executemany(
        "INSERT IGNORE INTO bookings "
        "(id, deal_id, quote_id, wedding_date, total_price, deposit_paid, "
        " deposit_paid_at, balance_due_at, notes) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        [
            (b_photo, d_photo, q_photo, wd, 3500.00, 700.00, NOW,
             str(date(2026, 8,  1)), "Full-day shoot. Ceremony starts 14:00 at Eden Garden."),

            (b_video, d_video, None, wd, 2800.00, 560.00, NOW,
             str(date(2026, 8,  1)), "Same-day edit highlight reel requested. Raw footage on USB."),

            (b_band,  d_band,  q_band, wd, 2200.00, 440.00, NOW,
             str(date(2026, 8, 15)), "Set break at 21:00. Final song: Perfect – Ed Sheeran."),
        ],
    )

    # ── 7. REVIEWS ──────────────────────────────────────────────────────────
    print("→ reviews")
    cur.executemany(
        "INSERT IGNORE INTO reviews "
        "(id, booking_id, couple_id, vendor_id, rating, comment, is_public) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s)",
        [
            (r_photo, b_photo, cp, vp_photo, 5.0,
             "Absolutely breathtaking photos! Lumina Studios captured every emotion perfectly. "
             "The gallery made us cry happy tears. Every single shot is a masterpiece. "
             "Highly recommend to every couple!",
             True),

            (r_band, b_band, cp, vp_band, 4.5,
             "Ritm & Note kept the dance floor packed all night long. "
             "Super professional — they learned our first-dance song perfectly and improvised beautifully. "
             "The DJ after-party was also excellent.",
             True),
        ],
    )

    # ── 8. GUESTS ───────────────────────────────────────────────────────────
    print("→ guests")
    cur.executemany(
        "INSERT IGNORE INTO guests "
        "(id, couple_id, first_name, last_name, email, phone, "
        " rsvp_status, plus_one, dietary_requirements, table_number, invite_sent) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        [
            (uid(), cp, "Elena",    "Ionescu",    "elena.ionescu@mail.ro",     "+40721000001", "CONFIRMED", False, None,           "3",  True),
            (uid(), cp, "Gheorghe", "Ionescu",    "gh.ionescu@mail.ro",        "+40721000002", "CONFIRMED", False, None,           "3",  True),
            (uid(), cp, "Cristina", "Constantin", "cris.c@mail.ro",            "+40721000003", "CONFIRMED", True,  None,           "4",  True),
            (uid(), cp, "Victor",   "Constantin", "victor.c@mail.ro",          "+40721000004", "CONFIRMED", False, None,           "4",  True),
            (uid(), cp, "Ioana",    "Popescu",    "ioana.p@mail.ro",           "+40721000005", "CONFIRMED", False, "Vegetarian",   "5",  True),
            (uid(), cp, "Andrei",   "Popescu",    "andrei.p@mail.ro",          "+40721000006", "CONFIRMED", True,  None,           "5",  True),
            (uid(), cp, "Maria",    "Dumitru",    "maria.d@mail.ro",           "+40721000007", "PENDING",   False, None,           None, True),
            (uid(), cp, "Radu",     "Dumitru",    "radu.d@mail.ro",            "+40721000008", "PENDING",   False, "Gluten-free",  None, True),
            (uid(), cp, "Alina",    "Munteanu",   "alina.m@mail.ro",           "+40721000009", "CONFIRMED", False, None,           "2",  True),
            (uid(), cp, "Bogdan",   "Munteanu",   "bogdan.m@mail.ro",          "+40721000010", "CONFIRMED", True,  None,           "2",  True),
            (uid(), cp, "Teodora",  "Popa",       "teodora.p@mail.ro",         "+40721000011", "DECLINED",  False, None,           None, True),
            (uid(), cp, "Sorin",    "Popa",       "sorin.p@mail.ro",           "+40721000012", "DECLINED",  False, None,           None, True),
            (uid(), cp, "Gabriela", "Stan",       "gabriela.s@mail.ro",        "+40721000013", "PENDING",   True,  "Vegan",        None, True),
            (uid(), cp, "Mihnea",   "Florescu",   "mihnea.f@mail.ro",          "+40721000014", "CONFIRMED", False, None,           "1",  True),
            (uid(), cp, "Camelia",  "Iacob",      "camelia.i@mail.ro",         "+40721000015", "CONFIRMED", False, None,           "1",  True),
        ],
    )

    # ── 9. BUDGET ITEMS ─────────────────────────────────────────────────────
    print("→ budget_items")
    cur.executemany(
        "INSERT IGNORE INTO budget_items "
        "(id, couple_id, category, name, estimated_cost, actual_cost, is_paid, vendor_name) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
        [
            (uid(), cp, "Venue",       "Eden Garden Estate",            12000.00, 12000.00, True,  "Eden Garden Estate"),
            (uid(), cp, "Catering",    "Food, Drinks & Service",         8500.00,  7600.00, False, "Saveur Catering"),
            (uid(), cp, "Photography", "Photography & Videography",      6300.00,  6300.00, True,  "Lumina Studios + Vision Films"),
            (uid(), cp, "Music",       "Live Band & DJ After-Party",     2200.00,  2200.00, True,  "Ritm & Note Orchestra"),
            (uid(), cp, "Florals",     "Flowers & Full Venue Decoration",1200.00,  None,    False, "La Fleur Studio"),
            (uid(), cp, "Cake",        "Wedding Cake & Dessert Station",  650.00,  None,    False, "Bella Cake Studio"),
            (uid(), cp, "Transport",   "Bridal Cars & Guest Coaches",     900.00,  None,    False, "Elegance Transport"),
            (uid(), cp, "Stationery",  "Save-the-dates, Invitations & Menus", 350.00, 310.00, True, "PrintShop SRL"),
        ],
    )

    # ── 10. CHECKLIST ITEMS ─────────────────────────────────────────────────
    print("→ checklist_items")
    completed_ts = ts(24 * 60)  # "2 months ago" placeholder
    checklist = [
        ("Book the venue",                 "Confirm Eden Garden Estate reservation and sign the contract.",
         "Venue",        date(2026,  1, 15), True,  completed_ts, 0),
        ("Book photographer",              "Sign contract with Lumina Studios and confirm package details.",
         "Photography",  date(2026,  1, 20), True,  completed_ts, 1),
        ("Send save-the-dates",            "Mail save-the-date cards to all 120 guests.",
         "Admin",        date(2026,  2,  1), True,  completed_ts, 2),
        ("Book caterer",                   "Confirm menu and headcount with Saveur Catering.",
         "Catering",     date(2026,  3,  1), False, None, 3),
        ("Send wedding invitations",       "Post formal invitations with RSVP deadline of 1 June.",
         "Admin",        date(2026,  4,  1), False, None, 4),
        ("Book florist",                   "Finalise floral arrangements and colour palette with La Fleur Studio.",
         "Florals",      date(2026,  4, 15), False, None, 5),
        ("Wedding dress fitting #1",       "First fitting at Atelier Bridal. Bring shoes and undergarments.",
         "Attire",       date(2026,  5,  1), False, None, 6),
        ("Book wedding cake",              "Confirm cake design, flavours and delivery with Bella Cake Studio.",
         "Catering",     date(2026,  5, 15), False, None, 7),
        ("Arrange guest accommodation",    "Block-book rooms at Hotel Sinaia for out-of-town guests.",
         "Logistics",    date(2026,  6,  1), False, None, 8),
        ("Confirm band setlist",           "Send must-play and do-not-play lists to Ritm & Note Orchestra.",
         "Music",        date(2026,  7,  1), False, None, 9),
        ("Wedding dress fitting #2",       "Final fitting — confirm all alterations are complete.",
         "Attire",       date(2026,  8,  1), False, None, 10),
        ("Collect rings from jeweller",    "Pick up wedding bands from Bijuteria Floreasca.",
         "Admin",        date(2026,  9, 10), False, None, 11),
        ("Wedding rehearsal",              "Rehearsal dinner and ceremony walk-through at Eden Garden Estate.",
         "Ceremony",     date(2026,  9, 12), False, None, 12),
    ]
    cur.executemany(
        "INSERT IGNORE INTO checklist_items "
        "(id, couple_id, title, description, category, due_date, "
        " completed, completed_at, sort_order, is_default) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        [(uid(), cp, c[0], c[1], c[2], str(c[3]), c[4], c[5], c[6], True)
         for c in checklist],
    )

    # ── 11. MESSAGING THREAD (venue deal) ────────────────────────────────────
    print("→ threads + messages")
    cur.execute(
        "INSERT IGNORE INTO threads (id, deal_id, created_at, last_message_at) "
        "VALUES (%s,%s,%s,%s)",
        (thread, d_venue, ts(3), ts(1)),
    )
    cur.executemany(
        "INSERT IGNORE INTO thread_participants (id, thread_id, user_id) VALUES (%s,%s,%s)",
        [(tp_c, thread, u_couple), (tp_v, thread, u_venue)],
    )
    cur.executemany(
        "INSERT IGNORE INTO messages "
        "(id, thread_id, sender_id, content, type, read_by, created_at) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s)",
        [
            (m1, thread, u_couple,
             "Bună ziua! Suntem foarte interesați de Eden Garden Estate pentru nunta noastră "
             "din 14 septembrie 2026. Am dori să programăm o vizită. "
             "Sunteți disponibili săptămâna viitoare?",
             "TEXT", json.dumps([u_couple]), ts(3)),

            (m2, thread, u_venue,
             "Bună ziua! Vă mulțumim pentru interes! Suntem disponibili marți sau joi "
             "între 10:00–16:00. Data de 14 septembrie 2026 este liberă pe calendarul nostru. "
             "Abia așteptăm să vă cunoaștem! 🌿",
             "TEXT", json.dumps([u_venue]), ts(2)),

            (m3, thread, u_couple,
             "Perfect! Joi ar fi ideal pentru noi. Putem veni la ora 11:00.",
             "TEXT", json.dumps([u_couple]), ts(1)),
        ],
    )

    # ── 12. NOTIFICATIONS ───────────────────────────────────────────────────
    print("→ notifications")
    cur.executemany(
        "INSERT IGNORE INTO notifications "
        "(id, user_id, type, title, body, entity_id, read_flag) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s)",
        [
            (uid(), u_couple, "DEAL_STATUS_CHANGED",
             "Photographer confirmed! 🎉",
             "Lumina Studios confirmed your booking for 14 Sep 2026. Your photography is locked in!",
             d_photo, True),

            (uid(), u_couple, "QUOTE_RECEIVED",
             "New quote from Saveur Catering",
             "You have a new quote for 120 guests. Tap to review and accept.",
             d_cater, False),

            (uid(), u_couple, "MESSAGE_RECEIVED",
             "New message from Eden Garden Estate",
             "Eden Garden Estate responded to your enquiry. Tap to read.",
             thread, False),

            (uid(), u_couple, "DEAL_STATUS_CHANGED",
             "Band booking confirmed 🎶",
             "Ritm & Note Orchestra confirmed your booking for the evening of 14 Sep 2026.",
             d_band, True),

            (uid(), u_couple, "DEAL_STATUS_CHANGED",
             "Enquiry sent to Bella Cake Studio",
             "Your enquiry has been delivered. You'll hear back within 48 hours.",
             d_cake, True),

            # Vendor-side notifications
            (uid(), u_photo, "DEAL_STATUS_CHANGED",
             "New enquiry from Ana & Mihai",
             "A couple sent you a booking enquiry for 14 Sep 2026. Tap to view.",
             d_photo, True),

            (uid(), u_venue, "MESSAGE_RECEIVED",
             "New message from Ana Ionescu",
             "Ana Ionescu sent you a message about a venue visit. Tap to reply.",
             thread, False),

            (uid(), u_band, "DEAL_STATUS_CHANGED",
             "Booking confirmed 🎉",
             "Your booking with Ana & Mihai for 14 Sep 2026 is now confirmed!",
             d_band, False),
        ],
    )

    conn.commit()
    cur.close()
    conn.close()

    print("""
╔══════════════════════════════════════════════════════════════╗
║                     ✅  Seed complete!                       ║
╠══════════════════════════════════════════════════════════════╣
║  Couple login     couple@demo.ro        / password123        ║
╠══════════════════════════════════════════════════════════════╣
║  Vendor logins    photo@demo.ro         / password123        ║
║  (all password    video@demo.ro         / password123        ║
║   = password123)  florist@demo.ro       / password123        ║
║                   catering@demo.ro      / password123        ║
║                   venue@demo.ro         / password123        ║
║                   cake@demo.ro          / password123        ║
║                   band@demo.ro          / password123        ║
║                   transport@demo.ro     / password123        ║
╚══════════════════════════════════════════════════════════════╝

  Seeded:
    9  users  (1 couple + 8 vendors)
    8  vendor profiles  (PHOTOGRAPHER → TRANSPORTATION)
    1  couple profile   (Ana & Mihai, wedding 14 Sep 2026, Sinaia)
    8  deals            (ENQUIRY → CONFIRMED, various stages)
    3  quotes           (photo ACCEPTED, caterer PENDING, band ACCEPTED)
    3  bookings         (photo + video + band)
    2  reviews          (★★★★★ photo, ★★★★½ band)
   15  guests           (9 CONFIRMED, 3 PENDING, 2 DECLINED, 1 PENDING+plusOne)
    8  budget items     (55 000 RON total budget)
   13  checklist items  (3 done, 10 to-do)
    1  message thread   (3 messages, venue deal)
    8  notifications    (couple + vendors)
""")


if __name__ == "__main__":
    main()
