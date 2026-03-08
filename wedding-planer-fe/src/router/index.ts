import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { setupGuards } from "./guards";

// Auth
const LoginView = () => import("@/views/auth/LoginView.vue");
const RegisterView = () => import("@/views/auth/RegisterView.vue");
const RegisterCoupleView = () => import("@/views/auth/RegisterCoupleView.vue");
const RegisterVendorView = () => import("@/views/auth/RegisterVendorView.vue");
const VerifyEmailView = () => import("@/views/auth/VerifyEmailView.vue");
const ResetPasswordView = () => import("@/views/auth/ResetPasswordView.vue");

// Couple
const CoupleLayout = () => import("@/views/couple/CoupleLayout.vue");
const CoupleOverviewView = () =>
  import("@/views/couple/CoupleOverviewView.vue");
const CoupleLeadsView = () => import("@/views/couple/CoupleLeadsView.vue");
const CoupleChecklistView = () =>
  import("@/views/couple/CoupleChecklistView.vue");
const CoupleBudgetView = () => import("@/views/couple/CoupleBudgetView.vue");
const CoupleGuestsView = () => import("@/views/couple/CoupleGuestsView.vue");
const CoupleSeatingView = () => import("@/views/couple/CoupleSeatingView.vue");
const CoupleTeamView = () => import("@/views/couple/CoupleTeamView.vue");
const CoupleGroupChatView = () =>
  import("@/views/couple/CoupleGroupChatView.vue");
const CoupleWebsiteView = () => import("@/views/couple/CoupleWebsiteView.vue");
const CoupleDocumentsView = () =>
  import("@/views/couple/CoupleDocumentsView.vue");
const CoupleVideoCallsView = () =>
  import("@/views/couple/CoupleVideoCallsView.vue");
const CoupleCalendarView = () =>
  import("@/views/couple/CoupleCalendarView.vue");
const CoupleProfileView = () => import("@/views/couple/CoupleProfileView.vue");

// Vendor
const VendorLayout = () => import("@/views/vendor/VendorLayout.vue");
const VendorOverviewView = () =>
  import("@/views/vendor/VendorOverviewView.vue");
const VendorInboxView = () => import("@/views/vendor/VendorInboxView.vue");
const VendorCalendarView = () =>
  import("@/views/vendor/VendorCalendarView.vue");
const VendorVideoCallsView = () =>
  import("@/views/vendor/VendorVideoCallsView.vue");
const VendorPortfolioView = () =>
  import("@/views/vendor/VendorPortfolioView.vue");
const VendorReviewsView = () => import("@/views/vendor/VendorReviewsView.vue");
const VendorNetworkView = () => import("@/views/vendor/VendorNetworkView.vue");
const VendorAnalyticsView = () =>
  import("@/views/vendor/VendorAnalyticsView.vue");
const VendorProfileView = () => import("@/views/vendor/VendorProfileView.vue");
const VendorSubscriptionView = () =>
  import("@/views/vendor/VendorSubscriptionView.vue");
const VendorGroupChatView = () =>
  import("@/views/vendor/VendorGroupChatView.vue");

// Public
const LandingView = () => import("@/views/public/LandingView.vue");
const VendorListingView = () => import("@/views/public/VendorListingView.vue");
const VendorPublicView = () => import("@/views/public/VendorPublicView.vue");
const WeddingWebsiteView = () =>
  import("@/views/public/WeddingWebsiteView.vue");
const RsvpView = () => import("@/views/public/RsvpView.vue");

const routes: RouteRecordRaw[] = [
  // AUTH
  { path: "/login", component: LoginView, meta: { guest: true } },
  { path: "/register", component: RegisterView, meta: { guest: true } },
  {
    path: "/register/couple",
    component: RegisterCoupleView,
    meta: { guest: true },
  },
  {
    path: "/register/vendor",
    component: RegisterVendorView,
    meta: { guest: true },
  },
  { path: "/verify-email", component: VerifyEmailView },
  {
    path: "/reset-password",
    component: ResetPasswordView,
    meta: { guest: true },
  },

  // COUPLE DASHBOARD
  {
    path: "/couple",
    component: CoupleLayout,
    meta: { requiresAuth: true, role: "COUPLE" },
    children: [
      { path: "", redirect: "/couple/overview" },
      {
        path: "overview",
        component: CoupleOverviewView,
        meta: { title: "nav.couple.items.overview" },
      },
      {
        path: "enquiries",
        component: CoupleLeadsView,
        meta: { title: "nav.couple.items.myEnquiries" },
      },
      {
        path: "checklist",
        component: CoupleChecklistView,
        meta: { title: "nav.couple.items.checklist" },
      },
      {
        path: "budget",
        component: CoupleBudgetView,
        meta: { title: "nav.couple.items.budgetTracker" },
      },
      {
        path: "guests",
        component: CoupleGuestsView,
        meta: { title: "nav.couple.items.guestList" },
      },
      {
        path: "seating",
        component: CoupleSeatingView,
        meta: { title: "nav.couple.items.seatingChart" },
      },
      {
        path: "team",
        component: CoupleTeamView,
        meta: { title: "nav.couple.items.myWeddingTeam" },
      },
      { path: "messages", redirect: "/couple/enquiries" },
      { path: "messages/:threadId", redirect: "/couple/enquiries" },
      {
        path: "group-chat",
        component: CoupleGroupChatView,
        meta: { title: "nav.couple.items.groupChat" },
      },
      {
        path: "website",
        component: CoupleWebsiteView,
        meta: { title: "nav.couple.items.weddingWebsite" },
      },
      {
        path: "documents",
        component: CoupleDocumentsView,
        meta: { title: "nav.couple.items.documentVault" },
      },
      {
        path: "calls",
        component: CoupleVideoCallsView,
        meta: { title: "nav.couple.items.videoCalls" },
      },
      {
        path: "calendar",
        component: CoupleCalendarView,
        meta: { title: "nav.couple.items.calendar" },
      },
      {
        path: "profile",
        component: CoupleProfileView,
        meta: { title: "nav.couple.items.myProfile" },
      },
    ],
  },

  // VENDOR DASHBOARD
  {
    path: "/vendor",
    component: VendorLayout,
    meta: { requiresAuth: true, role: "VENDOR" },
    children: [
      { path: "", redirect: "/vendor/overview" },
      {
        path: "overview",
        component: VendorOverviewView,
        meta: { title: "nav.vendor.items.overview" },
      },
      {
        path: "leads",
        component: VendorInboxView,
        meta: { title: "nav.vendor.items.inbox" },
      },
      { path: "messages", redirect: "/vendor/leads" },
      { path: "messages/:threadId", redirect: "/vendor/leads" },
      {
        path: "calendar",
        component: VendorCalendarView,
        meta: { title: "nav.vendor.items.availability" },
      },
      {
        path: "calls",
        component: VendorVideoCallsView,
        meta: { title: "nav.vendor.items.videoCalls" },
      },
      {
        path: "portfolio",
        component: VendorPortfolioView,
        meta: { title: "nav.vendor.items.portfolio" },
      },
      {
        path: "reviews",
        component: VendorReviewsView,
        meta: { title: "nav.vendor.items.reviews" },
      },
      {
        path: "network",
        component: VendorNetworkView,
        meta: { title: "nav.vendor.items.partnerNetwork" },
      },
      {
        path: "analytics",
        component: VendorAnalyticsView,
        meta: { title: "nav.vendor.items.analytics" },
      },
      {
        path: "profile",
        component: VendorProfileView,
        meta: { title: "nav.vendor.items.myProfile" },
      },
      {
        path: "subscription",
        component: VendorSubscriptionView,
        meta: { title: "nav.vendor.items.subscription" },
      },
      {
        path: "group-chat",
        component: VendorGroupChatView,
        meta: { title: "nav.vendor.items.groupChat" },
      },
    ],
  },

  // PUBLIC PAGES
  { path: "/", component: LandingView },
  { path: "/vendors", component: VendorListingView },
  { path: "/vendors/:id", component: VendorPublicView },
  { path: "/w/:subdomain", component: WeddingWebsiteView },
  { path: "/rsvp/:token", component: RsvpView },

  { path: "/:pathMatch(.*)*", redirect: "/" },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

setupGuards(router);

export default router;
