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
  signupsByDay: { date: string; count: number }[];
  vendorsByCategory: Record<string, number>;
  topVendors: AdminTopVendor[];
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
  leadCount: number;
  createdAt: string;
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
