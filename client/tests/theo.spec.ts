import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Sign In" }).click();
  await page.getByPlaceholder("Enter email or username").click();
  await page
    .getByPlaceholder("Enter email or username")
    .fill("name@example.com");
  await page.getByPlaceholder("Enter email or username").press("Enter");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByPlaceholder("Enter your password").fill("Example123!#");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button").click();
});

test("test top right button and top left title", async ({ page }) => {
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Profile").click();
  await expect(page.getByRole("heading", { name: "Profile" })).toBeVisible();
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Lobby").click();
  await expect(page.getByRole("heading", { name: "Lobby" })).toBeVisible();
});

test("test create game pop up", async ({ page }) => {
  await page.getByRole("button", { name: "+" }).click();
  await expect(
    page.getByRole("heading", { name: "Create a New Game" })
  ).toBeVisible();
  await expect(page.getByRole("combobox")).toContainText("EasyMediumHard");
  await expect(page.getByText("Select Match Duration")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Create Game!" })
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Cancel" })).toBeVisible();
});

test("test create match waiting page", async ({ page }) => {
  await page.getByRole("button", { name: "+" }).click();
  await page.getByRole("button", { name: "Create Game!" }).click();
  await expect(
    page.getByRole("heading", { name: "Waiting for a Player to Join" })
  ).toBeVisible();
  await expect(
    page.getByRole("img", { name: "loading animation" })
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Cancel" })).toBeVisible();
});

test("test seeing existing users", async ({ page }) => {
  await expect(page.getByText("All Users")).toBeVisible();
  await expect(page.getByText("theoromero11")).toBeVisible();
  await expect(page.getByText("moses_yang")).toBeVisible();
});

test("test leaderboard and stats", async ({ page }) => {
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Profile").click();
  await expect(page.getByText("#4) name - 0 wins")).toBeVisible();
  await expect(page.getByText("#2) testTwo - 2 wins")).toBeVisible();
  await expect(page.getByText("Username: name")).toBeVisible();
  await expect(page.getByText("Email: name@example.com")).toBeVisible();
  await expect(page.getByText("Leaderboard Rank: 4")).toBeVisible();
  await expect(page.getByText("Total Wins: 0")).toBeVisible();
});

test("test lobby games loading", async ({ page }) => {
  await page.getByRole("button", { name: "+" }).click();
  await page.getByRole("button", { name: "Create Game!" }).click();
  await page.getByText("Sign Out").click();
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await expect(
    page.getByRole("heading", { name: "name's Game" })
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Join!" }).nth(1)
  ).toBeVisible();
});

test("test past code loding", async ({ page }) => {
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Profile").click();
  await expect(page.getByRole("heading", { name: "mergeTwo" })).toBeVisible();
  await expect(page.getByText("Number Solved: 0/")).toBeVisible();
});
