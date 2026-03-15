export interface AdminStats {
  totalUsers: number;
  totalVendors: number;
  totalCouples: number;
  newUsersThisWeek: number;
  totalLeads: number;
  activeVendors: number;
  totalReviews: number;
  avgRating: number;
  totalProfileViews: number;
  uniqueVisitors: number;
  profileViewsThisWeek: number;
  landingPageVisits: number;
  uniqueLandingVisitors: number;
  landingPageVisitsThisWeek: number;
  signupsByDay: { date: string; count: number }[];
  vendorsByCategory: Record<string, number>;
  topVendors: AdminTopVendor[];
  pendingReviewsCount: number;
  paidVendors: number;
  paidCouples: number;
}

export interface AdminTopVendor {
  id: string;
  businessName: string;
  category: string;
  city: string;
  avgRating: number;
  reviewCount: number;
  leadCount: number;
}

export interface AdminUser {
  id: string;
  email: string;
  role: "COUPLE" | "VENDOR" | "ADMIN";
  emailVerified: boolean;
  createdAt: string;
  lastLogin: string | null;
  displayName: string | null;
  couplePlan: "FREE" | "DREAM_WEDDING" | null;
  coupleMonetizationEnabled: boolean;
  profileId: string | null;
}

export interface AdminVendor {
  id: string;
  userId: string;
  email: string;
  businessName: string;
  category: string;
  city: string;
  basePrice: number | null;
  avgRating: number;
  reviewCount: number;
  profileViews: number;
  isVerified: boolean;
  isActive: boolean;
  monetizationEnabled: boolean;
  leadCount: number;
  createdAt: string;
  tier: "FREE" | "STANDARD" | "PREMIUM";
}

export interface AdminReview {
  id: string;
  rating: number;
  comment: string | null;
  vendorReply: string | null;
  isPublic: boolean;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
  vendorId: string;
  vendorName: string;
  coupleId: string;
  coupleName: string;
}

export interface AdminPagedResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
