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
}

export interface ChecklistItem {
  id: string;
  coupleId: string;
  label: string;
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

export type RsvpStatus = "PENDING" | "CONFIRMED" | "DECLINED" | "MAYBE";
export type GuestSide = "BRIDE" | "GROOM" | "BOTH";
export type GuestGroup = "FAMILY" | "FRIENDS" | "COLLEAGUES" | "OTHER";
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
  songRequest?: string;
  notes?: string;
  isChildGuest: boolean;
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
  dietary?: DietaryRequirement;
  dietaryNotes?: string;
  tableAssignment?: string;
  songRequest?: string;
  notes?: string;
  isChildGuest?: boolean;
}

export interface GuestStats {
  total: number;
  confirmed: number;
  declined: number;
  pending: number;
  maybe: number;
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
  coupleId: string;
  subdomain: string;
  template: number;
  heroTitle: string;
  venueName?: string;
  coverPhotoUrl?: string;
  welcomeMessage?: string;
  views: number;
  rsvpsSubmitted: number;
  customDomain?: string;
  published: boolean;
}
