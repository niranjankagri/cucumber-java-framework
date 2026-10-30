# Manual test cases

The scenarios this framework automates, written as a tester would run them by hand. Every case has an
ID; the Cucumber scenario that covers it carries the same ID in its name (`[TC-101] Successful login
with valid credentials`) and as a tag (`@TC-101`), so a failure in the report leads straight to the
case below, and one case can be run on its own:

```
mvn test -Dcucumber.filter.tags="@TC-106"
```

## Setup

- Application: OrangeHRM demo <https://opensource-demo.orangehrmlive.com> (public, shared with other
  testers).
- Admin login: `Admin` / `admin123` (`src/test/resources/config.properties`).

## Summary

| ID | Title | Type | Priority | Feature file |
|---|---|---|---|---|
| TC-101 | Successful login with valid credentials | Smoke | High | `login.feature` |
| TC-102 | Login is rejected (wrong password, unknown user) | Negative | High | `login.feature` (Scenario Outline) |
| TC-103 | Login is not submitted without a password | Negative | Medium | `login.feature` |
| TC-104 | Successful logout | Smoke | High | `login.feature` |
| TC-105 | Open the System Users page from the menu | Smoke | High | `admin.feature` |
| TC-106 | Search users by username and status | Regression | High | `admin.feature` |
| TC-107 | Reset clears the search filters | Regression | Medium | `admin.feature` |

---

## Login (`features/login.feature`)

Precondition for all: the login page is open.

### TC-101 Successful login with valid credentials

| # | Step | Expected result |
|---|---|---|
| 1 | Enter `Admin` / `admin123`, click **Login** | The Dashboard is displayed |

### TC-102 Login is rejected

For every data set: enter the username and password, click **Login**. Expected: the message
"Invalid credentials"; still on the login page.

| Data set | Username | Password |
|---|---|---|
| the password is wrong | `Admin` | `wrong123` |
| the user is unknown | `nobody42` | `admin123` |

### TC-103 Login is not submitted without a password

| # | Step | Expected result |
|---|---|---|
| 1 | Enter username `Admin`, leave the password empty, click **Login** | The **Password** field shows "Required"; no login request |

### TC-104 Successful logout

| # | Step | Expected result |
|---|---|---|
| 1 | Log in as in TC-101 | Dashboard |
| 2 | Open the user menu (top right), click **Logout** | The login page is displayed |

## Admin user management (`features/admin.feature`)

Precondition for all: logged in as `Admin`.

### TC-105 Open the System Users page from the menu

| # | Step | Expected result |
|---|---|---|
| 1 | Click **Admin** in the side menu | System Users page: page title "Admin", the user search form and the user list |

### TC-106 Search users by username and status

| # | Step | Expected result |
|---|---|---|
| 1 | On System Users enter username `Admin`, choose status **Enabled**, click **Search** | Only users named "Admin" with status "Enabled" are listed |

### TC-107 Reset clears the search filters

| # | Step | Expected result |
|---|---|---|
| 1 | Enter username `Admin`, choose status **Enabled** | Filters filled |
| 2 | Click **Reset** | The username field is empty and status shows "-- Select --" |
