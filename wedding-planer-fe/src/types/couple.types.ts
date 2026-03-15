export interface CoupleProfile {
  id: string;
  userId: string;
  partner1Name: string;
  partner2Name: string;
  weddingDate: string;
  weddingLocation: string;
  estimatedGuestCount: number;
  totalBudget: number;
  subdomain?: string;
  profilePicture?: string;
  onboardingCompleted: boolean;
  plan?: "FREE" | "DREAM_WEDDING";
  monetizationEnabled?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CoupleProfileRequest {
  partner1Name: string;
  partner2Name: string;
  weddingDate: string;
  weddingLocation: string;
  estimatedGuestCount: number;
  totalBudget: number;
  onboardingCompleted?: boolean;
}

export interface ChecklistItem {
  id: string;
  coupleId: string;
  label: string;
  notes?: string;
  dueDate?: string;
  done: boolean;
  timePeriod: string;
  auto: boolean;
  createdAt: string;
}

export interface BudgetItem {
  id: string;
  coupleId: string;
  category: string;
  name?: string;
  vendorName?: string;
  estimatedCost: number;
  actualCost?: number;
  isPaid: boolean;
  notes?: string;
  createdAt: string;
}

export type RsvpStatus = "PENDING" | "CONFIRMED" | "DECLINED";
export type GuestSide = "BRIDE" | "GROOM" | "BOTH";
export type GuestGroup = "FAMILY" | "FRIENDS" | "COLLEAGUES" | "OTHER";
export type InviteStatus =
  | "NOT_INVITED"
  | "LINK_SENT"
  | "PENDING"
  | "ACCEPTED"
  | "DECLINED";
export type DietaryRequirement =
  | "NONE"
  | "VEGAN"
  | "VEGETARIAN"
  | "GLUTEN_FREE"
  | "HALAL"
  | "KOSHER"
  | "OTHER";

export interface Guest {
  id: string;
  coupleId: string;
  invitedById?: string;
  firstName: string;
  lastName?: string;
  fullName: string;
  email?: string;
  phone?: string;
  side: GuestSide;
  guestGroup: GuestGroup;
  rsvpStatus: RsvpStatus;
  plusOne: boolean;
  plusOneName?: string;
  dietary: DietaryRequirement;
  dietaryNotes?: string;
  tableAssignment?: string;
  notes?: string;
  isChildGuest: boolean;
  inviteToken?: string;
  inviteStatus: InviteStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface GuestRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  side?: GuestSide;
  guestGroup?: GuestGroup;
  rsvpStatus?: RsvpStatus;
  plusOne?: boolean;
  plusOneName?: string;
  plusOneDietary?: string;
  plusOneDietaryNotes?: string;
  dietary?: DietaryRequirement;
  dietaryNotes?: string;
  tableAssignment?: string;
  notes?: string;
  isChildGuest?: boolean;
}

export interface GuestStats {
  total: number;
  confirmed: number;
  declined: number;
  pending: number;
  estimatedCapacity: number;
  overCapacity: boolean;
  byDietary: Record<string, number>;
  bySide: Record<string, number>;
  byGroup: Record<string, number>;
}

export interface SeatingLayout {
  tables: SeatingTable[];
}

export interface SeatingTable {
  id: string;
  name: string;
  capacity: number;
  x: number;
  y: number;
  guests: string[];
}

export interface WeddingWebsite {
  id: string;
  subdomain?: string;
  heroMessage?: string;
  coverPhotoUrl?: string;
  story?: string;
  ceremonyDate?: string;
  ceremonyLocation?: string;
  receptionDate?: string;
  receptionLocation?: string;
  published: boolean;
  rsvpsSubmitted: number;
  visitorCount: number;
  subdomainCustomized: boolean;
}

export interface PublicWeddingWebsite {
  heroMessage?: string;
  coverPhotoUrl?: string;
  story?: string;
  ceremonyDate?: string;
  ceremonyLocation?: string;
  receptionDate?: string;
  receptionLocation?: string;
  partner1Name?: string;
  partner2Name?: string;
  weddingDate?: string;
  weddingLocation?: string;
  couplePhoto?: string;
  subdomain?: string;
}

export interface RsvpRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  attending: "CONFIRMED" | "DECLINED";
  dietary?: string;
  dietaryNotes?: string;
  plusOne?: boolean;
  plusOneName?: string;
  plusOneDietary?: string;
  plusOneDietaryNotes?: string;
  message?: string;
  inviteToken?: string;
}
