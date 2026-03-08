export interface Notification {
  id: string;
  userId: string;
  type:
    | "NEW_LEAD"
    | "LEAD_ACCEPTED"
    | "LEAD_DECLINED"
    | "NEW_OFFER"
    | "OFFER_ACCEPTED"
    | "NEW_MESSAGE"
    | "call_reminder"
    | "VIDEO_CALL_PROPOSED"
    | "VIDEO_CALL_RESCHEDULED"
    | "VIDEO_CALL_ACCEPTED"
    | "VIDEO_CALL_CANCELLED"
    | "review_request"
    | string;
  title: string;
  body?: string;
  entityId?: string;
  read: boolean;
  createdAt: string;
}
