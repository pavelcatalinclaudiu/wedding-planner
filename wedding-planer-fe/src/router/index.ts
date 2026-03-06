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
        meta: { title: "Overview" },
      },
      {
        path: "enquiries",
        component: CoupleLeadsView,
        meta: { title: "Enquiries" },
      },
      {
        path: "checklist",
        component: CoupleChecklistView,
        meta: { title: "Checklist" },
      },
      {
        path: "budget",
        component: CoupleBudgetView,
        meta: { title: "Budget" },
      },
      {
        path: "guests",
        component: CoupleGuestsView,
        meta: { title: "Guest List" },
      },
      {
        path: "seating",
        component: CoupleSeatingView,
        meta: { title: "Seating Chart" },
      },
      {
        path: "team",
        component: CoupleTeamView,
        meta: { title: "Vendor Team" },
      },
      { path: "messages", redirect: "/couple/enquiries" },
      { path: "messages/:threadId", redirect: "/couple/enquiries" },
      {
        path: "group-chat",
        component: CoupleGroupChatView,
        meta: { title: "Group Chat" },
      },
      {
        path: "website",
        component: CoupleWebsiteView,
        meta: { title: "Wedding Website" },
      },
      {
        path: "documents",
        component: CoupleDocumentsView,
        meta: { title: "Documents" },
      },
      {
        path: "calls",
        component: CoupleVideoCallsView,
        meta: { title: "Video Calls" },
      },
      {
        path: "calendar",
        component: CoupleCalendarView,
        meta: { title: "Calendar" },
      },
      {
        path: "profile",
        component: CoupleProfileView,
        meta: { title: "My Profile" },
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
        meta: { title: "Overview" },
      },
      { path: "leads", component: VendorInboxView, meta: { title: "Inbox" } },
      { path: "messages", redirect: "/vendor/leads" },
      { path: "messages/:threadId", redirect: "/vendor/leads" },
      {
        path: "calendar",
        component: VendorCalendarView,
        meta: { title: "Availability" },
      },
      {
        path: "calls",
        component: VendorVideoCallsView,
        meta: { title: "Video Calls" },
      },
      {
        path: "portfolio",
        component: VendorPortfolioView,
        meta: { title: "Portfolio" },
      },
      {
        path: "reviews",
        component: VendorReviewsView,
        meta: { title: "Reviews" },
      },
      {
        path: "network",
        component: VendorNetworkView,
        meta: { title: "Partner Network" },
      },
      {
        path: "analytics",
        component: VendorAnalyticsView,
        meta: { title: "Analytics" },
      },
      {
        path: "profile",
        component: VendorProfileView,
        meta: { title: "My Profile" },
      },
      {
        path: "subscription",
        component: VendorSubscriptionView,
        meta: { title: "Subscription" },
      },
      {
        path: "group-chat",
        component: VendorGroupChatView,
        meta: { title: "Group Chat" },
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
