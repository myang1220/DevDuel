import { expect, test } from "@playwright/test";

/**
 * Profile past code review
 */

test.beforeEach(async ({ page }) => {
  await page.goto("http://localhost:5173/");
});

test("on load, expect to see sign in, play as guest, title, and creators", async ({
  page,
}) => {
  await expect(page.getByRole("heading", { name: "DEVDUEL" })).toBeVisible();
  await expect(
    page.locator("button").filter({ hasText: "Sign In" })
  ).toBeVisible();
  await expect(
    page.locator("button").filter({ hasText: "Play as Guest" })
  ).toBeVisible();
  await expect(
    page.locator("div").filter({ hasText: "Created by: Malcolm Grant," }).nth(3)
  ).toBeVisible();
});

test("in lobby page I should see lobby title, DevDuel title, header, create game button, and all users panel", async ({
  page,
}) => {
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await expect(page.getByRole("heading", { name: "Lobby" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "DEVDUEL" })).toBeVisible();
  await expect(page.getByText("LobbyDEVDUELUser")).toBeVisible();
  await expect(page.getByRole("button", { name: "+" })).toBeVisible();
  await expect(page.getByText("All Users")).toBeVisible();
});

test("i see a waiting for game page when I create a game", async ({ page }) => {
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
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

test("signing out as guest takes me back to the home screen", async ({
  page,
}) => {
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Sign Out").click();
  await expect(
    page.locator("button").filter({ hasText: "Play as Guest" })
  ).toBeVisible();
  await expect(page.getByRole("heading", { name: "DEVDUEL" })).toBeVisible();
});

test("i can start a game and then sign in as another user to join that game. i should see a countdown and all of the game components.", async ({
  page,
}) => {
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await page.getByRole("button", { name: "+" }).click();
  await page.getByRole("button", { name: "Create Game!" }).click();
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await expect(page.getByRole("button", { name: "Join!" })).toBeVisible();
  await page.getByRole("button", { name: "Join!" }).click();
  await expect(page.getByText("Game Starting In...")).toBeVisible();
  await page.goto("http://localhost:5173/game");
  await expect(page.getByRole("button", { name: "Run" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Submit" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Score" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Case 1" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Case 2" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Case 3" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "You Lost!" })).toBeVisible();
  await expect(page.getByText("You Lost!You solved 0/10 test")).toBeVisible();
  await expect(page.getByRole("button", { name: "Leave Game" })).toBeVisible();
});

test("i can start up a game (like the test before) and see a win/loss screen when the game ends", async ({
  page,
}) => {
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await page.getByRole("button", { name: "+" }).click();
  await page.getByRole("button", { name: "Create Game!" }).click();
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Play as Guest" }).click();
  await expect(page.getByRole("button", { name: "Join!" })).toBeVisible();
  await page.getByRole("button", { name: "Join!" }).click();
  await page.goto("http://localhost:5173/game");
  await page.waitForTimeout(60000);
  await expect(page.getByRole("heading", { name: "You Lost!" })).toBeVisible();
  await expect(page.getByText("You Lost!You solved 0/10 test")).toBeVisible();
  await expect(page.getByRole("button", { name: "Leave Game" })).toBeVisible();
});

test("i can sign in as a user and sign back out", async ({ page }) => {
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Sign In" }).click();
  await page.getByPlaceholder("Enter email or username").click();
  await page
    .getByPlaceholder("Enter email or username")
    .fill("name@example.com");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByPlaceholder("Enter your password").click();
  await page.getByPlaceholder("Enter your password").fill("Example123!#");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button").click();
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Sign Out").click();
  await expect(
    page.locator("button").filter({ hasText: "Sign In" })
  ).toBeVisible();
});

test("i can see a profile page only when i am a signed in user", async ({
  page,
}) => {
  await page.locator("button").filter({ hasText: "Sign In" }).click();
  await page.getByPlaceholder("Enter email or username").click();
  await page
    .getByPlaceholder("Enter email or username")
    .fill("name@example.com");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByPlaceholder("Enter your password").click();
  await page.getByPlaceholder("Enter your password").fill("Example123!#");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button").click();
  await page.getByRole("button", { name: "User" }).click();
  await expect(page.getByText("Profile")).toBeVisible();
  await page.getByText("Profile").click();
  await expect(page.getByRole("heading", { name: "Profile" })).toBeVisible();
  await expect(page.getByText("Username:")).toBeVisible();
});

test("i can go back to the lobby screen when i am in the profile screen", async ({
  page,
}) => {
  await page.locator("button").filter({ hasText: "Sign In" }).click();
  await page.getByPlaceholder("Enter email or username").click();
  await page
    .getByPlaceholder("Enter email or username")
    .fill("name@example.com");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByPlaceholder("Enter your password").click();
  await page.getByPlaceholder("Enter your password").fill("Example123!#");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button").click();
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Profile").click();
  await page.getByRole("button", { name: "User" }).click();
  await expect(page.getByText("Lobby")).toBeVisible();
});

test("i can review my code in the profile page", async ({ page }) => {
  await page.goto("http://localhost:5173/");
  await page.locator("button").filter({ hasText: "Sign In" }).click();
  await page.getByPlaceholder("Enter email or username").click();
  await page
    .getByPlaceholder("Enter email or username")
    .fill("name@example.com");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByPlaceholder("Enter your password").click();
  await page.getByPlaceholder("Enter your password").fill("Example123!#");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.getByRole("button").click();
  await page.getByRole("button", { name: "User" }).click();
  await page.getByText("Profile").click();
  await expect(page.getByRole("heading", { name: "twoSum" })).toBeVisible();
  await page.getByRole("button", { name: "Review" }).click();
  await expect(page.locator("h1").filter({ hasText: "twoSum" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Exit Review" })).toBeVisible();
  await page.getByRole("button", { name: "Exit Review" }).click();
});
