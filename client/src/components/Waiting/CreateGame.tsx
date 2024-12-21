import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { queryAPI } from "../../api.ts";
import { useUser } from "@clerk/clerk-react";

interface CreateGameProps {
  onClose: () => void;
}

// pop up to create a game
export default function CreateGame({ onClose }: CreateGameProps) {
  const [difficulty, setDifficulty] = useState("Easy");
  const [matchDuration, setMatchDuration] = useState(1);
  const navigate = useNavigate();
  const user = useUser().user;
  const username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];

  // function to change difficulty
  const handleDifficultyChange = (
    event: React.ChangeEvent<HTMLSelectElement>
  ) => {
    setDifficulty(event.target.value);
  };

  const increaseDuration = () => {
    setMatchDuration((prev) => Math.min(prev + 1, 60)); // Max 60
  };

  const decreaseDuration = () => {
    setMatchDuration((prev) => Math.max(prev - 1, 1)); // Min 0
  };

  // generate a random room ID
  function generateRandomRoomID() {
    return Math.random().toString(36).substring(2, 8);
  }

  // use getproblem endpoint to fetch a problemID
  async function fetchProblemID() {
    try {
      const response = await queryAPI("getproblem", { difficulty: difficulty });
      if (response.response_type === "success") {
        const fetchedProblem = response.body[0];
        console.log(fetchedProblem);
        return fetchedProblem.problemID;
      } else {
        console.error("Failed to fetch problem");
      }
    } catch (error) {
      console.error("Error fetching problem:", error);
    }
  }

  // use RoomSet endpoint to create a room and advance to waiting page
  async function makeRoom() {
    const roomID = generateRandomRoomID();
    let guestName = "";
    if (!username) {
      guestName = localStorage.getItem("guestName") || "";
    }
    navigate("/pregame", {
      state: { roomID },
    });
    try {
      console.log(username);
      const response = await queryAPI("RoomSet", {
        roomID: roomID,
        roomName: (username as string) || guestName,
        problemID: await fetchProblemID(),
        difficulty: difficulty,
        timeCreated: new Date().toISOString(),
        duration: (matchDuration * 60).toString(),
        userName: (username as string) || guestName,
        userID: user?.id || guestName,
        userScore: "0/10",
        timeSubmitted: new Date().toISOString(),
      });
      if (response.response_type === "success") {
        console.log("Room created successfully");
      } else {
        console.error("Failed to create room");
      }
    } catch (error) {
      console.error("Error creating room:", error);
    }
  }

  return (
    <div className="fixed top-0 left-0 w-full h-full bg-black bg-opacity-70 z-30 flex justify-center items-center">
      <div className="relative bg-custom-radial from-green-800 to-green-950 w-2/5 h-3/5 z-40 rounded-xl flex flex-col justify-between p-6">
        {/* Title */}
        <h2 className="flex justify-center font-courier font-bold text-gray-400 text-5xl mt-4">
          Create a New Game
        </h2>

        {/* Select Difficulty */}
        <div className="flex flex-col items-center">
          <label className="text-gray-400 text-xl font-bold mb-4">
            Select Difficulty
          </label>
          <select
            value={difficulty}
            onChange={handleDifficultyChange}
            className="w-2/5 px-4 py-2 text-md rounded-lg bg-gray-300 text-black font-semibold shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition duration-200 ease-in-out"
          >
            <option value="Easy">Easy</option>
            <option value="Medium">Medium</option>
            <option value="Hard">Hard</option>
          </select>
        </div>

        {/* Select Match Duration */}
        <div className="flex flex-col items-center">
          <label className="text-gray-400 text-xl font-bold mb-4">
            Select Match Duration
          </label>
          <div className="flex items-center gap-4">
            <button
              onClick={decreaseDuration}
              className="px-4 py-2 bg-gray-500 text-white rounded-lg shadow hover:bg-gray-400"
            >
              -
            </button>
            <div className="text-gray-200 font-bold text-xl">
              {matchDuration} min
            </div>
            <button
              onClick={increaseDuration}
              className="px-4 py-2 bg-gray-500 text-white rounded-lg shadow hover:bg-gray-400"
            >
              +
            </button>
          </div>
        </div>

        {/* Create Game Button */}
        <div className="flex flex-row justify-center gap-6 pb-4">
          <button
            className="flex font-courier text-gray-400 rounded-xl shadow-lg bg-red-500 p-[1px] hover:shadow-xl"
            onClick={onClose}
          >
            <div className="flex justify-center bg-red-700 py-3 px-4 rounded-xl shadow-lg hover:bg-red-800 text-xl font-bold">
              Cancel
            </div>
          </button>

          <button
            className="flex font-courier text-gray-400 rounded-xl shadow-lg bg-gray-300 p-[1px] hover:shadow-xl"
            onClick={() => makeRoom()}
          >
            <div className="flex justify-center bg-stone-800 py-3 px-6 rounded-xl shadow-lg hover:bg-gray-700 text-xl font-bold">
              Create Game!
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}
