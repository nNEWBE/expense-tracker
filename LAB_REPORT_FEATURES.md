# TrackExpense - Personal Finance Management Application
## Complete Feature Documentation for Lab Report

---

## 📱 Project Overview

**Project Name:** TrackExpense  
**Platform:** Android (Native)  
**Programming Language:** Java  
**Minimum SDK:** Android 7.0 (API Level 24)  
**Target SDK:** Android 14 (API Level 34)  
**Architecture:** MVVM (Model-View-ViewModel)  
**Database:** Room Database (Local) + Firebase Firestore (Cloud)  
**Authentication:** Firebase Authentication  

---

## 🏗️ System Architecture

### Tech Stack
| Component | Technology |
|-----------|------------|
| Language | Java |
| UI Framework | Android XML Layouts + Material Design 3 |
| Local Database | Room Persistence Library |
| Cloud Database | Firebase Firestore |
| Authentication | Firebase Auth (Email/Password + Google Sign-In) |
| Charts | MPAndroidChart Library |
| Navigation | Android Jetpack Navigation Component |
| Background Tasks | WorkManager |
| Image Loading | Native Android |
| Notifications | Firebase Cloud Messaging + Local Notifications |

### Project Structure
```
app/src/main/java/com/example/trackexpense/
├── MainActivity.java              # Main entry point with navigation
├── adapters/                      # RecyclerView Adapters
│   └── AppNotificationAdapter.java
├── data/
│   ├── local/                     # Room Database
│   │   ├── AppDatabase.java
│   │   ├── Expense.java           # Entity
│   │   └── ExpenseDao.java        # Data Access Object
│   ├── model/                     # Data Models
│   │   ├── AppNotification.java
│   │   ├── Category.java
│   │   └── User.java
│   ├── remote/
│   │   └── FirestoreService.java  # Cloud operations
│   └── repository/
│       └── NotificationRepository.java
├── ui/
│   ├── admin/                     # Admin Panel
│   ├── analytics/                 # Analytics & Reports
│   ├── auth/                      # Authentication Screens
│   ├── dashboard/                 # Dashboard
│   ├── expense/                   # Transaction Management
│   ├── profile/                   # User Profile
│   └── splash/                    # Splash Screen
├── utils/                         # Utility Classes
│   ├── BeautifulNotification.java
│   ├── CategoryHelper.java
│   ├── ExportUtils.java
│   ├── NotificationHelper.java
│   ├── PreferenceManager.java
│   └── ReminderWorker.java
└── viewmodel/
    └── ExpenseViewModel.java      # ViewModel
```

---

## 🔐 1. Authentication Module

### 1.1 User Registration
- **Email/Password Registration**: Users can create accounts using email and password
- **Input Validation**: Real-time validation for email format and password strength
- **Password Requirements**: Minimum 6 characters
- **Email Verification**: Mandatory email verification before account activation
- **Profile Setup**: Users can set display name during registration
- **Error Handling**: Clear error messages for duplicate emails, weak passwords, etc.

### 1.2 User Login
- **Email/Password Login**: Standard authentication method
- **Google Sign-In**: One-tap Google OAuth integration
- **Remember Me**: Session persistence across app restarts
- **Forgot Password**: Password reset via email link
- **Auto-Login**: Automatic login for verified users

### 1.3 Email Verification
- **Verification Email**: Automatic sending upon registration
- **Resend Option**: Users can request new verification emails
- **Verification Status Check**: Real-time verification status monitoring
- **Redirect Flow**: Automatic navigation after successful verification

### 1.4 Guest Mode
- **Anonymous Access**: Users can explore the app without registration
- **Limited Features**: 
  - Maximum 5 transactions per day
  - No access to notifications
  - No access to category requests
  - No cloud sync
- **Data Migration**: Guest data can be synced when user registers
- **Daily Limit Reset**: Transaction count resets at midnight

### 1.5 Password Recovery
- **Forgot Password Flow**: Email-based password reset
- **Reset Link**: Secure time-limited password reset links
- **Confirmation**: Success/failure notifications

---

## 🏠 2. Dashboard Module

### 2.1 Financial Overview
- **Total Balance**: Real-time calculation of (Income - Expenses)
- **Total Income**: Sum of all income transactions
- **Total Expenses**: Sum of all expense transactions
- **Animated Counters**: Smooth number animations on load

### 2.2 Budget Card
- **Monthly Budget Display**: Shows set budget amount
- **Remaining Budget**: Calculated remaining amount for the month
- **Days Left**: Shows days remaining in current month
- **Progress Indicator**: Visual budget consumption progress
- **Card Flip Animation**: Smooth 3D flip between balance and budget cards

### 2.3 Recent Transactions
- **Last 10 Transactions**: Quick view of recent activity
- **Filter Options**: ALL / INCOME / EXPENSE filter chips
- **Transaction Cards**: Amount, category, date, notes display
- **See All Link**: Navigation to full transaction list

### 2.4 Greeting System
- **Time-Based Greeting**:
  - Good Morning (before 12 PM) 🌞
  - Good Afternoon (12 PM - 5 PM) ☀️
  - Good Evening (after 5 PM) 🌙
- **Personalized Name**: Displays user's display name

### 2.5 Quick Actions
- **Menu Button**: Opens navigation drawer
- **Notification Bell**: Opens notification panel (logged-in users only)
- **Category Requests Button**: Opens category requests panel (logged-in users only)

---

## 💰 3. Transaction Management Module

### 3.1 Add Transaction
- **Transaction Types**: Income and Expense toggle
- **Amount Input**: Numeric input with currency symbol
- **Quick Amount Chips**: ৳100, ৳500, ৳1000, ৳5000 buttons
- **Category Selection**: Grid of categorized icons
- **Date Picker**: Material Design date picker
- **Notes Field**: Optional transaction description
- **Budget Warning**: Alert when expense exceeds/approaches budget

### 3.2 Transaction Categories

#### Expense Categories
| Category | Icon | Color |
|----------|------|-------|
| Food | 🍔 | #F97316 |
| Transport | 🚗 | #3B82F6 |
| Shopping | 🛍️ | #EC4899 |
| Entertainment | 🎬 | #8B5CF6 |
| Health | 💊 | #10B981 |
| Bills | 📄 | #EAB308 |
| Education | 📚 | #06B6D4 |
| Travel | ✈️ | #F43F5E |
| Groceries | 🛒 | #22C55E |
| Subscription | 📱 | #A855F7 |
| Rent | 🏠 | #64748B |
| Insurance | 🛡️ | #64748B |
| Utilities | ⚡ | #EAB308 |
| Other | ❓ | #64748B |

#### Income Categories
| Category | Icon | Color |
|----------|------|-------|
| Salary | 💵 | #14B8A6 |
| Freelance | 💻 | #F59E0B |
| Investment | 📈 | #3B82F6 |
| Gift | 🎁 | #DB2777 |
| Bonus | 🎉 | #14B8A6 |
| Refund | 💳 | #6366F1 |
| Rental Income | 🏢 | #64748B |
| Other | ❓ | #64748B |

### 3.3 Edit Transaction
- **Inline Edit**: Tap on transaction to edit
- **All Fields Editable**: Amount, category, date, notes, type
- **Update Confirmation**: Success notification after update

### 3.4 Delete Transaction
- **Swipe to Delete**: Swipe gesture for quick deletion
- **Delete Confirmation**: Confirmation dialog before deletion
- **Undo Option**: Snackbar with undo action

### 3.5 Transaction List
- **Date-Based Grouping**: Transactions grouped by date
- **Search Functionality**: Search by category, notes, amount
- **Filter by Type**: Income/Expense/All filters
- **Date Range Filter**: Custom date range selection
- **Sort Options**: By date, amount, category
- **Expandable Details**: Tap to expand transaction details

---

## 📊 4. Analytics Module

### 4.1 Expense Breakdown
- **Pie Chart**: Visual distribution of expenses by category
- **Percentage Display**: Each category's percentage of total
- **Interactive Chart**: Tap on segments for details
- **Color-Coded Legend**: Matching category colors

### 4.2 Income vs Expense
- **Bar Chart**: Monthly comparison of income vs expenses
- **Trend Analysis**: Visual trend over time
- **Net Difference**: Calculated savings/deficit

### 4.3 Category Progress
- **Progress Bars**: Spending progress for each category
- **Percentage Indicators**: Visual percentage completion
- **Color Indicators**: 
  - Green: Under 70%
  - Yellow: 70-90%
  - Red: Over 90%

### 4.4 Statistics Cards
- **Total Transactions Count**: Number of all transactions
- **Average Transaction**: Mean transaction amount
- **Highest Expense**: Maximum single expense
- **Highest Income**: Maximum single income

### 4.5 Time Period Filters
- **This Week**: Current week data
- **This Month**: Current month data
- **This Year**: Current year data
- **All Time**: Complete history

---

## 👤 5. Profile Module

### 5.1 User Information
- **Display Name**: Editable username
- **Email Address**: User's registered email
- **Profile Avatar**: Default or custom avatar
- **Account Creation Date**: Registration timestamp

### 5.2 Budget Settings
- **Monthly Budget**: Set/update monthly spending limit
- **Budget Notifications**: Enable/disable budget alerts
- **Quick Amount Presets**: ৳5000, ৳10000, ৳20000, ৳50000

### 5.3 Currency Settings
- **Multiple Currencies Support**:
  - BDT (৳) - Bangladeshi Taka
  - USD ($) - US Dollar
  - EUR (€) - Euro
  - GBP (£) - British Pound
  - INR (₹) - Indian Rupee

### 5.4 Theme Settings
- **Light Theme**: Bright, clean interface
- **Dark Theme**: Dark mode for low-light usage
- **System Default**: Follow system theme

### 5.5 Account Actions
- **Edit Profile**: Change display name
- **Change Password**: Update account password
- **Logout**: Sign out with confirmation
- **Delete Account**: Permanent account deletion with password confirmation

---

## 🔔 6. Notification System

### 6.1 In-App Notifications
- **Transaction Notifications**: Created, updated, deleted transactions
- **Budget Alerts**: Warning at 90%, exceeded notifications
- **Category Request Updates**: Approval/rejection notifications (for logged-in users)

### 6.2 Notification Panel
- **Slide-in Panel**: Animated panel from right side
- **Unread Badge**: Count of unread notifications
- **Mark All Read**: Bulk read action
- **Clear All**: Delete all notifications
- **Individual Actions**: Delete, mark as read

### 6.3 Notification Types
| Type | Description | Icon |
|------|-------------|------|
| Transaction Created | New transaction added | ➕ |
| Transaction Updated | Existing transaction modified | ✏️ |
| Transaction Deleted | Transaction removed | 🗑️ |
| Budget Warning | Approaching budget limit (90%) | ⚠️ |
| Budget Exceeded | Over monthly budget | 🚨 |
| Category Request | Admin notifications for requests | 📝 |

### 6.4 Push Notifications
- **Budget Alerts**: System notifications for budget events
- **Daily Reminders**: Configurable reminder to add transactions
- **Background Processing**: WorkManager for scheduled notifications

---

## 📁 7. Data Management Module

### 7.1 Export Data
- **CSV Export**: Export transactions as CSV file
- **JSON Export**: Export transactions as JSON file
- **Date Range Selection**: Export specific period
- **File Location**: Saved to Downloads folder

### 7.2 Import Data
- **CSV Import**: Import from CSV files
- **JSON Import**: Import from JSON files
- **Validation**: Data format validation before import
- **Merge/Replace Options**: Handle existing data conflicts

### 7.3 Backup & Restore
- **Cloud Backup**: Firebase Firestore sync
- **Local Backup**: Room database persistence
- **Auto-Sync**: Real-time cloud synchronization
- **Offline Support**: Full functionality without internet

### 7.4 Guest Data Sync
- **Data Migration**: Transfer guest data to account upon registration
- **Merge Logic**: Combine guest transactions with existing data
- **Confirmation Dialog**: User consent before migration

---

## 👨‍💼 8. Admin Panel Module

### 8.1 Admin Access
- **Role-Based Access**: Admin flag in user profile
- **Admin Navigation**: Separate admin section in drawer
- **Protected Routes**: Admin-only screens

### 8.2 User Management
- **User List**: View all registered users
- **User Details**: Email, registration date, transaction count
- **User Transactions**: View user's transaction history
- **Delete User**: Remove user accounts (with confirmation)

### 8.3 Category Management
- **View All Categories**: List of expense and income categories
- **Add Category**: Create new categories with:
  - Name
  - Type (Income/Expense)
  - Icon selection
  - Color picker
  - Display order
- **Edit Category**: Modify existing categories
- **Delete Category**: Remove unused categories
- **System Categories**: Protected default categories

### 8.4 Category Request System
- **User Requests**: Users can request new categories
- **Request Queue**: Admin view of pending requests
- **Review Dialog**: 
  - Requester information
  - Category details
  - Reason for request
- **Approve/Reject**: Admin decision with status update
- **Notifications**: Users notified of request status

### 8.5 Transaction Oversight
- **All Transactions**: View system-wide transactions
- **User-wise Breakdown**: Filter by specific user
- **Edit Any Transaction**: Admin can modify user transactions
- **Delete Any Transaction**: Admin can remove transactions

---

## 🎨 9. UI/UX Features

### 9.1 Material Design 3
- **Dynamic Colors**: Theme-aware color system
- **Rounded Corners**: Consistent 16dp radius
- **Elevation Shadows**: Subtle depth effects
- **Ripple Effects**: Touch feedback animations

### 9.2 Skeleton Loading
- **Dashboard Skeleton**: Placeholder during data load
- **Transaction List Skeleton**: Loading state for lists
- **Profile Skeleton**: Profile section placeholders
- **Smooth Transitions**: Fade-in animations

### 9.3 Animations
- **Card Flip**: 3D flip animation for balance/budget cards
- **Panel Slide**: Notification panel slide-in/out
- **Counter Animation**: Animated number counting
- **Entrance Animations**: Staggered list item animations
- **Micro-interactions**: Button press, chip selection effects

### 9.4 Responsive Design
- **Adaptive Layouts**: Adjusts to screen sizes
- **Orientation Support**: Portrait and landscape
- **Overflow Handling**: Scrollable content areas

### 9.5 Beautiful Notifications (Toast)
- **Custom Toast Design**: Branded toast messages
- **Types**:
  - Success (Green) ✅
  - Error (Red) ❌
  - Warning (Yellow) ⚠️
  - Info (Blue) ℹ️
- **Icon Support**: Type-specific icons
- **Smooth Animations**: Fade in/out effects

---

## 🔒 10. Security Features

### 10.1 Authentication Security
- **Email Verification**: Mandatory email confirmation
- **Password Hashing**: Firebase secure password storage
- **Session Management**: Secure token-based sessions
- **Auto-Logout**: Session expiry handling

### 10.2 Data Protection
- **Local Encryption**: Room database encryption
- **Cloud Security**: Firebase security rules
- **Input Sanitization**: Protection against injection
- **Secure Preferences**: Encrypted SharedPreferences

### 10.3 Rate Limiting
- **Guest Limit**: 5 transactions per day for guests
- **Logged-in Limit**: 50 transactions per day
- **Daily Reset**: Automatic counter reset at midnight

### 10.4 Account Protection
- **Delete Confirmation**: Password required for account deletion
- **Logout Confirmation**: Dialog before sign out
- **Data Wipe**: Complete data removal on account deletion

---

## ⚙️ 11. Settings & Preferences

### 11.1 App Settings
- **Currency Selection**: Choose display currency
- **Theme Mode**: Light/Dark/System
- **Notification Preferences**: Enable/disable alerts
- **Daily Reminder Time**: Set reminder notification time

### 11.2 Budget Configuration
- **Monthly Budget**: Set spending limit
- **Budget Alerts**: Warning thresholds
- **Budget Reset**: Monthly automatic reset

### 11.3 Data Settings
- **Export Format**: CSV or JSON preference
- **Auto-Backup**: Enable cloud sync
- **Cache Management**: Clear cached data

---

## 🛠️ 12. Technical Features

### 12.1 Offline Support
- **Local Database**: Room for offline storage
- **Sync Queue**: Pending changes for sync
- **Conflict Resolution**: Last-write-wins strategy

### 12.2 Performance Optimization
- **Lazy Loading**: Load data on demand
- **View Recycling**: Efficient RecyclerView usage
- **Image Caching**: Efficient icon loading
- **Background Processing**: WorkManager for async tasks

### 12.3 Error Handling
- **Graceful Degradation**: App continues on partial failure
- **Error Messages**: User-friendly error descriptions
- **Retry Mechanisms**: Automatic retry for network failures
- **Crash Recovery**: State preservation on crash

### 12.4 Real-Time Updates
- **Firestore Listeners**: Real-time notification updates
- **LiveData Observers**: Reactive UI updates
- **Instant Badge Updates**: Real-time notification counts

---

## 📋 13. Navigation Structure

### 13.1 Bottom Navigation
| Tab | Icon | Screen |
|-----|------|--------|
| Home | 🏠 | Dashboard |
| Transactions | 📋 | Transaction List |
| Add | ➕ | Add Transaction (FAB) |
| Analytics | 📊 | Analytics & Reports |
| Profile | 👤 | User Profile |

### 13.2 Drawer Navigation
| Item | Description |
|------|-------------|
| Budget Goals | Set monthly budget |
| Categories | Manage categories (Request for users) |
| Export Data | Download transactions |
| Import Data | Upload transactions |
| Theme | Change app theme |
| Admin Panel | Admin dashboard (Admin only) |
| Help & Support | FAQ and support |
| Send Feedback | Email feedback |
| Rate App | Play Store rating |
| Logout | Sign out |

---

## 🔄 14. Data Flow

### 14.1 MVVM Architecture Flow
```
View (Fragment/Activity)
        ↓ User Action
ViewModel (ExpenseViewModel)
        ↓ Business Logic
Repository (ExpenseRepository)
        ↓ Data Operations
    ┌──────────────┐
    │              │
Room Database  Firestore
  (Local)       (Cloud)
```

### 14.2 Transaction Flow
1. User enters transaction details
2. Input validation in Fragment
3. ViewModel processes business logic
4. Budget check if expense type
5. Repository saves to Room database
6. Background sync to Firestore
7. UI updated via LiveData
8. Notification triggered

---

## 📱 15. Screen List

### Activity Screens
1. SplashActivity - App launch screen
2. WelcomeActivity - Welcome/onboarding
3. LoginActivity - User login
4. RegisterActivity - User registration
5. EmailVerificationActivity - Email verification
6. VerifyEmailActivity - Verification status
7. MainActivity - Main container
8. AdminActivity - Admin dashboard

### Fragment Screens
1. DashboardFragment - Home dashboard
2. TransactionsFragment - Transaction list
3. AddExpenseFragment - Add/edit transaction
4. AnalyticsFragment - Charts and statistics
5. ProfileFragment - User profile
6. AdminCategoriesFragment - Category management
7. AdminUsersFragment - User management

### Dialog Screens
1. Budget dialog - Set monthly budget
2. Currency dialog - Select currency
3. Theme dialog - Choose theme
4. Export dialog - Export options
5. Import dialog - Import file
6. Delete confirmation - Confirm deletions
7. Category request - Request new category
8. Category form - Add/edit category
9. Logout dialog - Confirm logout
10. Help dialog - FAQ and support

---

## 📊 16. Database Schema

### Local Database (Room)

#### Expense Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key (auto-generated) |
| amount | DOUBLE | Transaction amount |
| category | TEXT | Category name |
| date | LONG | Timestamp (milliseconds) |
| notes | TEXT | Optional description |
| type | TEXT | "INCOME" or "EXPENSE" |
| firestoreId | TEXT | Cloud document ID |
| synced | BOOLEAN | Sync status flag |

### Cloud Database (Firestore)

#### Users Collection
```
users/{userId}
├── email: string
├── displayName: string
├── isAdmin: boolean
├── createdAt: timestamp
├── lastTransactionDate: string
├── dailyTransactionCount: number
└── monthlyBudget: number
```

#### Transactions Collection
```
users/{userId}/transactions/{transactionId}
├── amount: number
├── category: string
├── date: timestamp
├── notes: string
├── type: string
└── createdAt: timestamp
```

#### Categories Collection
```
categories/{categoryId}
├── name: string
├── type: string
├── iconName: string
├── colorHex: string
├── order: number
├── isDefault: boolean
└── createdAt: timestamp
```

#### Category Requests Collection
```
category_requests/{requestId}
├── userId: string
├── userName: string
├── categoryName: string
├── categoryType: string
├── reason: string
├── status: string ("PENDING"/"APPROVED"/"REJECTED")
└── createdAt: timestamp
```

#### Notifications Collection
```
users/{userId}/notifications/{notificationId}
├── type: string
├── title: string
├── message: string
├── isRead: boolean
├── extraData: string
└── createdAt: timestamp
```

---

## 🧪 17. Testing Considerations

### Unit Testing Areas
- Transaction CRUD operations
- Budget calculations
- Date formatting utilities
- Currency conversion
- Input validation

### UI Testing Areas
- Navigation flows
- Form submissions
- Dialog interactions
- Filter functionality
- Animation completeness

### Integration Testing Areas
- Firebase authentication
- Firestore sync
- Offline/online transitions
- Import/export operations

---

## 📦 18. Dependencies

### Core Dependencies
```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.navigation:navigation-fragment:2.7.6'
implementation 'androidx.navigation:navigation-ui:2.7.6'
```

### Firebase Dependencies
```gradle
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-messaging'
```

### Database Dependencies
```gradle
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'
```

### Chart Library
```gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

### Background Processing
```gradle
implementation 'androidx.work:work-runtime:2.9.0'
```

---

## 🚀 19. Future Enhancements

Now ences

The application follows modern Android development practices including:
- MVVM architecture for separation of concerns
- Material Design 3 for beautiful, consistent UI
- Firebase integration for cloud sync and authentication
- Room database for offline-first data persistence
- LiveData and ViewModel for reactive programming

---

**Document Version:** 1.0  
**Last Updated:** January 21, 2026  
**Author:** TrackExpense Development Team
