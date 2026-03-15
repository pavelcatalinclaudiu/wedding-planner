export type VendorCategory =
  | "PHOTOGRAPHER"
  | "VIDEOGRAPHER"
  | "FLORIST"
  | "CATERER"
  | "VENUE"
  | "BAND"
  | "DJ"
  | "CAKE"
  | "MAKEUP_ARTIST"
  | "HAIR_STYLIST"
  | "OFFICIANT"
  | "PLANNER"
  | "TRANSPORTATION"
  | "LIGHTING"
  | "INVITATION_STATIONERY"
  | "JEWELRY"
  | "OTHER";

export type VendorTier = "FREE" | "STANDARD" | "PREMIUM";

export interface VendorProfile {
  id: string;
  userId: string;
  businessName: string;
  category: VendorCategory;
  city: string;
  description?: string;
  basePrice?: number;
  averageRating?: number;
  reviewCount: number;
  tier: VendorTier;
  active: boolean;
  monetizationEnabled?: boolean;
  yearsExperience?: number;
  languages?: string[];
  photos: VendorPhoto[];
  packages: VendorPackage[];
  coverPhoto?: string;
  profilePicture?: string;
  website?: string;
  instagram?: string;
  facebook?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VendorPhoto {
  id: string;
  vendorId: string;
  url: string;
  order: number;
  caption?: string;
}

export interface VendorPackage {
  id: string;
  vendorId: string;
  name: string;
  price: number;
  description?: string;
  items: string[];
}

export interface VendorProfileRequest {
  businessName: string;
  category: VendorCategory;
  city: string;
  description?: string;
  basePrice?: number;
  yearsExperience?: number;
  languages?: string[];
}

export interface VendorPartner {
  id: string;
  /** Profile ID of the linked platform vendor — undefined for name-only entries */
  partnerId?: string;
  /** Display name */
  partnerName: string;
  partnerCategory?: string;
  partnerCity?: string;
  partnerCoverPhoto?: string;
  /** true when the partner has a profile on this platform */
  onPlatform: boolean;
  createdAt: string;
}

export interface VendorAnalytics {
  profileViews: number;
  enquiries: number;
  conversionRate: number;
  revenue: number;
  avgResponseTimeHours: number;
  averageRating: number;
}

// ── Deep Analytics ────────────────────────────────────────────────────────────

export interface MonthlyRevenue {
  month: string; // "Jan", "Feb" …
  year: number;
  revenue: number;
  bookings: number;
}

export interface LeadFunnel {
  newLeads: number;
  inDiscussion: number;
  quoted: number;
  booked: number;
  declined: number;
}

export interface QuarterlyRating {
  label: string; // "Q1 2025"
  avgRating: number;
  reviewCount: number;
}

export interface BookingPeak {
  month: string; // "Jan" …
  monthNumber: number;
  count: number;
}

export interface VendorDeepAnalytics {
  revenueByMonth: MonthlyRevenue[];
  leadFunnel: LeadFunnel;
  ratingByQuarter: QuarterlyRating[];
  bookingPeaks: BookingPeak[];
}

export interface AvailabilityBlock {
  id: string;
  vendorId: string;
  date: string; // "yyyy-MM-dd"
  reason?: string;
}

// ── Overview Stats DTOs ───────────────────────────────────────────────────────

export interface RevenueStatDTO {
  current: number;
  trend: number; // positive = up, negative = down
  bookingCount: number;
}

export interface LeadsStatDTO {
  total: number;
  thisMonth: number;
  today: number;
  trend: number; // vs previous month
}

export interface ViewsStatDTO {
  total: number;
  thisWeek: number;
  trendPercent: number; // percentage change vs previous week
}

export interface RatingStatDTO {
  value: number; // e.g. 4.9
  reviewCount: number;
  lastReviewDaysAgo: number | null;
}

export interface ResponseRateDTO {
  percent: number; // 0–100
  trend: number; // percentage-point change vs last period
  repliedCount: number;
  totalEnquiries: number;
}

export interface MonthlyLeadDTO {
  month: string; // "Jan", "Feb" …
  count: number;
  isCurrent: boolean;
}

export type DealStatus =
  | "ENQUIRY"
  | "CONVERSATION"
  | "QUOTE_SENT"
  | "QUOTE_ACCEPTED"
  | "DEPOSIT_PAID"
  | "CONFIRMED";

export interface RecentLeadDTO {
  dealId: string;
  coupleName: string;
  coupleProfilePicture: string | null;
  weddingDate: string | null;
  location: string | null;
  message: string;
  receivedAt: string;
  status: DealStatus;
}

export interface UpcomingCallDTO {
  callId: string;
  coupleName: string;
  coupleProfilePicture: string | null;
  scheduledAt: string;
  dealId: string;
}

export interface ProfilePerformanceDTO {
  searchRank: number;
  searchRankCategory: string;
  avgResponseTimeHours: number;
  conversionRate: number;
  platformAvgConversion: number;
}

export interface VendorOverviewDTO {
  revenue: RevenueStatDTO;
  leads: LeadsStatDTO;
  profileViews: ViewsStatDTO;
  avgRating: RatingStatDTO;
  responseRate: ResponseRateDTO;
  monthlyLeads: MonthlyLeadDTO[];
  recentLeads: RecentLeadDTO[];
  upcomingCalls: UpcomingCallDTO[];
  profilePerformance: ProfilePerformanceDTO;
}
