import Button from "../Button";
import BinaryBackground from "./BinaryBackground";
import { useNavigate } from "react-router-dom";
import { SignedIn, SignedOut, useClerk, useUser } from "@clerk/clerk-react";
import { useEffect, useState } from "react";
import { queryAPI } from "../../api";
import { v4 as uuidv4 } from "uuid"; // Fallback for generating guest names

// title home page
export default function Home() {
  const navigate = useNavigate();
  const { openSignIn } = useClerk();
  const { user } = useUser();
  const [title, setTitle] = useState("DEVDUEL");

  // get current date for user data
  const getCurrentDate = () => {
    const date = new Date();
    return `${date.getFullYear()}-${(date.getMonth() + 1)
      .toString()
      .padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")}`;
  };

  // function to check if a user exists in database using UserList endpoint
  async function checkIfUserExists(username: string) {
    try {
      const response = await queryAPI("UserList", {});
      if (response.response_type === "success") {
        const users = response.data;
        const userExists = users.some(
          (user: any) => user.displayName === username
        );
        if (userExists) {
          return true;
        }
      } else {
        return false;
      }
    } catch (error) {
      console.error("Error checking if user exists:", error);
      return false;
    }
  }

  // function to save user data to backend using UserSet endpoint
  const saveUserToBackend = async (userData: {
    displayName: string;
    userID: string;
    email: string;
    wins: string;
    date: string;
  }) => {
    try {
      const response = await queryAPI("UserSet", userData);
      if (response.response_type === "failure") {
        console.error("Failed to save user data:", response);
      } else {
        console.log("User data saved successfully:", response.data);
      }
    } catch (error) {
      console.error("Error connecting to backend:", error);
    }
  };

  const handleSignInPlay = async () => {
    if (!user) {
      return;
    }

    // Determine displayName
    const displayName =
      user.username ||
      user.primaryEmailAddress?.emailAddress.split("@")[0] ||
      "Unknown";

    const userData = {
      displayName: displayName,
      userID: user.id,
      email: user.primaryEmailAddress?.emailAddress || "guest@example.com",
      wins: "0", // Default wins
      date: getCurrentDate(),
    };

    // if user exists, don't save to backend
    if (!(await checkIfUserExists(userData.displayName))) {
      await saveUserToBackend(userData);
    }
    navigate("/lobby");
  };

  // Handle guest play logic
  const handleGuestPlay = async () => {
    const guestName = `guest${uuidv4().slice(0, 6)}`;
    const userData = {
      displayName: guestName,
      userID: guestName,
      email: `${guestName}@example.com`,
      wins: "0", // Default wins
      date: getCurrentDate(),
    };

    // set up localStorage for guestName
    localStorage.setItem("guestName", guestName);

    await saveUserToBackend(userData);
    navigate("/lobby");
  };

  // animation for title
  function animation() {
    const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    let iterations = 0;

    const animateTitle = () => {
      iterations = 0;
      const interval = setInterval(() => {
        setTitle((title) =>
          title
            .split("")
            .map((_, index) => {
              if (index < iterations) {
                return "DEVDUEL"[index]; // Use the original title's letter
              }
              return letters[Math.floor(Math.random() * letters.length)];
            })
            .join("")
        );

        if (iterations >= "DEVDUEL".length) {
          clearInterval(interval);
        }

        iterations += 1 / 3;
      }, 30);
    };
    animateTitle();
  }

  useEffect(() => {
    const mainInterval = setInterval(animation, 5000);

    return () => clearInterval(mainInterval); // Clear the interval on unmount
  }, []);

  return (
    <div className="relative h-screen w-screen overflow-hidden">
      {/* Title */}
      <h1
        className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 font-courier text-9xl text-gray-400 font-bold z-10 text-shadow-glow"
        onMouseEnter={animation}
      >
        {title}
      </h1>
      <BinaryBackground />

      {/* Buttons Section */}
      <div className="flex flex-col items-center justify-center h-screen mt-20 gap-10">
        {/* SignedOut: Show sign-in button */}
        <SignedOut>
          <Button
            text="Sign In"
            onClick={() => {
              console.log("Sign In button clicked");
              openSignIn();
            }}
          />
          <Button text="Play as Guest" onClick={handleGuestPlay} />
        </SignedOut>

        {/* SignedIn: Show buttons for signed-in users */}
        <SignedIn>
          <Button text="Enter Game" onClick={handleSignInPlay} />
        </SignedIn>
      </div>

      {/* Footer */}
      <div className="absolute inset-x-0 bottom-8 transform translate-x-1/2 w-1/2 text-center text-gray-800 bg-black p-6 z-10 rounded-xl opacity-75">
        <div className="flex flex-col gap-2">
          <p>
            Created by: Malcolm Grant, Maxwell Adorsoo, Moses Yang, Theo Romero
          </p>
          <p>Mentors: Professor Tim Nelson and John Wilkinson</p>
          <p>Developed for Brown University's CSCI0320 Taught in Fall 2024</p>
        </div>
      </div>
    </div>
  );
}
