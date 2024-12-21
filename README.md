# DevDuel
Play your friends and practice coding in a 1v1 website game using Leetcode style questions!

### Collaborators - Total hours spent: 150
- Malcolm Grant
- Maxwell Adorsoo
- Moses Yang
- Theo Romero
- OpenAI. (2024). ChatGPT (November 24 version) [Large language model]. https://chat.openai.com/chat
  - Used ChatGPT for assistance with TypeScript and Java syntax

### Mentors
- Professor Tim Nelson
- John Wilkinson

## Future Goals
- Delete guest profiles on signout
- Add more problems and test to ensure that all of them work
- Show function signature instead of "You did not submit any code!" when no code is submitted
- Implement greater definitiveness on determining winners; currently if users are tied, both lose
- Responsiveness. Allow users on various different sizes of devices to play DevDuel

## Project Details
DevDuel contains numerous features to exponentiate coding progress by combining friendly competition with learning:
- Sign In/ Guest Play
  - DevDuel encourages you to sign in to play to keep track of your statistics and past code. However, it's also possible to get a quick game in without signing in, providing you with all features except for the profile page
  - Clerk Authentication is used, with email and username enabled, to allow users to sign in and play
  - UUID's are used to generate unique guest usernames to prevent errors from the same username
- Lobby
  - Allows you to see all available games, as well as all users of DevDuel
  - Continuously fetches rooms from backend to update state every 5 seconds
- Game
  - Game page with flexible boxes, allowing resizing if desired by user
  - Shows updated score, output, problem, and current code
  - Fetched problems are randomized by difficulty
  - Allows running of code in Python, Java, and JavaScript
  - Code is run using Piston API
  - When code is run, notification messages are displayed using React Notifications
- Profile
  - Keeps track of user stats
  - Showcases past code for individual problems (saved by highest score, and then most recent submission)
  - Leaderboard of all users
  - Individual user ranking, number of wins, username, email, and date of account creation
- Problem Form
  - A form, accessible at hosting site `/form`, allows admin easy addition of new problems into the database
- Navigation
  - DevDuel utilizes React Router to navigate between states
- Visual Design
  - TailwindCSS and ChakraUI for design
  - Monaco Code Editor for the code editor shown in game, profile, and win/loss screen
- Firestore Database
  - DevDuel uses Firestore Database to store all of our necessary components
  - Three collections: Users, Problems, and Rooms
    - Users keeps track of all user stats and past code
    - Problems keeps track of all problem information and code signatures
    - Rooms keeps track of game information including # of users, current problem ID, etc.

## How To Run
- Clone the project in github and add it to local repository
- Frontend
  - `cd client`
  - `npm install`
  - `npm run dev`
  - The frontend will run on localhost in port 5173
- Backend
  - `cd server`
  - `cd dev_duel`
  - `mvn package`
  - `./run`
  - The backend will run on localhost in port 3232
 
## Testing
DevDuel contains both frontend and backend tests:
- Frontend
  - Ensures that all features in project details operate as expected:
    - Sign In and Play As Guest buttons
    - Lobby with available games
    - Profile page with past code and stats
    - Game page with flexible boxes
    - Win/loss screen
    - Consistent headers
- Backend
  - Ensures that all endpoints and firestore incorporation operate as expected:
    - SOMEONE ADD HERE

## Resources 
- Gettin config file related error : move resources folder into src/main/resources
- permission denied for `./run`: in terminal do: `chmod -x run` and retry `./run`.
