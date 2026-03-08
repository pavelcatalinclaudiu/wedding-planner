-- Allow CANCELLED as a valid lead status (set when a vendor cancels a confirmed booking)
ALTER TABLE leads DROP CONSTRAINT chk_leads_status;
ALTER TABLE leads ADD CONSTRAINT chk_leads_status
    CHECK (status IN ('NEW','VIEWED','IN_DISCUSSION','QUOTED','BOOKED','DECLINED','CANCELLED'));
